package com.jiyingcao.a51fengliu.viewmodel

import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.Street
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.StreetRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 收藏暗巷列表UI状态
 * 包含收藏暗巷列表页面所有需要的状态信息
 */
data class FavoriteStreetsUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val streets: List<Street> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = "",
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasLoaded: Boolean = false, // 是否已经加载过数据
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showContent: Boolean get() = !isLoading && !isError && streets.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && streets.isEmpty() && hasLoaded
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && loadingType == LoadingType.FULL_SCREEN
}

sealed class FavoriteStreetsIntent {
    data object InitialLoad : FavoriteStreetsIntent()
    data object Retry : FavoriteStreetsIntent()
    data object Refresh : FavoriteStreetsIntent()
    data object LoadMore : FavoriteStreetsIntent()
}

class FavoriteStreetsViewModel(
    private val repository: StreetRepository
) : BaseViewModel() {
    private var fetchJob: Job? = null
    private val dataLock = Mutex()
    
    // 单一状态源 - 这是MVI的核心原则
    private val _uiState = MutableStateFlow(FavoriteStreetsUiState())
    val uiState = _uiState.asStateFlow()
    
    // 内部状态管理
    @GuardedBy("dataLock")
    private var currentStreets: MutableList<Street> = mutableListOf()
    private var currentPage = 0
    private var pendingInitialLoad = true

    fun processIntent(intent: FavoriteStreetsIntent) {
        when (intent) {
            FavoriteStreetsIntent.InitialLoad -> initialLoad()
            FavoriteStreetsIntent.Retry -> retry()
            FavoriteStreetsIntent.Refresh -> refresh()
            FavoriteStreetsIntent.LoadMore -> loadMore()
        }
    }

    private fun initialLoad() {
        // 避免UI发生配置更改时ViewModel重新加载数据
        if (pendingInitialLoad) {
            fetchData(page = 1, loadingType = LoadingType.FULL_SCREEN)
            pendingInitialLoad = false
        }
    }

    private fun retry() {
        fetchData(page = 1, loadingType = LoadingType.FULL_SCREEN)
    }

    private fun refresh() {
        fetchData(page = 1, loadingType = LoadingType.PULL_TO_REFRESH)
    }

    private fun loadMore() {
        fetchData(page = currentPage + 1, loadingType = LoadingType.LOAD_MORE)
    }

    private fun fetchData(page: Int, loadingType: LoadingType) {
        if (shouldPreventRequest(loadingType)) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiStateToLoading(loadingType)
            
            repository.getFavoriteStreets(page)
                .onEach { result -> 
                    handleDataResult(page, result, loadingType)
                }
                .onCompletion { fetchJob = null }
                .collect()
        }
    }
    
    private suspend fun handleDataResult(
        page: Int,
        result: Result<PageData<Street>?>,
        loadingType: LoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                currentPage = page
                val newStreets = updateStreetsList(page, pageData.records)
                updateUiStateToSuccess(newStreets, pageData.noMoreData(), loadingType)
            }
            .onFailure { e ->
                if (!handleFailure(e)) {    // 通用错误处理(如远程登录), 如果处理过就不用再处理了
                    updateUiStateToError(e.toUserFriendlyMessage(), loadingType)
                }
                AppLogger.w(TAG, "获取收藏暗巷失败: ", e)
            }
    }

    private suspend fun updateStreetsList(page: Int, newStreets: List<Street>): List<Street> {
        return dataLock.withLock {
            if (page == 1) {
                // 首页或刷新 - 替换数据
                currentStreets.clear()
                currentStreets.addAll(newStreets)
            } else {
                // 加载更多 - 追加数据
                currentStreets.addAll(newStreets)
            }
            currentStreets.toList() // 返回不可变副本
        }
    }

    // ===== 专门的UI状态更新方法 =====
    
    /**
     * 更新UI状态到加载中
     * @param loadingType 加载类型，决定显示哪种加载状态
     */
    private fun updateUiStateToLoading(loadingType: LoadingType) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = loadingType == LoadingType.FULL_SCREEN,
                isRefreshing = loadingType == LoadingType.PULL_TO_REFRESH,
                isLoadingMore = loadingType == LoadingType.LOAD_MORE,
                loadingType = loadingType,
                isError = false // 清除之前的错误状态
            )
        }
    }
    
    /**
     * 更新UI状态到成功状态
     * @param streets 暗巷列表
     * @param noMoreData 是否没有更多数据
     */
    private fun updateUiStateToSuccess(
        streets: List<Street>,
        noMoreData: Boolean = false,
        loadingType: LoadingType
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = false,
                streets = streets,
                noMoreData = noMoreData,
                hasLoaded = true, // 标记已经加载过数据
                loadingType = loadingType // 保留加载类型，便于UI层正确处理
            )
        }
    }
    
    /**
     * 更新UI状态到错误状态
     * @param errorMessage 错误信息
     * @param loadingType 错误类型，决定错误显示方式
     */
    private fun updateUiStateToError(errorMessage: String, loadingType: LoadingType) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = true,
                errorMessage = errorMessage,
                loadingType = loadingType
            )
        }
    }

    /** 防止重复请求 */
    private fun shouldPreventRequest(loadingType: LoadingType): Boolean {
        val currentState = _uiState.value
        return when (loadingType) {
            LoadingType.FULL_SCREEN -> currentState.isLoading
            LoadingType.PULL_TO_REFRESH -> currentState.isRefreshing
            LoadingType.LOAD_MORE -> currentState.isLoadingMore || currentState.noMoreData
            else -> false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }

    companion object {
        private const val TAG: String = "FavoriteStreetsViewModel"
    }

    class Factory(
        private val repository: StreetRepository = StreetRepository.getInstance()
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FavoriteStreetsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FavoriteStreetsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
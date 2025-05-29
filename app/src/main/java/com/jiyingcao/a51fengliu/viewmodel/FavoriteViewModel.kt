package com.jiyingcao.a51fengliu.viewmodel

import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 单一UI状态
 * 包含页面所有需要的状态信息
 */
data class FavoriteUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val records: List<RecordInfo> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = "",
    val errorType: LoadingType = LoadingType.FULL_SCREEN,
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val isEmpty: Boolean get() = !isLoading && !isError && records.isEmpty()
    val showContent: Boolean get() = !isLoading && !isError && records.isNotEmpty()
    val showEmptyState: Boolean get() = !isLoading && !isError && records.isEmpty()
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && errorType == LoadingType.FULL_SCREEN
}

sealed class FavoriteIntent {
    data object InitialLoad : FavoriteIntent()
    data object Retry : FavoriteIntent()
    data object Refresh : FavoriteIntent()
    data object LoadMore : FavoriteIntent()
}

class FavoriteViewModel(
    private val repository: RecordRepository
) : BaseViewModel() {
    private var fetchJob: Job? = null
    private val dataLock = Mutex()
    
    // 单一状态源 - 这是MVI的核心原则
    private val _uiState = MutableStateFlow(FavoriteUiState())
    val uiState = _uiState.asStateFlow()
    
    // 内部状态管理
    @GuardedBy("dataLock")
    private var currentRecords: MutableList<RecordInfo> = mutableListOf()
    private var currentPage = 0
    private var pendingInitialLoad = true

    fun processIntent(intent: FavoriteIntent) {
        when (intent) {
            FavoriteIntent.InitialLoad -> initialLoad()
            FavoriteIntent.Retry -> retry()
            FavoriteIntent.Refresh -> refresh()
            FavoriteIntent.LoadMore -> loadMore()
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
        val currentState = _uiState.value
        if (currentState.isLoadingMore || currentState.noMoreData) return
        fetchData(page = currentPage + 1, loadingType = LoadingType.LOAD_MORE)
    }

    private fun fetchData(page: Int, loadingType: LoadingType) {
        if (shouldPreventRequest(loadingType)) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiState { currentState ->
                currentState.copy(
                    isLoading = loadingType == LoadingType.FULL_SCREEN,
                    isRefreshing = loadingType == LoadingType.PULL_TO_REFRESH,
                    isLoadingMore = loadingType == LoadingType.LOAD_MORE,
                    loadingType = loadingType,
                    isError = false // 清除之前的错误状态
                )
            }
            
            repository.getFavorites(page)
                .onEach { result -> 
                    handleDataResult(page, result, loadingType)
                }
                .onCompletion { fetchJob = null }
                .collect()
        }
    }
    
    private suspend fun handleDataResult(
        page: Int,
        result: Result<PageData<RecordInfo>?>,
        loadingType: LoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                currentPage = page
                val newRecords = updateRecordsList(page, pageData.records)

                updateUiState { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isError = false,
                        records = newRecords,
                        noMoreData = pageData.noMoreData()
                    )
                }
            }
            .onFailure { e ->
                if (!handleFailure(e)) {    // 通用错误处理(如远程登录), 如果处理过就不用再处理了
                    updateUiState { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            isError = true,
                            errorMessage = e.toUserFriendlyMessage(),
                            errorType = loadingType
                        )
                    }
                }
                AppLogger.w(TAG, "网络请求失败: ", e)
            }
    }

    private suspend fun updateRecordsList(page: Int, newRecords: List<RecordInfo>): List<RecordInfo> {
        return dataLock.withLock {
            if (page == 1) {
                // 首页或刷新 - 替换数据
                currentRecords.clear()
                currentRecords.addAll(newRecords)
            } else {
                // 加载更多 - 追加数据
                currentRecords.addAll(newRecords)
            }
            currentRecords.toList() // 返回不可变副本
        }
    }

    private fun updateUiState(update: (FavoriteUiState) -> FavoriteUiState) {
        val currentState = _uiState.value
        val newState = update(currentState)
        if (newState != currentState) {
            _uiState.value = newState
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
        private const val TAG: String = "FavoriteViewModel"
    }
}

class FavoriteViewModelFactory(
    private val repository: RecordRepository = RecordRepository.getInstance(RetrofitClient.apiService)
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
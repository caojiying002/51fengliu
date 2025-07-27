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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 单一UI状态
 * 包含暗巷列表页面所有需要的状态信息
 */
data class StreetListUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val streets: List<Street> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = "",
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasLoaded: Boolean = false, // 是否已经加载过数据
    /**
     * 当前城市代码状态说明：
     * - CITY_CODE_UNINITIALIZED("UNINITIALIZED"): 未初始化状态，ViewModel刚创建还未接收到任何城市选择
     * - CITY_CODE_ALL_CITIES(""): 已经读取过DataStore，用户未选择城市，需要加载所有城市的数据
     * - 具体城市代码: 用户已选择特定城市，加载该城市的数据
     */
    val cityCode: String = StreetListViewModel.CITY_CODE_UNINITIALIZED,
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showContent: Boolean get() = !isLoading && !isError && streets.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && streets.isEmpty() && hasLoaded
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && loadingType == LoadingType.FULL_SCREEN
}

sealed class StreetListIntent {
    data class UpdateCity(val cityCode: String) : StreetListIntent()
    data object Retry : StreetListIntent()
    data object Refresh : StreetListIntent()
    data object LoadMore : StreetListIntent()
}

class StreetListViewModel(
    private val sort: String,
    private val repository: StreetRepository
) : BaseViewModel() {
    private var fetchJob: Job? = null
    private val dataLock = Mutex()
    
    // 单一状态源 - 这是MVI的核心原则
    private val _uiState = MutableStateFlow(StreetListUiState())
    val uiState = _uiState.asStateFlow()
    
    // 内部状态管理
    @GuardedBy("dataLock")
    private var currentStreets: MutableList<Street> = mutableListOf()
    private var currentPage = 0

    fun processIntent(intent: StreetListIntent) {
        when (intent) {
            is StreetListIntent.UpdateCity -> updateCity(intent.cityCode)
            StreetListIntent.Retry -> retry()
            StreetListIntent.Refresh -> refresh()
            StreetListIntent.LoadMore -> loadMore()
        }
    }

    /**
     * 更新城市代码并重新加载数据
     * 
     * 城市代码参数说明：
     * - CITY_CODE_ALL_CITIES(""): 表示用户未选择城市，将加载所有城市的数据
     * - 具体城市代码: 表示用户选择了特定城市，将加载该城市的数据
     * 
     * 注意：此方法不会接收CITY_CODE_UNINITIALIZED，UI层会将null转换为CITY_CODE_ALL_CITIES("")
     */
    private fun updateCity(cityCode: String) {
        val currentState = _uiState.value
        if (currentState.cityCode == cityCode) return
        
        // 清除当前暗巷列表，避免显示旧数据
        clearStreetsBlocking()
        // 更新城市代码后加载数据
        _uiState.update { currentState ->
            currentState.copy(
                streets = emptyList(),
                cityCode = cityCode
            )
        }
        
        fetchData(cityCode = cityCode, page = 1, loadingType = LoadingType.FULL_SCREEN)
    }

    private fun retry() {
        val currentState = _uiState.value
        fetchData(cityCode = currentState.cityCode, page = 1, loadingType = LoadingType.FULL_SCREEN)
    }

    private fun refresh() {
        val currentState = _uiState.value
        fetchData(cityCode = currentState.cityCode, page = 1, loadingType = LoadingType.PULL_TO_REFRESH)
    }

    private fun loadMore() {
        val currentState = _uiState.value
        fetchData(cityCode = currentState.cityCode, page = currentPage + 1, loadingType = LoadingType.LOAD_MORE)
    }

    private fun clearStreetsBlocking() {
        runBlocking {
            dataLock.withLock {
                currentStreets.clear()
            }
        }
    }

    private fun fetchData(cityCode: String, page: Int, loadingType: LoadingType) {
        // 如果城市代码未初始化，则不执行请求
        if (cityCode == CITY_CODE_UNINITIALIZED) return
        
        if (shouldPreventRequest(loadingType)) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiStateToLoading(loadingType)
            
            repository.getStreets(cityCode, sort, page)
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
                AppLogger.w(TAG, "网络请求失败: ", e)
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
        private const val TAG: String = "StreetListViewModel"
        
        /**
         * 城市代码状态常量
         */
        const val CITY_CODE_UNINITIALIZED = "UNINITIALIZED"  // 未初始化状态
        const val CITY_CODE_ALL_CITIES = ""                  // 加载所有城市数据
    }

    class Factory(
        private val sort: String,
        private val repository: StreetRepository = StreetRepository.getInstance()
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StreetListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StreetListViewModel(sort, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
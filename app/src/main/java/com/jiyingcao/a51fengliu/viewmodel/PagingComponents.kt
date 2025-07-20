package com.jiyingcao.a51fengliu.viewmodel

import androidx.annotation.GuardedBy
import com.jiyingcao.a51fengliu.api.response.PageData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 通用分页状态数据类
 * 支持泛型，可复用于不同数据类型的分页场景
 */
data class PagingUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val errorType: LoadingType = LoadingType.FULL_SCREEN,
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false
) {
    // 通用派生状态
    val showContent: Boolean get() = !isLoading && !isError && items.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && items.isEmpty()
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && errorType == LoadingType.FULL_SCREEN
}

/**
 * 分页数据源策略接口
 * 将数据获取逻辑抽象化，支持不同的Repository实现
 */
interface PagingDataSource<T> {
    /**
     * 加载指定页码的数据
     * @param page 页码，从1开始
     * @param params 额外参数，如搜索关键词、城市代码等
     * @return 包含分页数据的Result
     */
    suspend fun loadPage(page: Int, params: Map<String, Any>? = null): Result<PageData<T>?>
}

/**
 * 分页状态管理器
 * 封装通用的分页逻辑，采用组合模式复用
 */
class PagingStateManager<T>(
    private val dataSource: PagingDataSource<T>,
    private val scope: CoroutineScope,
    private val handleFailure: suspend (Throwable) -> Boolean = { false }
) {
    private var fetchJob: Job? = null
    private val dataLock = Mutex()
    
    // 状态管理
    private val _uiState = MutableStateFlow(PagingUiState<T>())
    val uiState: StateFlow<PagingUiState<T>> = _uiState.asStateFlow()
    
    // 内部状态
    @GuardedBy("dataLock")
    private var currentItems: MutableList<T> = mutableListOf()
    private var currentPage = 0
    private var pendingInitialLoad = true
    
    /**
     * 处理通用分页Intent
     */
    fun processIntent(intent: BasePagingIntent, params: Map<String, Any>? = null) {
        when (intent) {
            is BasePagingIntent.InitialLoad -> initialLoad(params)
            is BasePagingIntent.Retry -> retry(params)
            is BasePagingIntent.Refresh -> refresh(params)
            is BasePagingIntent.LoadMore -> loadMore(params)
            else -> {
                // 处理其他可能的Intent实现（如SearchIntent的特有Intent）
                // 这些会被子类特有的processIntent方法处理
            }
        }
    }
    
    private fun initialLoad(params: Map<String, Any>? = null) {
        if (pendingInitialLoad) {
            loadData(page = 1, loadingType = LoadingType.FULL_SCREEN, params = params)
            pendingInitialLoad = false
        }
    }
    
    private fun retry(params: Map<String, Any>? = null) {
        loadData(page = 1, loadingType = LoadingType.FULL_SCREEN, params = params, clearItems = true)
    }
    
    private fun refresh(params: Map<String, Any>? = null) {
        loadData(page = 1, loadingType = LoadingType.PULL_TO_REFRESH, params = params)
    }
    
    private fun loadMore(params: Map<String, Any>? = null) {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || currentState.noMoreData) return
        
        loadData(page = currentPage + 1, loadingType = LoadingType.LOAD_MORE, params = params)
    }
    
    private fun loadData(
        page: Int,
        loadingType: LoadingType,
        params: Map<String, Any>? = null,
        clearItems: Boolean = false
    ) {
        if (shouldPreventRequest(loadingType)) return
        
        fetchJob?.cancel()
        fetchJob = scope.launch {
            if (clearItems) {
                clearCurrentItems()
            }
            
            // 更新加载状态
            updateUiStateToLoading(loadingType)
            
            try {
                val result = dataSource.loadPage(page, params)
                handleDataResult(page, result, loadingType)
            } catch (e: Exception) {
                handleLoadError(e, loadingType)
            }
        }
    }
    
    private suspend fun handleDataResult(
        page: Int,
        result: Result<PageData<T>?>,
        loadingType: LoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                currentPage = page
                val newItems = updateItemsList(page, pageData.records)
                updateUiStateToSuccess(newItems, pageData.isLastPage())
            }
            .onFailure { e ->
                handleLoadError(e, loadingType)
            }
    }
    
    private suspend fun handleLoadError(e: Throwable, loadingType: LoadingType) {
        if (!handleFailure(e)) {
            updateUiStateToError(e.message ?: "Unknown error", loadingType)
        }
    }
    
    private suspend fun updateItemsList(page: Int, newItems: List<T>): List<T> {
        return dataLock.withLock {
            if (page == 1) {
                // 首页或刷新 - 替换数据
                currentItems.clear()
                currentItems.addAll(newItems)
            } else {
                // 加载更多 - 追加数据
                currentItems.addAll(newItems)
            }
            currentItems.toList() // 返回不可变副本
        }
    }
    
    private suspend fun clearCurrentItems() {
        dataLock.withLock {
            currentItems.clear()
        }
        // 同步清理UI状态中的items，避免显示旧数据
        _uiState.update { currentState ->
            currentState.copy(items = emptyList())
        }
    }
    
    // ===== 专门的UI状态更新方法 =====
    
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
    
    private fun updateUiStateToSuccess(items: List<T>, noMoreData: Boolean = false) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = false,
                items = items,
                noMoreData = noMoreData
            )
        }
    }
    
    private fun updateUiStateToError(errorMessage: String, errorType: LoadingType) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = true,
                errorMessage = errorMessage,
                errorType = errorType
            )
        }
    }
    
    private fun shouldPreventRequest(loadingType: LoadingType): Boolean {
        val currentState = _uiState.value
        return when (loadingType) {
            LoadingType.FULL_SCREEN -> currentState.isLoading
            LoadingType.PULL_TO_REFRESH -> currentState.isRefreshing
            LoadingType.LOAD_MORE -> currentState.isLoadingMore || currentState.noMoreData
            else -> false
        }
    }
    
    /**
     * 清理资源
     */
    fun clear() {
        fetchJob?.cancel()
        currentPage = 0
        pendingInitialLoad = true
    }
    
    /**
     * 重置为初始状态（用于参数变化时）
     */
    fun reset() {
        fetchJob?.cancel()
        currentPage = 0
        pendingInitialLoad = true
        scope.launch {
            clearCurrentItems()
            _uiState.value = PagingUiState()
        }
    }
}

/**
 * 通用分页Intent基类
 */
sealed interface BasePagingIntent {
    data object InitialLoad : BasePagingIntent
    data object Retry : BasePagingIntent
    data object Refresh : BasePagingIntent
    data object LoadMore : BasePagingIntent
}
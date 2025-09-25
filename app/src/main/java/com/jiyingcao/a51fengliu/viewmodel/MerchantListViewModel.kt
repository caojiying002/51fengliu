package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.MerchantRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 单一UI状态
 * 包含商家列表页面所有需要的状态信息
 */
data class MerchantListUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val merchants: List<Merchant> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = "",
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val lastLoadedPage: Int = 0, // 最近成功加载的页码
    val hasLoaded: Boolean = false // 是否已经加载过数据
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showContent: Boolean get() = !isLoading && !isError && merchants.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && merchants.isEmpty() && hasLoaded
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && loadingType == LoadingType.FULL_SCREEN
    val nextPageToLoad: Int get() = lastLoadedPage + 1
}

sealed class MerchantListIntent {
    data object InitialLoad : MerchantListIntent()
    data object Retry : MerchantListIntent()
    data object Refresh : MerchantListIntent()
    data object LoadMore : MerchantListIntent()
}

@HiltViewModel
class MerchantListViewModel @Inject constructor(
    private val repository: MerchantRepository
) : BaseViewModel() {
    private var fetchJob: Job? = null

    // 单一状态源 - 这是MVI的核心原则
    private val _uiState = MutableStateFlow(MerchantListUiState())
    val uiState = _uiState.asStateFlow()
    
    // 内部状态管理
    private var pendingInitialLoad = true
    
    /**
     * 检查并加载第一页数据
     */
    private fun initialLoad() {
        if (pendingInitialLoad) {
            pendingInitialLoad = false
            fetchData(page = 1, loadingType = LoadingType.FULL_SCREEN)
        }
    }
    
    fun processIntent(intent: MerchantListIntent) {
        when (intent) {
            MerchantListIntent.InitialLoad -> initialLoad()
            MerchantListIntent.Retry -> retry()
            MerchantListIntent.Refresh -> refresh()
            MerchantListIntent.LoadMore -> loadMore()
        }
    }
    
    private fun fetchData(page: Int, loadingType: LoadingType) {
        if (shouldPreventRequest(loadingType)) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiStateToLoading(loadingType)
            
            repository.getMerchants(page)
                .onEach { result -> 
                    handleDataResult(page, result, loadingType)
                }
                .onCompletion { fetchJob = null }
                .collect()
        }
    }
    
    private suspend fun handleDataResult(
        page: Int,
        result: Result<PageData<Merchant>?>,
        loadingType: LoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                updateUiStateToSuccess(page, pageData.records, pageData.noMoreData(), loadingType)
            }
            .onFailure { e ->
                if (!handleFailure(e)) {    // 通用错误处理(如远程登录), 如果处理过就不用再处理了
                    updateUiStateToError(e.toUserFriendlyMessage(), loadingType)
                }
                AppLogger.w(TAG, "网络请求失败: ", e)
            }
    }

    private fun retry() {
        fetchData(page = 1, loadingType = LoadingType.FULL_SCREEN)
    }

    private fun refresh() {
        fetchData(page = 1, loadingType = LoadingType.PULL_TO_REFRESH)
    }

    private fun loadMore() {
        fetchData(page = uiState.value.nextPageToLoad, loadingType = LoadingType.LOAD_MORE)
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
     * @param page 当前页码，用于判断替换还是追加列表
     * @param newMerchants 商家列表
     * @param noMoreData 是否没有更多数据
     * @param loadingType 成功对应的加载类型，UI层可据此正确结束对应的加载状态
     */
    private fun updateUiStateToSuccess(
        page: Int,
        newMerchants: List<Merchant>,
        noMoreData: Boolean = false,
        loadingType: LoadingType
    ) {
        _uiState.update { currentState ->
            val mergedMerchants = if (page == 1) newMerchants else currentState.merchants + newMerchants
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = false,
                merchants = mergedMerchants,
                noMoreData = noMoreData,
                lastLoadedPage = page,
                hasLoaded = true,
                loadingType = loadingType
            )
        }
    }
    
    /**
     * 更新UI状态到错误状态
     * @param errorMessage 错误信息
     * @param loadingType 错误对应的加载类型，决定错误显示方式
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
        private const val TAG: String = "MerchantListViewModel"
    }
}
package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.util.getErrorMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 单一UI状态
 * 包含页面所有需要的状态信息
 */
data class HomeRecordListUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val records: List<RecordInfo> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = "",
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val lastLoadedPage: Int = 0, // 最近成功加载的页码
    val hasLoaded: Boolean = false // 是否已经加载过数据
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showContent: Boolean get() = !isLoading && !isError && records.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && records.isEmpty() && hasLoaded
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && loadingType == LoadingType.FULL_SCREEN
    val nextPageToLoad: Int get() = lastLoadedPage + 1
}

sealed class HomeRecordListIntent {
    data object InitialLoad : HomeRecordListIntent()
    data object Retry : HomeRecordListIntent()
    data object Refresh : HomeRecordListIntent()
    data object LoadMore : HomeRecordListIntent()
}

@HiltViewModel(assistedFactory = HomeRecordListViewModel.Factory::class)
class HomeRecordListViewModel @AssistedInject constructor(
    @Assisted private val sort: String,
    private val repository: RecordRepository
) : BaseViewModel() {
    private var fetchJob: Job? = null
    
    // 单一状态源 - 这是MVI的核心原则
    private val _uiState = MutableStateFlow(HomeRecordListUiState())
    val uiState = _uiState.asStateFlow()
    
    // 内部状态管理
    private var pendingInitialLoad = true

    fun processIntent(intent: HomeRecordListIntent) {
        when (intent) {
            HomeRecordListIntent.InitialLoad -> initialLoad()
            HomeRecordListIntent.Retry -> retry()
            HomeRecordListIntent.Refresh -> refresh()
            HomeRecordListIntent.LoadMore -> loadMore()
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
        fetchData(page = uiState.value.nextPageToLoad, loadingType = LoadingType.LOAD_MORE)
    }

    private fun fetchData(page: Int, loadingType: LoadingType) {
        if (shouldPreventRequest(loadingType)) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiStateToLoading(loadingType)
            
            val request = RecordsRequest.forHome(sort, page)
            repository.getRecords(request)
                .collect { result ->
                    handleDataResult(page, result, loadingType)
                    fetchJob = null
                }
        }
    }
    
    private suspend fun handleDataResult(
        page: Int,
        result: ApiResult<PageData<RecordInfo>>,
        loadingType: LoadingType
    ) {
        when (result) {
            is ApiResult.Success -> {
                updateUiStateToSuccess(page, result.data.records, result.data.noMoreData(), loadingType)
            }
            is ApiResult.ApiError -> {
                updateUiStateToError(result.message, loadingType)
            }
            is ApiResult.NetworkError -> {
                updateUiStateToError(result.getErrorMessage("网络连接失败"), loadingType)
            }
            is ApiResult.UnknownError -> {
                updateUiStateToError(result.getErrorMessage("未知错误"), loadingType)
            }
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
     * @param page 当前页码，用于判断替换还是追加列表
     * @param newRecords 本次请求返回的记录列表
     * @param noMoreData 是否没有更多数据
     * @param loadingType 成功的加载类型，UI层可据此正确结束对应的加载状态
     */
    private fun updateUiStateToSuccess(
        page: Int,
        newRecords: List<RecordInfo>, 
        noMoreData: Boolean = false,
        loadingType: LoadingType
    ) {
        _uiState.update { currentState ->
            val mergedRecords = if (page == 1) newRecords else currentState.records + newRecords
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = false,
                records = mergedRecords,
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
     * @param errorType 错误类型，决定错误显示方式
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

    @AssistedFactory
    interface Factory {
        fun create(sort: String): HomeRecordListViewModel
    }

    companion object {
        private const val TAG: String = "HomeRecordListViewModel"
    }
}
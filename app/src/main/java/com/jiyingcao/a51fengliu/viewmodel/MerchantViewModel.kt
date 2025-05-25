package com.jiyingcao.a51fengliu.viewmodel

import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class MerchantLoadingType {
    FULL_SCREEN,
    PULL_TO_REFRESH,
    LOAD_MORE
}

private fun MerchantLoadingType.toLoadingState(): MerchantState.Loading = when (this) {
    MerchantLoadingType.FULL_SCREEN -> MerchantState.Loading.FullScreen
    MerchantLoadingType.PULL_TO_REFRESH -> MerchantState.Loading.PullToRefresh
    MerchantLoadingType.LOAD_MORE -> MerchantState.Loading.LoadMore
}

private fun MerchantLoadingType.toErrorState(message: String): MerchantState.Error = when (this) {
    MerchantLoadingType.FULL_SCREEN -> MerchantState.Error.FullScreen(message)
    MerchantLoadingType.PULL_TO_REFRESH -> MerchantState.Error.PullToRefresh(message)
    MerchantLoadingType.LOAD_MORE -> MerchantState.Error.LoadMore(message)
}

sealed class MerchantState {
    data object Init : MerchantState()
    sealed class Loading : MerchantState() {
        data object FullScreen : Loading()
        data object PullToRefresh : Loading()
        data object LoadMore : Loading()
    }
    data object Success : MerchantState()
    sealed class Error(open val message: String) : MerchantState() {
        data class FullScreen(override val message: String) : Error(message)
        data class PullToRefresh(override val message: String) : Error(message)
        data class LoadMore(override val message: String) : Error(message)
    }
}

sealed class MerchantIntent {
    data object InitialLoad : MerchantIntent()
    data object Retry : MerchantIntent()
    data object Refresh : MerchantIntent()
    data object LoadMore : MerchantIntent()
}

class MerchantViewModel(
    private val repository: RecordRepository
) : BaseViewModel() {
    private var fetchJob: Job? = null
    
    private val _state = MutableStateFlow<MerchantState>(MerchantState.Init)
    val state = _state.asStateFlow()
    
    private val _noMoreDataState = MutableStateFlow(false)
    val noMoreDataState = _noMoreDataState.asStateFlow()

    /** 标记是否需要初始化加载，当首次UI可见时加载 */
    private var pendingInitialLoad = true
    
    /**
     * 加载成功的页数，每次加载成功后加1，用于加载更多时的页码计算。
     * 0表示没有加载成功过，1表示第一页加载成功，以此类推。
     */
    private val _pageLoaded = MutableStateFlow(0)
    
    /** 保护[data]列表，确保在多线程环境下的数据安全。 */
    private val dataLock = Mutex()
    
    /**
     * 存放Merchants的列表，刷新时清空。
     *
     * 【注意】应当使用[updateMerchants]方法修改这个列表，确保[_merchants]流每次都能获得更新。
     */
    @GuardedBy("dataLock")
    private var data: MutableList<Merchant> = mutableListOf()
    
    /**
     * 【注意】不要直接修改这个流，应当使用[updateMerchants]。
     */
    private val _merchants = MutableStateFlow<List<Merchant>>(emptyList())
    val merchants = _merchants.asStateFlow()
    
    /**
     * 修改[data]列表并同时更新[_merchants]流。
     */
    private suspend fun updateMerchants(operation: suspend (MutableList<Merchant>) -> Unit) {
        dataLock.withLock {
            operation(data)
            _merchants.value = data.toList()
        }
    }
    
    /**
     * 检查并加载第一页数据
     */
    private fun checkAndLoadPendingData() {
        // 如果有待初始化加载的数据，则加载第一页
        if (pendingInitialLoad) {
            fetchData(1)
            pendingInitialLoad = false
        }
    }
    
    fun processIntent(intent: MerchantIntent) {
        when (intent) {
            MerchantIntent.InitialLoad -> checkAndLoadPendingData()
            MerchantIntent.Retry -> retry()
            MerchantIntent.Refresh -> refresh()
            MerchantIntent.LoadMore -> loadMore()
        }
    }
    
    private fun fetchData(
        page: Int,
        loadingType: MerchantLoadingType = MerchantLoadingType.FULL_SCREEN
    ) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            _state.value = loadingType.toLoadingState()
            
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
        loadingType: MerchantLoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                _pageLoaded.value = if (page == 1) 1 else _pageLoaded.value + 1
                _state.value = MerchantState.Success
                _noMoreDataState.value = pageData.isLastPage()
                
                updateMerchants {
                    it.apply {
                        // 只有下拉刷新或初次加载时清空列表，其他情况直接添加
                        if (page == 1) clear()
                        addAll(pageData.records)
                    }
                }
            }
            .onFailure { e ->
                if (!handleFailure(e)) {
                    _state.value = loadingType.toErrorState(e.toUserFriendlyMessage())
                }
                AppLogger.w(TAG, "网络请求失败: ", e)
            }
    }

    private fun retry() {
        fetchData(1, MerchantLoadingType.FULL_SCREEN)
    }

    private fun refresh() {
        fetchData(1, MerchantLoadingType.PULL_TO_REFRESH)
    }

    private fun loadMore() {
        fetchData(_pageLoaded.value + 1, MerchantLoadingType.LOAD_MORE)
    }
    
    private fun clearMerchants() {
        viewModelScope.launch {
            updateMerchants { it.clear() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }

    companion object {
        private const val TAG: String = "MerchantViewModel"
    }
}

class MerchantViewModelFactory(
    private val repository: RecordRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MerchantViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MerchantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
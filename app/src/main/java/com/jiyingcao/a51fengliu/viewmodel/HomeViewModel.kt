package com.jiyingcao.a51fengliu.viewmodel

import android.util.Log
import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class HomeLoadingType {
    FULL_SCREEN,
    PULL_TO_REFRESH,
    LOAD_MORE
}

private fun HomeLoadingType.toLoadingState(): HomeState.Loading = when (this) {
    HomeLoadingType.FULL_SCREEN -> HomeState.Loading.FullScreen
    HomeLoadingType.PULL_TO_REFRESH -> HomeState.Loading.PullToRefresh
    HomeLoadingType.LOAD_MORE -> HomeState.Loading.LoadMore
}

private fun HomeLoadingType.toErrorState(message: String): HomeState.Error = when (this) {
    HomeLoadingType.FULL_SCREEN -> HomeState.Error.FullScreen(message)
    HomeLoadingType.PULL_TO_REFRESH -> HomeState.Error.PullToRefresh(message)
    HomeLoadingType.LOAD_MORE -> HomeState.Error.LoadMore(message)
}

sealed class HomeState {
    data object Init : HomeState()
    sealed class Loading : HomeState() {
        data object FullScreen : Loading()
        data object PullToRefresh : Loading()
        data object LoadMore : Loading()
    }
    data object Success : HomeState()
    sealed class Error(open val message: String) : HomeState() {
        data class FullScreen(override val message: String) : Error(message)
        data class PullToRefresh(override val message: String) : Error(message)
        data class LoadMore(override val message: String) : Error(message)
    }
}

sealed class HomeIntent {
    data object InitialLoad : HomeIntent()
    data object Retry : HomeIntent()
    data object Refresh : HomeIntent()
    data object LoadMore : HomeIntent()
}

class HomeViewModel(
    private val repository: RecordRepository,
    /** daily热门，publish最新 */
    val sort: String
) : BaseViewModel() {
    private var fetchJob: Job? = null
    
    private val _state = MutableStateFlow<HomeState>(HomeState.Init)
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
     * 存放Records的列表，刷新时清空。
     *
     * 【注意】应当使用[updateRecords]方法修改这个列表，确保[_records]流每次都能获得更新。
     */
    @GuardedBy("dataLock")
    private var data: MutableList<RecordInfo> = mutableListOf()
    
    /**
     * 【注意】不要直接修改这个流，应当使用[updateRecords]。
     */
    private val _records = MutableStateFlow<List<RecordInfo>>(emptyList())
    val records = _records.asStateFlow()
    
    /**
     * 修改[data]列表并同时更新[_records]流。
     */
    private suspend fun updateRecords(operation: suspend (MutableList<RecordInfo>) -> Unit) {
        dataLock.withLock {
            operation(data)
            _records.value = data.toList()
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
    fun processIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.InitialLoad -> checkAndLoadPendingData()
            HomeIntent.Retry -> retry()
            HomeIntent.Refresh -> refresh()
            HomeIntent.LoadMore -> loadMore()
        }
    }
    
    private fun fetchData(
        page: Int,
        loadingType: HomeLoadingType = HomeLoadingType.FULL_SCREEN
    ) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            _state.value = loadingType.toLoadingState()
            
            val request = RecordsRequest.forHome(sort, page)
            repository.getRecords(request)
                .onEach { result -> 
                    handleDataResult(request, result, loadingType)
                }
                .onCompletion { fetchJob = null }
                .collect()
        }
    }
    
    private suspend fun handleDataResult(
        request: RecordsRequest,
        result: Result<PageData?>,
        loadingType: HomeLoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                _pageLoaded.value = if (request.page == 1) 1 else _pageLoaded.value + 1
                _state.value = HomeState.Success
                _noMoreDataState.value = pageData.isLastPage()
                
                updateRecords {
                    it.apply {
                        // 只有下拉刷新或初次加载时清空列表，其他情况直接添加
                        if (request.page == 1) clear()
                        addAll(pageData.records)
                    }
                }
            }
            .onFailure { e ->
                if (!handleFailure(e)) {
                    _state.value = loadingType.toErrorState(e.toUserFriendlyMessage())
                }
                Log.w(TAG, "网络请求失败: ", e)
            }
    }

    private fun retry() {
        fetchData(1, HomeLoadingType.FULL_SCREEN)
    }

    private fun refresh() {
        fetchData(1, HomeLoadingType.PULL_TO_REFRESH)
    }

    private fun loadMore() {
        fetchData(_pageLoaded.value + 1, HomeLoadingType.LOAD_MORE)
    }
    
    private fun clearRecords() {
        viewModelScope.launch {
            updateRecords { it.clear() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }

    companion object {
        private const val TAG: String = "HomeViewModel"
    }
}

class HomeViewModelFactory(
    private val repository: RecordRepository,
    private val sort: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, sort) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
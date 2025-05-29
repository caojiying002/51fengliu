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
 * 我的收藏列表状态 - 采用密封类 + 接口的混合模式
 */
sealed class FavoriteState : BaseState {
    object Init : FavoriteState()
    data class Loading(override val loadingType: LoadingType) : FavoriteState(), LoadingState    // 实现通用加载状态接口
    object Success : FavoriteState()
    data class Error(
        override val message: String,
        override val errorType: LoadingType
    ) : FavoriteState(), ErrorState  // 实现通用错误状态接口
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
    
    private val _state = MutableStateFlow<FavoriteState>(FavoriteState.Init)
    val state = _state.asStateFlow()
    
    private val _noMoreDataState = MutableStateFlow(false)
    val noMoreDataState = _noMoreDataState.asStateFlow()

    /** 保护[data]列表，确保在多线程环境下的数据安全。 */
    private val dataLock = Mutex()
    
    /**
     * 存放Records的列表。
     * 【注意】应当使用[updateRecords]方法修改这个列表，确保[_records]流每次都能获得更新。
     */
    @GuardedBy("dataLock")
    private var data: MutableList<RecordInfo> = mutableListOf()
    
    /**
     * 【注意】不要直接修改这个流，应当使用[updateRecords]。
     */
    private val _records = MutableStateFlow<List<RecordInfo>>(emptyList())
    val records = _records.asStateFlow()
    
    /** 加载成功的页数，每次加载成功后加1，用于加载更多时的页码计算。 */
    private val _pageLoaded = MutableStateFlow(0)

    fun processIntent(intent: FavoriteIntent) {
        when (intent) {
            FavoriteIntent.InitialLoad -> fetchData(1)
            FavoriteIntent.Retry -> retry()
            FavoriteIntent.Refresh -> refresh()
            FavoriteIntent.LoadMore -> loadMore()
        }
    }

    private fun fetchData(
        page: Int,
        loadingType: LoadingType = LoadingType.FULL_SCREEN
    ) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            _state.value = loadingType.toLoadingState<FavoriteState>()
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
                _pageLoaded.value = if (page == 1) 1 else _pageLoaded.value + 1
                _state.value = FavoriteState.Success
                _noMoreDataState.value = pageData.isLastPage()
                
                updateRecords {
                    it.apply {
                        // 只有下拉刷新或初次加载时清空列表，其他情况直接添加
                        if (page == 1) clear()
                        addAll(pageData.records)
                    }
                }
            }
            .onFailure { e ->
                if (!handleFailure(e)) {
                    _state.value = loadingType.toErrorState<FavoriteState>(e.toUserFriendlyMessage())
                }
                AppLogger.w(TAG, "网络请求失败: ", e)
            }
    }

    private fun retry() {
        fetchData(1, LoadingType.FULL_SCREEN)
    }

    private fun refresh() {
        fetchData(1, LoadingType.PULL_TO_REFRESH)
    }

    private fun loadMore() {
        fetchData(_pageLoaded.value + 1, LoadingType.LOAD_MORE)
    }

    private suspend fun updateRecords(operation: suspend (MutableList<RecordInfo>) -> Unit) {
        dataLock.withLock {
            operation(data)
            _records.value = data.toList()
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
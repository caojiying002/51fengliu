package com.jiyingcao.a51fengliu.viewmodel

import androidx.annotation.GuardedBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.*
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class CityLoadingType {
    FULL_SCREEN,
    PULL_TO_REFRESH,
    LOAD_MORE
}

private fun CityLoadingType.toLoadingState(): CityState.Loading = when (this) {
    CityLoadingType.FULL_SCREEN -> CityState.Loading.FullScreen
    CityLoadingType.PULL_TO_REFRESH -> CityState.Loading.PullToRefresh
    CityLoadingType.LOAD_MORE -> CityState.Loading.LoadMore
}

private fun CityLoadingType.toErrorState(message: String): CityState.Error = when (this) {
    CityLoadingType.FULL_SCREEN -> CityState.Error.FullScreen(message)
    CityLoadingType.PULL_TO_REFRESH -> CityState.Error.PullToRefresh(message)
    CityLoadingType.LOAD_MORE -> CityState.Error.LoadMore(message)
}

sealed class CityState {
    data object Init : CityState()
    sealed class Loading : CityState() {
        data object FullScreen : Loading()
        data object PullToRefresh : Loading()
        data object LoadMore : Loading()
    }
    data object Success : CityState()
    sealed class Error(open val message: String) : CityState() {
        data class FullScreen(override val message: String) : Error(message)
        data class PullToRefresh(override val message: String) : Error(message)
        data class LoadMore(override val message: String) : Error(message)
    }
}

sealed class CityIntent {
    data class UpdateCity(val cityCode: String) : CityIntent()
    data object Retry : CityIntent()
    data object Refresh : CityIntent()
    data object LoadMore : CityIntent()
}

class CityViewModel(
    private val repository: RecordRepository,
    /** publish最新发布，weekly一周热门，monthly本月热门，lastMonth上月热门 */
    val sort: String
): BaseViewModel() {
    private var fetchJob: Job? = null
    
    private val _state = MutableStateFlow<CityState>(CityState.Init)
    val state = _state.asStateFlow()
    
    private val _noMoreDataState = MutableStateFlow(false)
    val noMoreDataState = _noMoreDataState.asStateFlow()

    private val _isUIVisible = MutableStateFlow(false)

    /** 标记城市是否发生了变化，需要重新加载数据 */
    private var pendingCityUpdate = false
    
    /** 保存当前城市代码 */
    private val _cityCode = MutableStateFlow<String?>(null)
    val cityCode = _cityCode.asStateFlow()
    
    /**
     * 加载成功的页数，每次加载成功后加1，用于加载更多时的页码计算。
     * 0表示没有加载成功过，1表示第一页加载成功，以此类推。
     */
    private val _pageLoaded = MutableStateFlow(0)
    
    /** 保护[data]列表，确保在多线程环境下的数据安全。 */
    private val dataLock = Mutex()
    
    /**
     * 存放Records的列表，更改关键词或者城市时清空。
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
    
    init {
        // 监听UI可见性变化，当变为可见且有待处理的城市更新时加载数据
        viewModelScope.launch {
            _isUIVisible.collect { isVisible ->
                if (isVisible) {
                    checkAndLoadPendingData()
                }
            }
        }
        
        // 监听城市代码变化
        viewModelScope.launch {
            _cityCode.collect { newCityCode ->
                newCityCode?.let {
                    // 重置页码和清空记录
                    _pageLoaded.value = 0
                    clearRecords()
                    
                    // 标记需要更新数据，但不立即加载
                    pendingCityUpdate = true
                    
                    // 仅当UI可见时才加载数据
                    if (_isUIVisible.value) {
                        fetchData(it, 1)
                        pendingCityUpdate = false
                    }
                    // 否则等待变为可见时加载
                }
            }
        }
    }
    
    /**
     * 修改[data]列表并同时更新[_records]流。
     */
    private suspend fun updateRecords(operation: suspend (MutableList<RecordInfo>) -> Unit) {
        dataLock.withLock {
            operation(data)
            _records.value = data.toList()
        }
    }
    
    fun processIntent(intent: CityIntent) {
        when (intent) {
            is CityIntent.UpdateCity -> updateCity(intent.cityCode)
            CityIntent.Retry -> retry()
            CityIntent.Refresh -> refresh()
            CityIntent.LoadMore -> loadMore()
        }
    }
    
    private fun updateCity(cityCode: String) {
        _cityCode.value = cityCode
        // 其他处理逻辑已移动到对_cityCode的流监听中，只有当城市代码变化时才刷新数据
    }
    
    /**
     * 检查并加载待处理的数据
     */
    private fun checkAndLoadPendingData() {
        val currentCityCode = _cityCode.value ?: return
        
        // 如果有待处理的城市更新
        if (pendingCityUpdate) {
            fetchData(currentCityCode, 1)
            pendingCityUpdate = false
        }
    }
    
    private fun fetchData(
        cityCode: String,
        page: Int,
        loadingType: CityLoadingType = CityLoadingType.FULL_SCREEN
    ) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            _state.value = loadingType.toLoadingState()
            
            val request = RecordsRequest.forCity(cityCode, sort, page)
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
        loadingType: CityLoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                _pageLoaded.value = if (request.page == 1) 1 else _pageLoaded.value + 1
                _state.value = CityState.Success
                _noMoreDataState.value = pageData.isLastPage()
                
                updateRecords {
                    it.apply {
                        // 只有下拉刷新时清空列表，其他情况直接添加
                        if (loadingType == CityLoadingType.PULL_TO_REFRESH) clear()
                        addAll(pageData.records)
                    }
                }
            }
            .onFailure { e ->
                if (!handleFailure(e)) {
                    _state.value = loadingType.toErrorState(e.toUserFriendlyMessage())
                }
            }
    }

    private fun retry() {
        fetchData(_cityCode.value!!, 1, CityLoadingType.FULL_SCREEN)
    }

    private fun refresh() {
        fetchData(_cityCode.value!!, 1, CityLoadingType.PULL_TO_REFRESH)
    }

    private fun loadMore() {
        fetchData(_cityCode.value!!, _pageLoaded.value + 1, CityLoadingType.LOAD_MORE)
    }
    
    private fun clearRecords() {
        viewModelScope.launch {
            updateRecords { it.clear() }
        }
    }

    fun setUIVisibility(isVisible: Boolean) {
        _isUIVisible.value = isVisible
    }
    
    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }

    companion object {
        private const val TAG: String = "CityViewModel"
    }
}

class CityViewModelFactory(
    private val repository: RecordRepository,
    private val sort: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CityViewModel(repository, sort) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
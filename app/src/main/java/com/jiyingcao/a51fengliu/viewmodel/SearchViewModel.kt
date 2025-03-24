package com.jiyingcao.a51fengliu.viewmodel

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class SearchLoadingType {
    FULL_SCREEN,
    PULL_TO_REFRESH,
    LOAD_MORE
}

private fun SearchLoadingType.toLoadingState(): SearchState.Loading = when (this) {
    SearchLoadingType.FULL_SCREEN -> SearchState.Loading.FullScreen
    SearchLoadingType.PULL_TO_REFRESH -> SearchState.Loading.PullToRefresh
    SearchLoadingType.LOAD_MORE -> SearchState.Loading.LoadMore
}

private fun SearchLoadingType.toErrorState(message: String): SearchState.Error = when (this) {
    SearchLoadingType.FULL_SCREEN -> SearchState.Error.FullScreen(message)
    SearchLoadingType.PULL_TO_REFRESH -> SearchState.Error.PullToRefresh(message)
    SearchLoadingType.LOAD_MORE -> SearchState.Error.LoadMore(message)
}

sealed class SearchState {
    data object Init : SearchState()
    sealed class Loading : SearchState() {
        data object FullScreen : Loading()
        data object PullToRefresh : Loading()
        data object LoadMore : Loading()
    }
    data object Success : SearchState() // 必要时也可以区分FullScreen、PullToRefresh、LoadMore等状态，现在暂时不需要
    sealed class Error(open val message: String) : SearchState() {
        data class FullScreen(override val message: String) : Error(message)
        data class PullToRefresh(override val message: String) : Error(message)
        data class LoadMore(override val message: String) : Error(message)
    }
}

sealed class SearchIntent {
    data class UpdateKeywords(val keywords: String) : SearchIntent()
    data class UpdateCityWithKeywords(val cityCode: String, val keywords: String) : SearchIntent()
    data object Refresh : SearchIntent()
    data object NextPage : SearchIntent()
    // TODO 几种样式的错误重试，如全屏错误重试、下拉加载错误重试等
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: RecordRepository
) : BaseViewModel() {
    private var searchJob: Job? = null

    private val _state: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.Init)
    val state = _state.asStateFlow()

    private val _noMoreDataState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val noMoreDataState = _noMoreDataState.asStateFlow()

    private val _keywords: MutableStateFlow<String?> = MutableStateFlow(null)
    val keywords = _keywords.asStateFlow()

    private val _cityCode: MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * 加载成功的页数，每次加载成功后加1，用于加载更多时的页码计算。
     * 0表示没有加载成功过，1表示第一页加载成功，以此类推。
     */
    private val _pagedLoaded = MutableStateFlow(0)

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

    /**
     * 修改[data]列表并同时更新[_records]流。
     */
    private suspend fun updateRecords(operation: suspend (MutableList<RecordInfo>) -> Unit) {
        dataLock.withLock {
            operation(data)
            _records.value = data.toList()
        }
    }

    fun processIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateKeywords -> updateKeywords(intent.keywords)
            is SearchIntent.UpdateCityWithKeywords -> updateCityWithKeywords(intent.cityCode, intent.keywords)
            SearchIntent.Refresh -> refresh()
            SearchIntent.NextPage -> nextPage()
        }
    }

    /**
     * 当前的交互方式是点击按钮触发搜索。
     * 如果将来改成由输入框文字变化触发，需要增加防抖处理。
     * （可以用delay/debounce/throttleFirst等操作符）
     */
    private fun updateKeywords(keywords: String) {
        if (_keywords.value == keywords) return

        _keywords.value = keywords
        search0(
            RecordsRequest.forSearch(
                keywords,
                _cityCode.value.orEmpty(),
                1
            ),
            SearchLoadingType.FULL_SCREEN,
            true    // 关键词有变化，清空之前的搜索结果
        )
    }

    private fun updateCityWithKeywords(cityCode: String, keywords: String) {
        var cityChanged = false
        var keywordsChanged = false

        if (_cityCode.value != cityCode) {
            cityChanged = true
            _cityCode.value = cityCode
        }
        if (_keywords.value != keywords) {
            keywordsChanged = true
            _keywords.value = keywords
        }

        // 无事发生
        if (!cityChanged && !keywordsChanged) return

        search0(
            RecordsRequest.forSearch(
                keywords,
                cityCode,
                1
            ),
            SearchLoadingType.FULL_SCREEN,
            true    // 城市或关键字有变化，清空之前的搜索结果
        )
    }

    private fun refresh() {
        search0(
            RecordsRequest.forSearch(
                _keywords.value.orEmpty(),
                _cityCode.value.orEmpty(),
                1
            ),
            SearchLoadingType.PULL_TO_REFRESH
        )
    }

    private fun nextPage() {
        search0(
            RecordsRequest.forSearch(
                _keywords.value.orEmpty(),
                _cityCode.value.orEmpty(),
                _pagedLoaded.value + 1
            ),
            SearchLoadingType.LOAD_MORE
        )
    }

    private fun search0(
        request: RecordsRequest,
        loadingType: SearchLoadingType = SearchLoadingType.FULL_SCREEN,
        clearRecordsBeforeSearch: Boolean = false
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            if (clearRecordsBeforeSearch) clearRecords()
            _state.value = loadingType.toLoadingState()
            repository.getRecords(request)
                .onEach { result ->
                    handleSearchResult(request, result, loadingType)
                }
                .onCompletion { searchJob = null }
                .collect()
        }
    }

    private suspend fun handleSearchResult(
        request: RecordsRequest,
        result: Result<PageData?>,
        loadingType: SearchLoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pagedData ->
                _pagedLoaded.value = if (request.page == 1) 1 else _pagedLoaded.value + 1
                _state.value = SearchState.Success
                _noMoreDataState.value = pagedData.isLastPage()

                updateRecords {
                    it.apply {
                        // 只有下拉刷新时清空列表，其他情况直接添加（更新关键字或城市时已经预先清除过列表）
                        if (loadingType == SearchLoadingType.PULL_TO_REFRESH) clear()
                        addAll(pagedData.records)
                    }
                }
            }.onFailure { e ->
                if (!handleFailure(e)) {
                    _state.value = loadingType.toErrorState(e.toUserFriendlyMessage())
                }
            }
    }

    private suspend fun clearRecords() {
        updateRecords { it.clear() }
    }

    private fun resetPage() {
        _pagedLoaded.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        resetPage()
        searchJob?.cancel()
    }

    companion object {
        const val TAG: String = "SearchViewModel"
    }
}

class SearchViewModelFactory(
    private val repository: RecordRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
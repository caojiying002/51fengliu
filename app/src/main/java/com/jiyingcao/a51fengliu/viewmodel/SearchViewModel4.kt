package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.viewmodel.DetailState.Loading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private enum class LoadingType1 {
    FULL_SCREEN,
    PULL_TO_REFRESH,
    LOAD_MORE,
    FLOAT
}

private fun LoadingType1.toLoadingState(): SearchState.Loading = when (this) {
    LoadingType1.FULL_SCREEN -> SearchState.Loading.FullScreen
    LoadingType1.PULL_TO_REFRESH -> SearchState.Loading.PullToRefresh
    LoadingType1.LOAD_MORE -> SearchState.Loading.LoadMore
    LoadingType1.FLOAT -> SearchState.Loading.Float
}

private fun LoadingType1.toErrorState(message: String): SearchState.Error = when (this) {
    LoadingType1.FULL_SCREEN -> SearchState.Error.FullScreen(message)
    LoadingType1.PULL_TO_REFRESH -> SearchState.Error.PullToRefresh(message)
    LoadingType1.LOAD_MORE -> SearchState.Error.LoadMore(message)
    LoadingType1.FLOAT -> SearchState.Error.Float(message)
}

sealed class RefreshState {
    object Init : RefreshState()
    object Refreshing : RefreshState()
    object RefreshSuccess : RefreshState()
    object RefreshError : RefreshState()
    object LoadingMore : RefreshState()
    object LoadMoreSuccess : RefreshState()
    object LoadMoreError : RefreshState()
}

sealed class SearchState {
    object Init : SearchState()
    sealed class Loading : SearchState() {
        object FullScreen : Loading()
        object PullToRefresh : Loading()
        object LoadMore : Loading()
        object Float : Loading()
    }
    data class Success(
        val pagedData: PageData, // TODO 是否可以用List<RecordInfo>代替？
        val isFirstPage: Boolean,
        val isLastPage: Boolean
    ) : SearchState()
    sealed class Error(open val message: String) : SearchState() {
        data class FullScreen(override val message: String) : Error(message)
        data class PullToRefresh(override val message: String) : Error(message)
        data class LoadMore(override val message: String) : Error(message)
        data class Float(override val message: String) : Error(message)
    }
}

sealed class SearchIntent {
    data class UpdateKeywords(val keywords: String) : SearchIntent()
    data class UpdateCity(val cityCode: String) : SearchIntent()
    object Refresh : SearchIntent()
    object NextPage : SearchIntent()
    // TODO 几种样式的错误重试，如全屏错误重试、下拉加载错误重试等
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel4(
    private val repository: RecordRepository
) : ViewModel() {

    private val _state: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.Init)
    val state = _state.asStateFlow()

    @Deprecated("Use _state instead")
    private val _refreshState: MutableStateFlow<RefreshState> = MutableStateFlow(RefreshState.Init)
    @Deprecated("Use state instead")
    val refreshState = _refreshState.asStateFlow()

    private val _noMoreDataState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val noMoreDataState = _noMoreDataState.asStateFlow()

    @Deprecated("Use _request and _cityCode instead")
    private val _request: MutableStateFlow<RecordsRequest?> = MutableStateFlow(null)

    private val _keywords: MutableStateFlow<String?> = MutableStateFlow(null)
    val keywords = _keywords.asStateFlow()

    private val _cityCode: MutableStateFlow<String?> = MutableStateFlow(null)

    private var pagedLoaded: Int = 0

    init {
        _request
            .filterNotNull()
            .distinctUntilChanged() // not necessary: state flows are always distinct
            //.debounce(300)
            .onEach { /*search0(
                request = it,
                refreshState = _refreshState.value,
            )*/ }
            .launchIn(viewModelScope)
    }

    fun processIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateKeywords -> updateKeywords(intent.keywords)
            is SearchIntent.UpdateCity -> updateCityCode(intent.cityCode)
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

        val cityCode = _cityCode.value ?: ""
        search1(
            RecordsRequest.forSearch(keywords, cityCode, 1),
            if (pagedLoaded > 0) LoadingType1.FLOAT else LoadingType1.FULL_SCREEN
        )
    }

    private fun updateCityCode(cityCode: String) {
        if (_cityCode.value == cityCode) return

        _cityCode.value = cityCode

        val keywords = _keywords.value ?: ""
        search1(
            RecordsRequest.forSearch(keywords, cityCode, 1),
            if (pagedLoaded > 0) LoadingType1.FLOAT else LoadingType1.FULL_SCREEN
        )
    }

    private fun refresh() {
        val keywords = _keywords.value ?: ""
        val cityCode = _cityCode.value ?: ""
        search1(
            RecordsRequest.forSearch(keywords, cityCode, 1),
            LoadingType1.PULL_TO_REFRESH
        )
    }

    private fun nextPage() {
        val keywords = _keywords.value ?: ""
        val cityCode = _cityCode.value ?: ""
        search1(
            RecordsRequest.forSearch(keywords, cityCode, pagedLoaded + 1),
            LoadingType1.LOAD_MORE
        )
    }

    private fun search0(
        request: RecordsRequest,
        loadingType: LoadingType1 = LoadingType1.FULL_SCREEN,
        refreshState: RefreshState = RefreshState.Init,
    ) {
        viewModelScope.launch(remoteLoginCoroutineContext) {
            _state.value = loadingType.toLoadingState()
            repository.getRecords(request)
                .collect { result ->
                    result.mapCatching { requireNotNull(it) }
                        .onSuccess { pagedData ->
                            pagedLoaded = if (request.page == 1) 1 else pagedLoaded + 1
                            _state.value = SearchState.Success(
                                pagedData,
                                pagedData.isFirstPage(),
                                pagedData.isLastPage()
                            )

                            _noMoreDataState.value = pagedData.isLastPage()

                            when (refreshState) {
                                RefreshState.Refreshing -> _refreshState.value = RefreshState.RefreshSuccess
                                RefreshState.LoadingMore -> _refreshState.value = RefreshState.LoadMoreSuccess
                                else -> Unit
                            }
                        }.onFailure { e ->
                            if (!handleFailure(e)) {
                                _state.value = loadingType.toErrorState(e.toUserFriendlyMessage())

                                when (refreshState) {
                                    RefreshState.Refreshing -> _refreshState.value = RefreshState.RefreshError
                                    RefreshState.LoadingMore -> _refreshState.value = RefreshState.LoadMoreError
                                    else -> Unit
                                }
                            }
                        }
                }
        }
    }

    // TODO Job应当可以取消
    private fun search1(
        request: RecordsRequest,
        loadingType: LoadingType1 = LoadingType1.FULL_SCREEN,
    ) {
        viewModelScope.launch(remoteLoginCoroutineContext) {
            _state.value = loadingType.toLoadingState()
            repository.getRecords(request)
                .collect { result ->
                    result.mapCatching { requireNotNull(it) }
                        .onSuccess { pagedData ->
                            pagedLoaded = if (request.page == 1) 1 else pagedLoaded + 1
                            _state.value = SearchState.Success(
                                pagedData,
                                pagedData.isFirstPage(),
                                pagedData.isLastPage()
                            )

                            _noMoreDataState.value = pagedData.isLastPage()
                        }.onFailure { e ->
                            if (!handleFailure(e)) {
                                _state.value = loadingType.toErrorState(e.toUserFriendlyMessage())
                            }
                        }
                }
        }
    }

    private fun resetPage() {
        pagedLoaded = 0
    }

    override fun onCleared() {
        super.onCleared()
        resetPage()
    }

    companion object {
        const val TAG: String = "SearchViewModel4"
    }
}

class SearchViewModelFactory(
    private val repository: RecordRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel4::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel4(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
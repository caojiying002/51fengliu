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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
        object Pagination : Loading()
    }
    data class Success(
        val pagedData: PageData, // TODO 是否可以用List<RecordInfo>代替？
        val isFirstPage: Boolean,
        val isLastPage: Boolean
    ) : SearchState()
    sealed class Error : SearchState() {
        data class FullScreen(val message: String) : Error()
        data class PullToRefresh(val message: String) : Error()
        data class Pagination(val message: String) : Error()
    }
}

private fun SearchState.Loading.correspondingError(error: Throwable): SearchState.Error = when (this) {
    SearchState.Loading.FullScreen -> SearchState.Error.FullScreen(error.toUserFriendlyMessage())
    SearchState.Loading.PullToRefresh -> SearchState.Error.PullToRefresh(error.toUserFriendlyMessage())
    SearchState.Loading.Pagination -> SearchState.Error.Pagination(error.toUserFriendlyMessage())
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

    private val _refreshState: MutableStateFlow<RefreshState> = MutableStateFlow(RefreshState.Init)
    val refreshState = _refreshState.asStateFlow()

    private val _noMoreDataState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val noMoreDataState = _noMoreDataState.asStateFlow()

    private val _request: MutableStateFlow<RecordsRequest?> = MutableStateFlow(null)

    private var pagedLoaded: Int = 0

    init {
        _request
            .filterNotNull()
            .distinctUntilChanged() // not necessary: state flows are always distinct
            //.debounce(300)
            .onEach { search0(
                request = it,
                refreshState = _refreshState.value,
            ) }
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
        if (_request.value?.keywords == keywords) return

        _refreshState.value = RefreshState.Init
        _request.update {
            it?.copy(keywords = keywords, page = 1)
                ?: RecordsRequest.forSearch(keywords = keywords, "", 1)
        }
    }

    private fun updateCityCode(cityCode: String) {
        if (_request.value?.cityCode == cityCode) return

        _refreshState.value = RefreshState.Init
        _request.update {
            it?.copy(cityCode = cityCode, page = 1)
                ?: RecordsRequest.forSearch("", cityCode = cityCode, 1)
        }
    }

    private fun refresh() {
        _refreshState.value = RefreshState.Refreshing
        _request.update {
            it?.copy(page = 1)
                ?: RecordsRequest.forSearch("", "", 1)
        }
    }

    private fun nextPage() {
        _refreshState.value = RefreshState.LoadingMore
        _request.update {
            it?.copy(page = pagedLoaded + 1)
                ?: RecordsRequest.forSearch("", "", 1)
        }
    }

    private fun search0(
        request: RecordsRequest,
        loadingState: SearchState.Loading = SearchState.Loading.FullScreen,
        refreshState: RefreshState = RefreshState.Init,
    ) {
        viewModelScope.launch(remoteLoginCoroutineContext) {
            _state.value = loadingState
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
                                _state.value = loadingState.correspondingError(e)

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
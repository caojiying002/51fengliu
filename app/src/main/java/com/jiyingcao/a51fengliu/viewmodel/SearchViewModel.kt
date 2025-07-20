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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 单一UI状态 - 搜索页所有状态信息
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val records: List<RecordInfo> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = "",
    val errorType: LoadingType = LoadingType.FULL_SCREEN,
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val keywords: String? = null, // 当前搜索关键词
    val hasSearched: Boolean = false // 是否已经进行过搜索
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showContent: Boolean get() = !isLoading && !isError && records.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && records.isEmpty() && hasSearched
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && errorType == LoadingType.FULL_SCREEN
    val showInitialState: Boolean get() = !hasSearched && !isLoading && !isError
}

sealed class SearchIntent {
    data class UpdateKeywords(val keywords: String) : SearchIntent()
    data class UpdateCityWithKeywords(val cityCode: String, val keywords: String) : SearchIntent()
    data object Refresh : SearchIntent()
    data object LoadMore : SearchIntent()
    data object Retry : SearchIntent()
}

class SearchViewModel(
    private val repository: RecordRepository
) : BaseViewModel() {
    private var searchJob: Job? = null

    // 单一状态源 - 这是MVI的核心原则
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    /** 保护[data]列表，确保在多线程环境下的数据安全。 */
    private val dataLock = Mutex()

    // 内部状态管理 - 不暴露给UI

    /**
     * 存放Records的列表，更改关键词或者城市时清空。
     *
     * 【注意】应当使用[updateRecords]方法修改这个列表，确保[_records]流每次都能获得更新。
     */
    @GuardedBy("dataLock")
    private var currentRecords: MutableList<RecordInfo> = mutableListOf()
    private var currentPage = 0
    private var currentCityCode: String? = null

    fun processIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.UpdateKeywords -> updateKeywords(intent.keywords)
            is SearchIntent.UpdateCityWithKeywords -> updateCityWithKeywords(intent.cityCode, intent.keywords)
            SearchIntent.Refresh -> refresh()
            SearchIntent.LoadMore -> loadMore()
            SearchIntent.Retry -> retry()
        }
    }

    /**
     * 当前的交互方式是点击按钮触发搜索。
     * 如果将来改成由输入框文字变化触发，需要增加防抖处理。
     * （可以用delay/debounce/throttleFirst等操作符）
     */
    private fun updateKeywords(keywords: String) {
        val currentState = _uiState.value
        if (currentState.keywords == keywords) return

        search(
            keywords = keywords,
            cityCode = currentCityCode.orEmpty(),
            page = 1,
            loadingType = LoadingType.FULL_SCREEN,
            clearRecords = true    // 关键词有变化，清空之前的搜索结果
        )
    }

    private fun updateCityWithKeywords(cityCode: String, keywords: String) {
        val currentState = _uiState.value
        val cityChanged = currentCityCode != cityCode
        val keywordsChanged = currentState.keywords != keywords

        // 无事发生
        if (!cityChanged && !keywordsChanged) return

        currentCityCode = cityCode
        search(
            keywords = keywords,
            cityCode = cityCode,
            page = 1,
            loadingType = LoadingType.FULL_SCREEN,
            clearRecords = true   // 城市或关键字有变化，清空之前的搜索结果
        )
    }

    private fun refresh() {
        val currentState = _uiState.value
        if (!currentState.hasSearched) return

        search(
            keywords = currentState.keywords.orEmpty(),
            cityCode = currentCityCode.orEmpty(),
            page = 1,
            loadingType = LoadingType.PULL_TO_REFRESH
        )
    }

    private fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || currentState.noMoreData || !currentState.hasSearched) return

        search(
            keywords = currentState.keywords.orEmpty(),
            cityCode = currentCityCode.orEmpty(),
            page = currentPage + 1,
            loadingType = LoadingType.LOAD_MORE
        )
    }

    private fun retry() {
        val currentState = _uiState.value
        if (!currentState.hasSearched) return

        search(
            keywords = currentState.keywords.orEmpty(),
            cityCode = currentCityCode.orEmpty(),
            page = 1,
            loadingType = LoadingType.FULL_SCREEN,
            clearRecords = true
        )
    }

    private fun search(
        keywords: String,
        cityCode: String,
        page: Int,
        loadingType: LoadingType,
        clearRecords: Boolean = false
    ) {
        if (shouldPreventRequest(loadingType)) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            if (clearRecords) {
                clearCurrentRecords()
            }

            // 更新加载状态
            updateUiStateToLoading(loadingType, keywords, hasSearched = true)

            val request = RecordsRequest.forSearch(keywords, cityCode, page)
            repository.getRecords(request)
                .onEach { result ->
                    handleSearchResult(page, result, loadingType)
                }
                .onCompletion { searchJob = null }
                .collect()
        }
    }

    private suspend fun handleSearchResult(
        page: Int,
        result: Result<PageData<RecordInfo>?>,
        loadingType: LoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                currentPage = page
                val newRecords = updateRecordsList(page, pageData.records)
                updateUiStateToSuccess(newRecords, pageData.noMoreData())
            }
            .onFailure { e ->
                if (!handleFailure(e)) { // 通用错误处理
                    updateUiStateToError(e.toUserFriendlyMessage(), loadingType)
                }
            }
    }

    private suspend fun updateRecordsList(page: Int, newRecords: List<RecordInfo>): List<RecordInfo> {
        return dataLock.withLock {
            if (page == 1) {
                // 首页或刷新 - 替换数据
                currentRecords.clear()
                currentRecords.addAll(newRecords)
            } else {
                // 加载更多 - 追加数据
                currentRecords.addAll(newRecords)
            }
            currentRecords.toList() // 返回不可变副本
        }
    }

    private suspend fun clearCurrentRecords() {
        dataLock.withLock {
            currentRecords.clear()
        }
        // 同步清理UI状态中的records，避免显示旧数据
        _uiState.update { currentState ->
            currentState.copy(records = emptyList())
        }
    }

    // ===== 专门的UI状态更新方法 - 语义明确 =====

    /**
     * 更新UI状态到加载中
     * @param loadingType 加载类型，决定显示哪种加载状态
     * @param keywords 当前搜索关键词
     * @param hasSearched 是否已经进行过搜索
     */
    private fun updateUiStateToLoading(
        loadingType: LoadingType,
        keywords: String,
        hasSearched: Boolean = false
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = loadingType == LoadingType.FULL_SCREEN,
                isRefreshing = loadingType == LoadingType.PULL_TO_REFRESH,
                isLoadingMore = loadingType == LoadingType.LOAD_MORE,
                loadingType = loadingType,
                isError = false, // 清除之前的错误状态
                keywords = keywords,
                hasSearched = hasSearched || currentState.hasSearched
            )
        }
    }

    /**
     * 更新UI状态到成功状态
     * @param records 搜索结果列表
     * @param noMoreData 是否没有更多数据
     */
    private fun updateUiStateToSuccess(records: List<RecordInfo>, noMoreData: Boolean = false) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = false,
                records = records,
                noMoreData = noMoreData
            )
        }
    }

    /**
     * 更新UI状态到错误状态
     * @param errorMessage 错误信息
     * @param errorType 错误类型，决定错误显示方式
     */
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
        searchJob?.cancel()
        currentPage = 0
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
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
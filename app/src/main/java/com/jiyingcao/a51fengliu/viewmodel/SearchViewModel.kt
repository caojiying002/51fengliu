package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 单一UI状态 - 搜索页所有状态信息
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val records: List<RecordInfo> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = "",
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val keywords: String? = null, // 当前搜索关键词
    val cityCode: String = "", // 当前城市代码
    val lastLoadedPage: Int = 0, // 最近成功加载的页码
    val hasSearched: Boolean = false // 是否已经进行过搜索
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showContent: Boolean get() = !isLoading && !isError && records.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && records.isEmpty() && hasSearched
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && loadingType == LoadingType.FULL_SCREEN
    val showInitialState: Boolean get() = !hasSearched && !isLoading && !isError
    val nextPageToLoad: Int get() = lastLoadedPage + 1
}

sealed class SearchIntent {
    data class UpdateKeywords(val keywords: String) : SearchIntent()
    data class UpdateCityWithKeywords(val cityCode: String, val keywords: String) : SearchIntent()
    data object Refresh : SearchIntent()
    data object LoadMore : SearchIntent()
    data object Retry : SearchIntent()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: RecordRepository
) : BaseViewModel() {
    private var searchJob: Job? = null

    // 单一状态源 - 这是MVI的核心原则
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

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

        // 更新关键词并清空记录显示
        _uiState.update { currentState ->
            currentState.copy(
                records = emptyList(),
                keywords = keywords,
                lastLoadedPage = 0
            )
        }

        search(
            keywords = keywords,
            cityCode = currentState.cityCode,
            page = 1,
            loadingType = LoadingType.FULL_SCREEN
        )
    }

    private fun updateCityWithKeywords(cityCode: String, keywords: String) {
        val currentState = _uiState.value
        val cityChanged = currentState.cityCode != cityCode
        val keywordsChanged = currentState.keywords != keywords

        // 无事发生
        if (!cityChanged && !keywordsChanged) return

        // 更新城市和关键词并清空记录显示
        _uiState.update { currentState ->
            currentState.copy(
                records = emptyList(),
                keywords = keywords,
                cityCode = cityCode,
                lastLoadedPage = 0
            )
        }

        search(
            keywords = keywords,
            cityCode = cityCode,
            page = 1,
            loadingType = LoadingType.FULL_SCREEN
        )
    }

    private fun refresh() {
        val currentState = _uiState.value
        search(
            keywords = currentState.keywords.orEmpty(),
            cityCode = currentState.cityCode,
            page = 1,
            loadingType = LoadingType.PULL_TO_REFRESH
        )
    }

    private fun loadMore() {
        val currentState = _uiState.value
        search(
            keywords = currentState.keywords.orEmpty(),
            cityCode = currentState.cityCode,
            page = currentState.nextPageToLoad,
            loadingType = LoadingType.LOAD_MORE
        )
    }

    private fun retry() {
        val currentState = _uiState.value
        search(
            keywords = currentState.keywords.orEmpty(),
            cityCode = currentState.cityCode,
            page = 1,
            loadingType = LoadingType.FULL_SCREEN
        )
    }

    private fun search(
        keywords: String,
        cityCode: String,
        page: Int,
        loadingType: LoadingType
    ) {
        if (shouldPreventRequest(loadingType)) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiStateToLoading(loadingType, keywords, cityCode, hasSearched = true)

            val request = RecordsRequest.forSearch(keywords, cityCode, page)
            repository.getRecords(request)
                .onEach { result ->
                    handleDataResult(page, result, loadingType)
                }
                .onCompletion { searchJob = null }
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
                updateUiStateToSuccess(page, pageData.records, pageData.noMoreData(), loadingType)
            }
            .onFailure { e ->
                if (!handleFailure(e)) { // 通用错误处理
                    updateUiStateToError(e.toUserFriendlyMessage(), loadingType)
                }
                AppLogger.w(TAG, "网络请求失败: ", e)
            }
    }

    // ===== 专门的UI状态更新方法 - 语义明确 =====

    /**
     * 更新UI状态到加载中
     * @param loadingType 加载类型，决定显示哪种加载状态
     * @param keywords 当前搜索关键词
     * @param cityCode 当前城市代码
     * @param hasSearched 是否已经进行过搜索
     */
    private fun updateUiStateToLoading(
        loadingType: LoadingType,
        keywords: String,
        cityCode: String,
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
                cityCode = cityCode,
                hasSearched = hasSearched || currentState.hasSearched
            )
        }
    }

    /**
     * 更新UI状态到成功状态
     * @param page 当前页码，用于判断替换还是追加列表
     * @param newRecords 本次请求返回的记录列表
     * @param noMoreData 是否没有更多数据
     * @param loadingType 成功对应的加载类型，UI层可据此正确结束对应的加载状态
     */
    private fun updateUiStateToSuccess(
        page: Int,
        newRecords: List<RecordInfo>,
        noMoreData: Boolean = false,
        loadingType: LoadingType
    ) {
        _uiState.update { currentState ->
            val mergedRecords = if (page == 1) {
                newRecords
            } else {
                currentState.records + newRecords
            }
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = false,
                records = mergedRecords,
                noMoreData = noMoreData,
                lastLoadedPage = page,
                hasSearched = true,
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
        searchJob?.cancel()
    }

    companion object {
        const val TAG: String = "SearchViewModel"
    }
}
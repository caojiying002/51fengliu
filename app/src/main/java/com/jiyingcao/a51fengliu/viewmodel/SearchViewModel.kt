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
import com.jiyingcao.a51fengliu.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val hasSearched: Boolean = false // 是否已经进行过搜索
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showContent: Boolean get() = !isLoading && !isError && records.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && records.isEmpty() && hasSearched
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && loadingType == LoadingType.FULL_SCREEN
    val showInitialState: Boolean get() = !hasSearched && !isLoading && !isError
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

    /** 保护[data]列表，确保在多线程环境下的数据安全。 */
    private val dataLock = Mutex()

    // 内部状态管理 - 不暴露给UI

    /**
     * 存放Records的列表，更改关键词或者城市时清空。
     *
     * 【注意】应当使用[updateRecordsList]方法修改这个列表，确保数据同步更新。
     */
    @GuardedBy("dataLock")
    private var currentRecords: MutableList<RecordInfo> = mutableListOf()
    private var currentPage = 0

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

        // 清除当前记录，避免显示旧数据
        clearRecordsBlocking()
        // 更新关键词并清空记录显示
        _uiState.update { currentState ->
            currentState.copy(
                records = emptyList(),
                keywords = keywords
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

        // 清除当前记录，避免显示旧数据
        clearRecordsBlocking()
        // 更新城市和关键词并清空记录显示
        _uiState.update { currentState ->
            currentState.copy(
                records = emptyList(),
                keywords = keywords,
                cityCode = cityCode
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
            page = currentPage + 1,
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

    private fun clearRecordsBlocking() {
        runBlocking {
            dataLock.withLock {
                currentRecords.clear()
            }
        }
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
                currentPage = page
                val newRecords = updateRecordsList(page, pageData.records)
                updateUiStateToSuccess(newRecords, pageData.noMoreData(), loadingType)
            }
            .onFailure { e ->
                if (!handleFailure(e)) { // 通用错误处理
                    updateUiStateToError(e.toUserFriendlyMessage(), loadingType)
                }
                AppLogger.w(TAG, "网络请求失败: ", e)
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
     * @param records 记录列表
     * @param noMoreData 是否没有更多数据
     * @param loadingType 成功对应的加载类型，UI层可据此正确结束对应的加载状态
     */
    private fun updateUiStateToSuccess(
        records: List<RecordInfo>,
        noMoreData: Boolean = false,
        loadingType: LoadingType
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = false,
                records = records,
                noMoreData = noMoreData,
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
        currentPage = 0
    }

    companion object {
        const val TAG: String = "SearchViewModel"
    }
}
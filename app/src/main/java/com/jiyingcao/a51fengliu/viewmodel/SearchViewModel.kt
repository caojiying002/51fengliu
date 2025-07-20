package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.repository.RecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * 搜索页面的分页数据源实现
 */
private class SearchDataSource(
    private val repository: RecordRepository
) : PagingDataSource<RecordInfo> {
    override suspend fun loadPage(page: Int, params: Map<String, Any>?): Result<PageData<RecordInfo>?> {
        val keywords = params?.get("keywords") as? String ?: ""
        val cityCode = params?.get("cityCode") as? String ?: ""
        val request = RecordsRequest.forSearch(keywords, cityCode, page)
        return repository.getRecords(request).first()
    }
}

/**
 * 搜索页面状态 - 使用组合模式
 */
data class SearchUiState(
    val pagingState: PagingUiState<RecordInfo> = PagingUiState(),
    val keywords: String? = null,
    val hasSearched: Boolean = false
) {
    // 委托通用属性给pagingState
    val records: List<RecordInfo> get() = pagingState.items
    val isLoading: Boolean get() = pagingState.isLoading
    val isError: Boolean get() = pagingState.isError
    val errorMessage: String get() = pagingState.errorMessage
    val noMoreData: Boolean get() = pagingState.noMoreData
    val isRefreshing: Boolean get() = pagingState.isRefreshing
    val isLoadingMore: Boolean get() = pagingState.isLoadingMore
    val loadingType: LoadingType get() = pagingState.loadingType
    val errorType: LoadingType get() = pagingState.errorType
    
    // 派生状态
    val showContent: Boolean get() = pagingState.showContent
    val showEmpty: Boolean get() = pagingState.showEmpty && hasSearched
    val showFullScreenLoading: Boolean get() = pagingState.showFullScreenLoading
    val showFullScreenError: Boolean get() = pagingState.showFullScreenError
    val showInitialState: Boolean get() = !hasSearched && !isLoading && !isError
}

/**
 * 搜索页面Intent - 扩展通用分页Intent
 */
sealed interface SearchIntent : BasePagingIntent {
    data class UpdateKeywords(val keywords: String) : SearchIntent
    data class UpdateCityWithKeywords(val cityCode: String, val keywords: String) : SearchIntent
}

class SearchViewModel(
    private val repository: RecordRepository
) : BaseViewModel() {
    
    // 使用组合模式 - 核心分页逻辑委托给PagingStateManager
    private val dataSource = SearchDataSource(repository)
    private val pagingManager = PagingStateManager(
        dataSource = dataSource,
        scope = viewModelScope.plus(remoteLoginCoroutineContext),
        handleFailure = ::handleFailure
    )
    
    // 简单的单一状态源 - 直接管理完整的SearchUiState
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    
    // 内部状态管理
    private var currentCityCode: String? = null
    
    init {
        // 监听分页状态变化，同步更新到SearchUiState
        viewModelScope.launch {
            pagingManager.uiState.collect { pagingState ->
                _uiState.update { currentState ->
                    currentState.copy(pagingState = pagingState)
                }
            }
        }
    }

    fun processIntent(intent: BasePagingIntent) {
        when (intent) {
            is SearchIntent.UpdateKeywords -> updateKeywords(intent.keywords)
            is SearchIntent.UpdateCityWithKeywords -> updateCityWithKeywords(intent.cityCode, intent.keywords)
            // 通用分页Intent直接转发给分页管理器
            else -> {
                val currentState = _uiState.value
                if (currentState.hasSearched) {
                    val params = buildSearchParams(currentState.keywords, currentCityCode)
                    pagingManager.processIntent(intent, params)
                }
            }
        }
    }

    /**
     * 当前的交互方式是点击按钮触发搜索。
     * 如果将来改成由输入框文字变化触发，需要增加防抖处理。
     */
    private fun updateKeywords(keywords: String) {
        val currentState = _uiState.value
        if (currentState.keywords == keywords) return

        updateSearchState(keywords = keywords, hasSearched = true)
        startNewSearch(keywords, currentCityCode.orEmpty())
    }

    private fun updateCityWithKeywords(cityCode: String, keywords: String) {
        val currentState = _uiState.value
        val cityChanged = currentCityCode != cityCode
        val keywordsChanged = currentState.keywords != keywords

        if (!cityChanged && !keywordsChanged) return

        currentCityCode = cityCode
        updateSearchState(keywords = keywords, hasSearched = true)
        startNewSearch(keywords, cityCode)
    }

    private fun startNewSearch(keywords: String, cityCode: String) {
        // 重置分页管理器，开始新的搜索
        pagingManager.reset()
        val params = buildSearchParams(keywords, cityCode)
        pagingManager.processIntent(BasePagingIntent.InitialLoad, params)
    }

    private fun buildSearchParams(keywords: String?, cityCode: String?): Map<String, Any> {
        return mapOf(
            "keywords" to (keywords ?: ""),
            "cityCode" to (cityCode ?: "")
        )
    }

    private fun updateSearchState(keywords: String, hasSearched: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                keywords = keywords,
                hasSearched = hasSearched
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pagingManager.clear()
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
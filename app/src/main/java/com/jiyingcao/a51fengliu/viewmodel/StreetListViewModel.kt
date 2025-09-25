package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.Street
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.StreetRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 单一UI状态
 * 包含暗巷列表页面所有需要的状态信息
 */
data class StreetListUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val streets: List<Street> = emptyList(),
    val isError: Boolean = false,
    val errorMessage: String = "",
    val noMoreData: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val lastLoadedPage: Int = 0,
    val hasLoaded: Boolean = false, // 是否已经加载过数据
    /**
     * 当前城市代码状态说明：
     * - CITY_CODE_UNINITIALIZED("UNINITIALIZED"): 未初始化状态，ViewModel刚创建还未接收到任何城市选择
     * - CITY_CODE_ALL_CITIES(""): 已经读取过DataStore，用户未选择城市，需要加载所有城市的数据
     * - 具体城市代码: 用户已选择特定城市，加载该城市的数据
     */
    val cityCode: String = StreetListViewModel.CITY_CODE_UNINITIALIZED,
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showContent: Boolean get() = !isLoading && !isError && streets.isNotEmpty()
    val showEmpty: Boolean get() = !isLoading && !isError && streets.isEmpty() && hasLoaded
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showFullScreenError: Boolean get() = isError && loadingType == LoadingType.FULL_SCREEN
    val nextPageToLoad: Int get() = lastLoadedPage + 1
}

sealed class StreetListIntent {
    data class UpdateCity(val cityCode: String) : StreetListIntent()
    data object Retry : StreetListIntent()
    data object Refresh : StreetListIntent()
    data object LoadMore : StreetListIntent()
}

@HiltViewModel(assistedFactory = StreetListViewModel.Factory::class)
class StreetListViewModel @AssistedInject constructor(
    @Assisted private val sort: String,
    private val repository: StreetRepository
) : BaseViewModel() {
    private var fetchJob: Job? = null

    
    // 单一状态源 - 这是MVI的核心原则
    private val _uiState = MutableStateFlow(StreetListUiState())
    val uiState = _uiState.asStateFlow()

    fun processIntent(intent: StreetListIntent) {
        when (intent) {
            is StreetListIntent.UpdateCity -> updateCity(intent.cityCode)
            StreetListIntent.Retry -> retry()
            StreetListIntent.Refresh -> refresh()
            StreetListIntent.LoadMore -> loadMore()
        }
    }

    /**
     * 更新城市代码并重新加载数据
     * 
     * 城市代码参数说明：
     * - CITY_CODE_ALL_CITIES(""): 表示用户未选择城市，将加载所有城市的数据
     * - 具体城市代码: 表示用户选择了特定城市，将加载该城市的数据
     * 
     * 注意：此方法不会接收CITY_CODE_UNINITIALIZED，UI层会将null转换为CITY_CODE_ALL_CITIES("")
     */
    private fun updateCity(cityCode: String) {
        val currentState = _uiState.value
        if (currentState.cityCode == cityCode) return

        // 更新城市代码后加载数据
        _uiState.update { currentState ->
            currentState.copy(
                streets = emptyList(),
                cityCode = cityCode
            )
        }
        
        fetchData(cityCode = cityCode, page = 1, loadingType = LoadingType.FULL_SCREEN)
    }

    private fun retry() {
        val currentState = _uiState.value
        fetchData(cityCode = currentState.cityCode, page = 1, loadingType = LoadingType.FULL_SCREEN)
    }

    private fun refresh() {
        val currentState = _uiState.value
        fetchData(cityCode = currentState.cityCode, page = 1, loadingType = LoadingType.PULL_TO_REFRESH)
    }

    private fun loadMore() {
        val currentState = _uiState.value
        fetchData(cityCode = currentState.cityCode, page = currentState.nextPageToLoad, loadingType = LoadingType.LOAD_MORE)
    }

    private fun fetchData(cityCode: String, page: Int, loadingType: LoadingType) {
        // 如果城市代码未初始化，则不执行请求
        if (cityCode == CITY_CODE_UNINITIALIZED) return
        
        if (shouldPreventRequest(loadingType)) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiStateToLoading(loadingType)
            
            repository.getStreets(cityCode, sort, page)
                .onEach { result -> 
                    handleDataResult(page, result, loadingType)
                }
                .onCompletion { fetchJob = null }
                .collect()
        }
    }
    
    private suspend fun handleDataResult(
        page: Int,
        result: Result<PageData<Street>?>,
        loadingType: LoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { pageData ->
                updateUiStateToSuccess(page, pageData.records, pageData.noMoreData(), loadingType)
            }
            .onFailure { e ->
                if (!handleFailure(e)) {    // 通用错误处理(如远程登录), 如果处理过就不用再处理了
                    updateUiStateToError(e.toUserFriendlyMessage(), loadingType)
                }
                AppLogger.w(TAG, "网络请求失败: ", e)
            }
    }

    // ===== 专门的UI状态更新方法 =====
    
    /**
     * 更新UI状态到加载中
     * @param loadingType 加载类型，决定显示哪种加载状态
     */
    private fun updateUiStateToLoading(loadingType: LoadingType) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = loadingType == LoadingType.FULL_SCREEN,
                isRefreshing = loadingType == LoadingType.PULL_TO_REFRESH,
                isLoadingMore = loadingType == LoadingType.LOAD_MORE,
                loadingType = loadingType,
                isError = false // 清除之前的错误状态
            )
        }
    }
    
    /**
     * 更新UI状态到成功状态
     * @param page 当前页码
     * @param newStreets 本次请求返回的暗巷列表
     * @param noMoreData 是否没有更多数据
     * @param loadingType 成功对应的加载类型，UI层可据此正确结束对应的加载状态
     */
    private fun updateUiStateToSuccess(
        page: Int,
        newStreets: List<Street>,
        noMoreData: Boolean = false,
        loadingType: LoadingType
    ) {
        val mergedStreets = if (page == 1) newStreets else _uiState.value.streets + newStreets
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                isError = false,
                streets = mergedStreets,
                noMoreData = noMoreData,
                lastLoadedPage = page,
                hasLoaded = true, // 标记已经加载过数据
                loadingType = loadingType // 保留加载类型，便于UI层正确处理
            )
        }
    }
    
    /**
     * 更新UI状态到错误状态
     * @param errorMessage 错误信息
     * @param loadingType 错误类型，决定错误显示方式
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
        fetchJob?.cancel()
    }

    @AssistedFactory
    interface Factory {
        fun create(sort: String) : StreetListViewModel
    }

    companion object {
        private const val TAG: String = "StreetListViewModel"
        
        /**
         * 城市代码状态常量
         */
        const val CITY_CODE_UNINITIALIZED = "UNINITIALIZED"  // 未初始化状态
        const val CITY_CODE_ALL_CITIES = ""                  // 加载所有城市数据
    }
}
package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 单一UI状态 - 商家详情页所有状态信息
 */
data class MerchantDetailUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val merchant: Merchant? = null,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val errorType: LoadingType = LoadingType.FULL_SCREEN,
    val isRefreshing: Boolean = false,
    val isOverlayLoading: Boolean = false
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showOverlayLoading: Boolean get() = isOverlayLoading
    val showContent: Boolean get() = !isLoading && !isError && merchant != null
    val showFullScreenError: Boolean get() = isError && errorType == LoadingType.FULL_SCREEN
    val hasData: Boolean get() = merchant != null
}

sealed class MerchantDetailIntent {
    object LoadDetail : MerchantDetailIntent()
    object PullToRefresh : MerchantDetailIntent()
    object Retry : MerchantDetailIntent()
    // 从未登录态跳转登录页面登录成功返回到本页面，本页面再次可见(onStart/onResume)时需要重新加载数据
    object HandleLoginSuccess : MerchantDetailIntent()
}

class MerchantDetailViewModel(
    private val merchantId: String,
    private val repository: RecordRepository = RecordRepository.getInstance(RetrofitClient.apiService),
    //private val tokenManager: TokenManager
) : BaseViewModel() {
    private var fetchJob: Job? = null

    // 单一状态源 - 商家详情页的所有UI状态
    private val _uiState = MutableStateFlow(MerchantDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var pendingInitialLoad = true

    fun processIntent(intent: MerchantDetailIntent) {
        when (intent) {
            MerchantDetailIntent.LoadDetail -> initialLoad()
            MerchantDetailIntent.PullToRefresh -> pullToRefresh()
            MerchantDetailIntent.Retry -> retry()
            MerchantDetailIntent.HandleLoginSuccess -> handleLoginSuccess()
        }
    }

    private fun loadDetail(loadingType: LoadingType = LoadingType.FULL_SCREEN) {
        // 避免重复加载
        if (shouldPreventRequest(loadingType)) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiState { currentState ->
                currentState.copy(
                    isLoading = loadingType == LoadingType.FULL_SCREEN,
                    isRefreshing = loadingType == LoadingType.PULL_TO_REFRESH,
                    isOverlayLoading = loadingType == LoadingType.OVERLAY,
                    loadingType = loadingType,
                    isError = false // 清除之前的错误状态
                )
            }

            repository.getMerchantDetail(merchantId)
                .collect { result ->
                    handleDataResult(result, loadingType)
                }
        }
    }

    private suspend fun handleDataResult(
        result: Result<Merchant?>,
        loadingType: LoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { merchant ->
                updateUiState { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isOverlayLoading = false,
                        isError = false,
                        merchant = merchant
                    )
                }
            }
            .onFailure { e ->
                if (!handleFailure(e)) { // 通用错误处理，如果处理过就不用再处理了
                    updateUiState { currentState ->
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isOverlayLoading = false,
                            isError = true,
                            errorMessage = e.toUserFriendlyMessage(),
                            errorType = loadingType
                        )
                    }
                }
            }
    }

    private fun initialLoad() {
        // 避免UI发生配置更改时ViewModel重新加载数据
        if (pendingInitialLoad) {
            loadDetail(LoadingType.FULL_SCREEN)
            pendingInitialLoad = false
        }
    }

    private fun pullToRefresh() {
        loadDetail(LoadingType.PULL_TO_REFRESH)
    }

    private fun retry() {
        loadDetail(LoadingType.FULL_SCREEN)
    }

    private fun handleLoginSuccess() {
        // TODO: 实现登录成功后的处理逻辑
        // 可能需要重新加载数据以获取登录后的权限状态
        loadDetail(LoadingType.OVERLAY)
    }

    /** 防止重复请求 */
    private fun shouldPreventRequest(loadingType: LoadingType): Boolean {
        val currentState = _uiState.value
        return when (loadingType) {
            LoadingType.FULL_SCREEN -> currentState.isLoading
            LoadingType.PULL_TO_REFRESH -> currentState.isRefreshing
            LoadingType.OVERLAY -> currentState.isOverlayLoading
            else -> false
        }
    }

    // 线程安全的状态更新
    private fun updateUiState(update: (MerchantDetailUiState) -> MerchantDetailUiState) {
        val currentState = _uiState.value
        val newState = update(currentState)
        if (newState != currentState) {
            _uiState.value = newState
        }
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }

    companion object {
        private const val TAG: String = "MerchantDetailViewModel"
    }
}

class MerchantDetailViewModelFactory(
    private val merchantId: String,
    private val repository: RecordRepository = RecordRepository.getInstance(RetrofitClient.apiService),
    //private val tokenManager: TokenManager
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MerchantDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MerchantDetailViewModel(merchantId, repository/*, tokenManager*/) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

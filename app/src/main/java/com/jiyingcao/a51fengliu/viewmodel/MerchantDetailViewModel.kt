package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.data.LoginStateManager
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.MerchantRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 联系信息显示状态
 */
data class ContactDisplayState(
    val showContact: Boolean,
    val contactText: String?,
    val promptMessage: String,
    val actionButtonText: String,
    val actionType: ContactActionType
)

enum class ContactActionType {
    LOGIN, UPGRADE_VIP, NONE
}

/**
 * 增强的UI状态 - 包含登录状态信息
 */
data class MerchantDetailUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val merchant: Merchant? = null,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val errorType: LoadingType = LoadingType.FULL_SCREEN,
    val isRefreshing: Boolean = false,
    val isOverlayLoading: Boolean = false,
    // 新增：登录状态相关
    val isLoggedIn: Boolean = false,
    val contactDisplayState: ContactDisplayState? = null
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
    private val repository: MerchantRepository = MerchantRepository.getInstance(RetrofitClient.apiService),
    private val loginStateManager: LoginStateManager = LoginStateManager.getInstance() // 依赖注入
) : BaseViewModel() {
    private var fetchJob: Job? = null

    // 单一状态源 - 商家详情页的所有UI状态
    private val _uiState = MutableStateFlow(MerchantDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var pendingInitialLoad = true

    init {
        // 观察登录状态变化，响应式更新UI
        observeLoginStateChanges()
    }

    /**
     * 观察登录状态变化并更新UI状态
     * 这是最佳实践：在ViewModel中处理跨组件状态同步
     */
    private fun observeLoginStateChanges() {
        viewModelScope.launch {
            // 组合登录状态和商家数据，生成联系信息显示状态
            combine(
                loginStateManager.isLoggedIn,
                _uiState
            ) { isLoggedIn, currentState ->
                val contactDisplayState = currentState.merchant?.let { merchant ->
                    generateContactDisplayState(merchant, isLoggedIn)
                }
                
                currentState.copy(
                    isLoggedIn = isLoggedIn,
                    contactDisplayState = contactDisplayState
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    /**
     * 根据商家信息和登录状态生成联系信息显示状态
     * 业务逻辑集中在ViewModel中，便于测试和维护
     */
    private fun generateContactDisplayState(
        merchant: Merchant,
        isLoggedIn: Boolean
    ): ContactDisplayState {
        return when {
            // 有联系方式 - 直接显示
            !merchant.contact.isNullOrBlank() -> ContactDisplayState(
                showContact = true,
                contactText = merchant.contact,
                promptMessage = "",
                actionButtonText = "",
                actionType = ContactActionType.NONE
            )
            
            // 已登录但没有联系方式 - 需要升级VIP
            isLoggedIn -> ContactDisplayState(
                showContact = false,
                contactText = null,
                promptMessage = "你需要VIP才能继续查看联系方式。", // TODO 从资源文件获取
                actionButtonText = "立即升级VIP",
                actionType = ContactActionType.UPGRADE_VIP
            )
            
            // 未登录 - 需要先登录
            else -> ContactDisplayState(
                showContact = false,
                contactText = null,
                promptMessage = "你需要登录才能继续查看联系方式。",
                actionButtonText = "立即登录",
                actionType = ContactActionType.LOGIN
            )
        }
    }

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
            updateUiStateToLoading(loadingType)

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
                updateUiStateToSuccess(merchant)
            }
            .onFailure { e ->
                if (!handleFailure(e)) { // 通用错误处理，如果处理过就不用再处理了
                    updateUiStateToError(e.toUserFriendlyMessage(), loadingType)
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
        // TODO: 登录成功后重新加载数据以获取最新权限状态
        //loadDetail(LoadingType.OVERLAY)
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
                isOverlayLoading = loadingType == LoadingType.OVERLAY,
                loadingType = loadingType,
                isError = false // 清除之前的错误状态
            )
        }
    }
    
    /**
     * 更新UI状态到成功状态
     * @param merchant 商家详情数据
     */
    private fun updateUiStateToSuccess(merchant: Merchant) {
        _uiState.update { currentState ->
            val contactDisplayState = generateContactDisplayState(merchant, currentState.isLoggedIn)
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isOverlayLoading = false,
                isError = false,
                merchant = merchant,
                contactDisplayState = contactDisplayState
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
                isOverlayLoading = false,
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
            LoadingType.OVERLAY -> currentState.isOverlayLoading
            else -> false
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

/**
 * 支持依赖注入的ViewModelFactory
 * 便于测试时注入Mock对象
 */
class MerchantDetailViewModelFactory(
    private val merchantId: String,
    private val repository: MerchantRepository = MerchantRepository.getInstance(RetrofitClient.apiService),
    private val loginStateManager: LoginStateManager = LoginStateManager.getInstance()
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MerchantDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MerchantDetailViewModel(merchantId, repository, loginStateManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
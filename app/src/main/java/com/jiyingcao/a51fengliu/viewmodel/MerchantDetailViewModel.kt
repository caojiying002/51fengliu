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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Deprecated("LoadingType")
private enum class MerchantDetailLoadingType {
    FULL_SCREEN,
    PULL_TO_REFRESH,
    FLOAT
}

@Deprecated("LoadingType")
private fun MerchantDetailLoadingType.toLoadingState(): MerchantDetailState.Loading = when (this) {
    MerchantDetailLoadingType.FULL_SCREEN -> MerchantDetailState.Loading.FullScreen
    MerchantDetailLoadingType.PULL_TO_REFRESH -> MerchantDetailState.Loading.PullToRefresh
    MerchantDetailLoadingType.FLOAT -> MerchantDetailState.Loading.Float
}

@Deprecated("LoadingType")
private fun MerchantDetailLoadingType.toErrorState(message: String): MerchantDetailState.Error = when (this) {
    MerchantDetailLoadingType.FULL_SCREEN -> MerchantDetailState.Error.FullScreen(message)
    MerchantDetailLoadingType.PULL_TO_REFRESH -> MerchantDetailState.Error.PullToRefresh(message)
    MerchantDetailLoadingType.FLOAT -> MerchantDetailState.Error.Float(message)
}

/**
 * 商家详情状态 - 采用密封类 + 接口的混合模式
 */
sealed class MerchantDetailState2 : BaseState {
    object Init : MerchantDetailState2()
    data class Loading(override val loadingType: LoadingType) : MerchantDetailState2(), LoadingState    // 实现通用加载状态接口
    data class Success(val merchant: Merchant) : MerchantDetailState2()
    data class Error(
        override val message: String,
        override val errorType: LoadingType
    ) : MerchantDetailState2(), ErrorState  // 实现通用错误状态接口
}

@Deprecated("use MerchantDetailState2")
sealed class MerchantDetailState {
    object Idle : MerchantDetailState()
    sealed class Loading : MerchantDetailState() {
        object FullScreen : Loading()
        object PullToRefresh : Loading()
        object Float : Loading()
    }
    data class Success(val merchant: Merchant) : MerchantDetailState()    
    sealed class Error(open val message: String) : MerchantDetailState() {
        data class FullScreen(override val message: String) : Error(message)
        data class PullToRefresh(override val message: String) : Error(message)
        data class Float(override val message: String) : Error(message)
    }
}

sealed class MerchantDetailIntent {
    object LoadDetail : MerchantDetailIntent()
    object PullToRefresh : MerchantDetailIntent()
    object Retry : MerchantDetailIntent()
    // 从未登录态跳转登录页面登录成功返回到本页面，本页面再次可见(onStart/onResume)时需要重新加载数据
    object HandleLoginSuccess : MerchantDetailIntent()
}

class MerchantDetailViewModel (
    private val merchantId: String,
    private val repository: RecordRepository = RecordRepository.getInstance(RetrofitClient.apiService),
    //private val tokenManager: TokenManager
) : ViewModel() {
    private var fetchJob: Job? = null

    private val _state = MutableStateFlow<MerchantDetailState2>(MerchantDetailState2.Init)
    val state = _state.asStateFlow()

    fun processIntent(intent: MerchantDetailIntent) {
        when (intent) {
            is MerchantDetailIntent.LoadDetail -> loadDetail()
            is MerchantDetailIntent.PullToRefresh -> pullToRefresh()
            is MerchantDetailIntent.Retry -> retry()
            is MerchantDetailIntent.HandleLoginSuccess -> handleLoginSuccess()
        }
    }

    private fun loadDetail(loadingType: LoadingType = LoadingType.FULL_SCREEN) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 使用通用的泛型扩展函数创建状态
            _state.value = loadingType.toLoadingState<MerchantDetailState2>()
            repository.getMerchantDetail(merchantId)
                .collect { result ->
                    result.mapCatching { requireNotNull(it) }
                        .onSuccess { merchant ->
                            //hasLoadedData = true  // TODO 登录成功也许用到
                            _state.value = MerchantDetailState2.Success(merchant)
                        }.onFailure { e ->
                            if (!handleFailure(e))  // 通用的错误处理，如果处理过就不用再处理了
                                _state.value = loadingType.toErrorState<MerchantDetailState2>(e.toUserFriendlyMessage())
                        }
                }
        }
    }

    private fun pullToRefresh() {
        loadDetail(LoadingType.PULL_TO_REFRESH)
    }

    private fun retry() {
        loadDetail(LoadingType.FULL_SCREEN)
    }

    private fun handleLoginSuccess() {
        // TODO 实现登录成功后的处理逻辑
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

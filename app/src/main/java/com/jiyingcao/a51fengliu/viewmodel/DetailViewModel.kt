package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.data.LoginStateManager
import com.jiyingcao.a51fengliu.data.LoginEvent
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 单一UI状态 - 详情页所有状态信息
 */
data class DetailUiState(
    val isLoading: Boolean = false,
    val loadingType: LoadingType = LoadingType.FULL_SCREEN,
    val record: RecordInfo? = null,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val errorType: LoadingType = LoadingType.FULL_SCREEN,
    val isRefreshing: Boolean = false,
    val isOverlayLoading: Boolean = false,
    val favoriteProgress: FavoriteProgress = FavoriteProgress.None
) {
    // 派生状态 - 通过计算得出，避免状态冗余
    val showFullScreenLoading: Boolean get() = isLoading && loadingType == LoadingType.FULL_SCREEN
    val showOverlayLoading: Boolean get() = isOverlayLoading
    val showContent: Boolean get() = !isLoading && !isError && record != null
    val showFullScreenError: Boolean get() = isError && errorType == LoadingType.FULL_SCREEN
    val hasData: Boolean get() = record != null
    val isFavorited: Boolean get() = record?.isFavorite == true
}

/**
 * 收藏操作进度 - 用于表示当前有没有进行中的收藏/取消收藏请求
 */
sealed class FavoriteProgress {
    /** 无操作 */
    object None : FavoriteProgress()
    /** 收藏中 */
    object Favoriting : FavoriteProgress()
    /** 取消收藏中 */
    object Unfavoriting : FavoriteProgress()
}

sealed class DetailIntent {
    object InitialLoad : DetailIntent()
    object PullToRefresh : DetailIntent()
    object Retry : DetailIntent()
    object ToggleFavorite : DetailIntent()
}

/**
 * 副作用 - 保持独立，用于处理一次性事件
 */
sealed class DetailEffect {
    object ShowLoadingDialog : DetailEffect()
    object DismissLoadingDialog : DetailEffect()
    data class ShowToast(val message: String) : DetailEffect()
}

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted private val infoId: String,
    private val repository: RecordRepository,
    private val loginStateManager: LoginStateManager
) : BaseViewModel() {

    // 单一状态源 - 详情页的所有UI状态
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // 副作用通道
    private val _effect = Channel<DetailEffect>()
    val effect = _effect.receiveAsFlow()

    // 内部状态管理
    @Volatile private var isUIVisible: Boolean = false
    @Volatile private var needsRefresh: Boolean = false
    @Volatile private var pendingInitialLoad: Boolean = true

    // Job tracking
    private var detailLoadJob: Job? = null
    private var favoriteToggleJob: Job? = null

    init {
        // 监听登录状态变化事件
        loginStateManager.loginEvents
            .onEach { event ->
                when (event) {
                    LoginEvent.LoggedIn, LoginEvent.LoggedOut -> {
                        needsRefresh = true
                        checkAndRefresh()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun checkAndRefresh() {
        if (needsRefresh &&
            isUIVisible &&
            _uiState.value.hasData  // 第三个条件：确保之前已经加载过数据
        ) {
            needsRefresh = false
            refreshOnLoginStateChange()
        }
    }

    /**
     * # `setUIVisibility`方法与MVI模式的关系
     *
     * ## 严格的MVI模式视角
     *
     * 从严格的MVI模式来看，`setUIVisibility`方法确实偏离了标准实现：
     *
     * - **不符合单一数据流** - 直接调用方法修改ViewModel内部状态，而非通过Intent
     * - **混合了命令式编程** - 方法直接触发了行为，不只是传递意图
     * - **打破了关注点分离** - View层直接告知ViewModel其UI可见性
     *
     * ## 实用主义视角
     *
     * 然而，从实用主义角度看，这种设计有其合理性：
     *
     * - **生命周期感知** - 可见性通常来自生命周期事件，不完全是"用户意图"
     * - **性能优化** - 可避免在UI不可见时进行不必要的刷新
     * - **常见模式** - 在实际开发中，这种模式很常见且有效
     *
     * ## 实践中的选择
     *
     * 在实际开发中，通常会根据团队约定和项目需求做出平衡：
     *
     * 1. **严格MVI团队** - 应该将所有状态变更（包括可见性）转换为Intent
     * 2. **混合MVI团队** - 可以为"用户行为"使用Intent，而对生命周期相关事件使用直接方法
     * 3. **结果导向团队** - 可以简化API，只要内部保持一致的状态管理
     *
     * 关键是保持团队内的一致性，以及明确文档中说明哪些操作遵循严格MVI，哪些是为了实用性的例外。
     */

    /**
     * UI可见性管理 - 实用主义方法
     * 虽然不是严格的MVI，但在生命周期管理方面很实用
     */
    fun setUIVisibility(isVisible: Boolean) {
        isUIVisible = isVisible
        if (isVisible) {
            checkAndRefresh()
        }
    }

    fun processIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.InitialLoad -> initialLoad()
            DetailIntent.PullToRefresh -> pullToRefresh()
            DetailIntent.Retry -> retry()
            DetailIntent.ToggleFavorite -> toggleFavorite()
        }
    }

    private fun initialLoad() {
        // 避免UI发生配置更改时ViewModel重新加载数据
        if (pendingInitialLoad) {
            loadDetail(LoadingType.FULL_SCREEN)
            pendingInitialLoad = false
        }
    }

    private fun loadDetail(loadingType: LoadingType = LoadingType.FULL_SCREEN) {
        if (shouldPreventRequest(loadingType)) return

        detailLoadJob?.cancel()
        detailLoadJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 更新加载状态
            updateUiStateToLoading(loadingType)

            repository.getDetail(infoId)
                .collect { result ->
                    handleDataResult(result, loadingType)
                }
        }
    }

    private suspend fun handleDataResult(
        result: Result<RecordInfo?>,
        loadingType: LoadingType
    ) {
        result.mapCatching { requireNotNull(it) }
            .onSuccess { record ->
                updateUiStateToSuccess(record)
            }
            .onFailure { e ->
                if (!handleFailure(e)) { // 通用的错误处理
                    updateUiStateToError(e.toUserFriendlyMessage(), loadingType)
                }
                AppLogger.w(TAG, e)
            }
    }

    private fun pullToRefresh() {
        loadDetail(LoadingType.PULL_TO_REFRESH)
    }

    private fun refreshOnLoginStateChange() {
        loadDetail(LoadingType.FULL_SCREEN)
    }

    /**
     * 对于请求失败的场景，专门定义 DetailIntent.Retry 更合适：
     * - 语义更明确，表达了用户重试的意图
     * - 错误恢复是独立的业务场景，可能需要特殊处理（如清除错误状态）
     * - 方便后续添加重试相关的特殊逻辑（如重试次数限制）
     */
    private fun retry() {
        loadDetail(LoadingType.FULL_SCREEN)
    }

    private fun toggleFavorite() {
        val currentState = _uiState.value
        val record = currentState.record ?: return

        // 如果有进行中的收藏或者取消收藏请求，忽略本次点击
        if (currentState.favoriteProgress != FavoriteProgress.None) return

        val wasFavorited = record.isFavorite == true
        val progress = if (wasFavorited) FavoriteProgress.Unfavoriting else FavoriteProgress.Favoriting

        favoriteToggleJob?.cancel()
        favoriteToggleJob = viewModelScope.launch(remoteLoginCoroutineContext) {
            // 立即更新UI状态为收藏操作进行中，不等待网络请求
            _uiState.update { it.copy(favoriteProgress = progress) }

            val toggleFavoriteFlow =
                if (wasFavorited) repository.unfavorite(infoId) else repository.favorite(infoId)
            toggleFavoriteFlow.collect { result ->
                result.onSuccess {
                    // 网络请求成功，更新record中的收藏/未收藏状态，清除进行中状态
                    val updatedRecord = record.copy(isFavorite = !wasFavorited)
                    _uiState.update { currentState ->
                        currentState.copy(
                            record = updatedRecord,
                            favoriteProgress = FavoriteProgress.None
                        )
                    }
                    val message = if (wasFavorited) "取消收藏成功" else "收藏成功"
                    _effect.send(DetailEffect.ShowToast(message))
                }.onFailure { e ->
                    // 网络请求失败，恢复原来的收藏/未收藏状态
                    _uiState.update { it.copy(favoriteProgress = FavoriteProgress.None) }
                    if (!handleFailure(e)) {
                        _effect.send(DetailEffect.ShowToast(e.toUserFriendlyMessage()))
                    }
                    AppLogger.w(TAG, e)
                }
            }
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
                isOverlayLoading = loadingType == LoadingType.OVERLAY,
                loadingType = loadingType,
                isError = false // 清除之前的错误状态
            )
        }
    }
    
    /**
     * 更新UI状态到成功状态
     * @param record 详情记录数据
     */
    private fun updateUiStateToSuccess(record: RecordInfo) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isOverlayLoading = false,
                isError = false,
                record = record
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
        detailLoadJob?.cancel()
        favoriteToggleJob?.cancel()
    }

    @AssistedFactory
    interface Factory {
        fun create(infoId: String): DetailViewModel
    }

    companion object {
        private const val TAG = "DetailViewModel"
    }
}
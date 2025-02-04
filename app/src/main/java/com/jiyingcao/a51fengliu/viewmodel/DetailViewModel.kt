package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.domain.exception.RemoteLoginException
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class DetailState {
    object Init : DetailState()
    data class Loading(val isFloatLoading: Boolean = false) : DetailState()
    data class Success(val record: RecordInfo) : DetailState()
    data class Error(val message: String) : DetailState()
}

sealed class DetailIntent {
    object LoadDetail : DetailIntent()
    object Retry : DetailIntent()
    object Refresh : DetailIntent()
    object Favorite : DetailIntent()
    object Unfavorite : DetailIntent()
}

sealed class DetailEffect {
    object ShowLoadingDialog : DetailEffect()
    object DismissLoadingDialog : DetailEffect()
    data class ShowToast(val message: String) : DetailEffect()
}

class DetailViewModel(
    private val infoId: String,
    private val repository: RecordRepository,
    private val tokenManager: TokenManager
) : BaseViewModel() {

    private val _state = MutableStateFlow<DetailState>(DetailState.Init)
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    //val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    // 表示是否已经收藏的流
    private val _isFavorited: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val isFavorited: StateFlow<Boolean?> = _isFavorited.asStateFlow()

    private val _isUIVisible = MutableStateFlow(false)

    /** 从未登录状态转变为已登录时，标记为true */
    private val _needsRefresh = MutableStateFlow(false)

    private var hasLoadedData = false

    private val _effect = Channel<DetailEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            _state.collect { detailState ->
                when (detailState) {
                    is DetailState.Success -> {
                        _isFavorited.value = detailState.record.isFavorite
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            tokenManager.token
                .map { token -> !token.isNullOrBlank() }
                .distinctUntilChanged()
                .collect { isLoggedIn ->
                    val wasLoggedIn = _isLoggedIn.value
                    _isLoggedIn.value = isLoggedIn
                    // 标记需要刷新的条件：从未登录状态变为已登录状态
                    if (isLoggedIn && wasLoggedIn == false) {
                        _needsRefresh.value = true
                        checkAndRefresh()
                    }
                }
        }
    }

    private fun checkAndRefresh() {
        if (_needsRefresh.value &&
            _isUIVisible.value &&
            hasLoadedData  // 第三个条件：确保之前已经加载过数据
        ) {
            _needsRefresh.value = false
            processIntent(DetailIntent.Refresh)
        }
    }

    fun setUIVisibility(isVisible: Boolean) {
        _isUIVisible.value = isVisible
        if (isVisible) {
            checkAndRefresh()
        }
    }

    fun processIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.LoadDetail -> loadDetail()
            is DetailIntent.Retry -> loadDetail()
            is DetailIntent.Refresh -> refresh()
            is DetailIntent.Favorite -> favorite()
            is DetailIntent.Unfavorite -> unfavorite()
        }
    }

    private fun loadDetail(isFloatLoading: Boolean = false) {
        viewModelScope.launch(RemoteLoginManager.networkScope.coroutineContext) {
            _state.value = DetailState.Loading(isFloatLoading)
            repository.getDetail(infoId)
                .collect { result ->
                    result.onSuccess { record ->
                        hasLoadedData = true
                        _state.value = DetailState.Success(record)
                    }.onFailure { e ->
                        handleFailure(e)  // 使用基类的统一错误处理
                        _state.value = DetailState.Error(e.toUserFriendlyMessage())
                    }
                }
        }
    }

    private fun refresh() {
        loadDetail(isFloatLoading = true)
    }

    private fun favorite() {
        viewModelScope.launch {
            repository.favorite(infoId)
                .collect { result ->
                    result.onSuccess {
                        _isFavorited.value = true
                        _effect.send(DetailEffect.ShowToast("收藏成功"))
                    }.onFailure { e ->
                        _effect.send(DetailEffect.ShowToast(e.toUserFriendlyMessage()))
                    }
                }
        }
    }
    private fun unfavorite() {
        viewModelScope.launch {
            repository.unfavorite(infoId)
                .collect { result ->
                    result.onSuccess {
                        _isFavorited.value = false
                        _effect.send(DetailEffect.ShowToast("取消收藏成功"))
                    }.onFailure { e ->
                        _effect.send(DetailEffect.ShowToast(e.toUserFriendlyMessage()))
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        hasLoadedData = false
    }
}

class DetailViewModelFactory(
    private val infoId: String,
    private val repository: RecordRepository,
    private val tokenManager: TokenManager
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(infoId, repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.Profile
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Init : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: Profile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class ProfileIntent {
    object LoadProfile : ProfileIntent()
    object Logout : ProfileIntent()
}

sealed class LogoutEffect {
    object ShowLoadingDialog : LogoutEffect()
    object DismissLoadingDialog : LogoutEffect()
    data class ShowToast(val message: String) : LogoutEffect()
}

class ProfileViewModel(
    private val repository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Init)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    private val _isUIVisible = MutableStateFlow(false)

    /** 从未登录状态转变为已登录时，标记为true */
    private val _needsProfileRefresh = MutableStateFlow(false)

    private val _effect = Channel<LogoutEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            tokenManager.token
                .map { token -> !token.isNullOrBlank() }
                .distinctUntilChanged()
                .collect { isLoggedIn ->
                    _isLoggedIn.value = isLoggedIn
                    // 标记需要刷新的条件：从未登录状态变为已登录状态
                    if (isLoggedIn) {
                        _needsProfileRefresh.value = true
                        checkAndLoadProfile()
                    }
                }
        }
    }

    private fun checkAndLoadProfile() {
        if (_needsProfileRefresh.value && _isUIVisible.value) {
            _needsProfileRefresh.value = false
            processIntent(ProfileIntent.LoadProfile)
        }
    }

    fun setUIVisibility(isVisible: Boolean) {
        _isUIVisible.value = isVisible
        if (isVisible) {
            checkAndLoadProfile()
        }
    }

    fun processIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> fetchProfile()
            is ProfileIntent.Logout -> logout()
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            repository.getProfile().collect { result ->
                result.onSuccess { profile ->
                    _state.value = ProfileState.Success(profile)
                }.onFailure { e ->
                    _state.value = ProfileState.Error(e.toUserFriendlyMessage())
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _effect.send(LogoutEffect.ShowLoadingDialog)
            repository.logout().collect { result ->
                result.onSuccess {
                    tokenManager.clearToken()
                    //_isLoggedIn.value = false // tokenManager.token流会自动触发_isLoggedIn流的赋值，这里不需要再手动设置

                    _effect.send(LogoutEffect.DismissLoadingDialog)
                    _effect.send(LogoutEffect.ShowToast("已退出登录"))
                }.onFailure { e ->
                    _effect.send(LogoutEffect.DismissLoadingDialog)
                    _effect.send(LogoutEffect.ShowToast(e.toUserFriendlyMessage()))
                }
            }
        }
    }
}

class ProfileViewModelFactory(
    private val repository: UserRepository,
    private val tokenManager: TokenManager
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
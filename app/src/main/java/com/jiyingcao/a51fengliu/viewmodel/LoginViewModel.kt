package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.LoginErrorData
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.domain.exception.LoginException
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// UI 错误类型
sealed class LoginErrorType {
    data class NamePassword(val name: String?, val password: String?) : LoginErrorType()
    data class UnknownError(val message: String) : LoginErrorType()
}

// 登录状态
sealed class LoginState {
    object Init : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    data class Error(
        val errorType: LoginErrorType,
        val code: Int,
    ) : LoginState()
}

// 用户意图
sealed class LoginIntent {
    data class Login(
        val username: String,
        val password: String
    ) : LoginIntent()

    object ClearError : LoginIntent()
}

// 副作用
sealed class LoginEffect {
    data class ShowToast(val message: String) : LoginEffect()
    object NavigateToMain : LoginEffect()   // TODO 改成更合适的语义命名
    object RequestNotificationPermission : LoginEffect()
}

class LoginViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Init)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effect = Channel<LoginEffect>()
    val effect = _effect.receiveAsFlow()

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.Login -> login(intent.username, intent.password)
            is LoginIntent.ClearError -> clearError()
        }
    }

    private fun login(username: String, password: String) {
        viewModelScope.launch { // TODO context
            _state.value = LoginState.Loading
            repository.login(username, password)
                .collect { result ->
                    result.onSuccess { token ->
                        TokenManager.getInstance().saveToken(token)
                        _state.value = LoginState.Success(token)
                        _effect.send(LoginEffect.RequestNotificationPermission)
                        _effect.send(LoginEffect.NavigateToMain)
                    }.onFailure { e ->
                        when (e) {
                            is LoginException -> {
                                // 处理特定的登录错误
                                val errorType = when (e.errorData) {
                                    is LoginErrorData -> {
                                        LoginErrorType.NamePassword(
                                            name = e.errorData.name,
                                            password = e.errorData.password
                                        )
                                    }
                                    else -> {
                                        LoginErrorType.UnknownError(e.toUserFriendlyMessage())
                                    }
                                }
                                _state.value = LoginState.Error(errorType, e.code)
                            }
                            else -> {
                                // 处理其他错误
                                _state.value = LoginState.Error(
                                    errorType = LoginErrorType.UnknownError(e.toUserFriendlyMessage()),
                                    code = -999,
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun clearError() {
        _state.value = LoginState.Init
    }

    class Factory(
        private val repository: UserRepository = UserRepository.getInstance()
    ): ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
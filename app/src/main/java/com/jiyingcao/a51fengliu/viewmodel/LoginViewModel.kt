package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import com.jiyingcao.a51fengliu.util.getErrorMessage
import com.jiyingcao.a51fengliu.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: UserRepository,
    private val tokenManager: TokenManager
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
                    when (result) {
                        is ApiResult.Success -> {
                            // 登录成功
                            val token = result.data
                            tokenManager.saveToken(token)
                            _state.value = LoginState.Success(token)
                            _effect.send(LoginEffect.NavigateToMain)
                        }
                        is ApiResult.ApiError -> {
                            // 检查是否为字段验证错误 (code=0 且 data 是 Map)
                            val errorType = if (result.code == 0 && result.data is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                val fieldErrors = result.data as Map<String, String>
                                val nameError = fieldErrors["name"]
                                val passwordError = fieldErrors["password"]
                                LoginErrorType.NamePassword(nameError, passwordError)
                            } else {
                                // 通用业务错误
                                LoginErrorType.UnknownError(result.message)
                            }
                            _state.value = LoginState.Error(errorType, result.code)
                        }
                        is ApiResult.NetworkError -> {
                            // 网络错误
                            _state.value = LoginState.Error(
                                errorType = LoginErrorType.UnknownError(result.getErrorMessage("网络连接失败")),
                                code = -999,
                            )
                        }
                        is ApiResult.UnknownError -> {
                            // 未知错误
                            _state.value = LoginState.Error(
                                errorType = LoginErrorType.UnknownError(result.getErrorMessage("未知错误")),
                                code = -999,
                            )
                        }
                    }
                }
        }
    }

    private fun clearError() {
        _state.value = LoginState.Init
    }
}
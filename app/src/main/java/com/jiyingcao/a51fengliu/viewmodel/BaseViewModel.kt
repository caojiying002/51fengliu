package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.domain.exception.RemoteLoginException
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    protected fun handleFailure(e: Throwable) {
        if (e is RemoteLoginException) {
            viewModelScope.launch {
                RemoteLoginManager.handleRemoteLogin()
            }
            return
        }
        // 处理其他错误...
    }
}
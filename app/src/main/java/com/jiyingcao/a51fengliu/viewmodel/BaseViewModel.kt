package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.domain.exception.RemoteLoginException

abstract class BaseViewModel : ViewModel() {
    /**
     * 组合代替继承！！！！
     * 组合代替继承！！！！
     * 组合代替继承！！！！
     *
     * 除非万不得已，不要在Base类里写逻辑，考虑用其他的方式（e.g. 扩展函数、Kotlin委托）
     */
}

/**
 * 处理一些通用的错误。目前只处理了异地登录[RemoteLoginException]，其他错误需要在ViewModel中具体处理。
 * Called inside a ViewModel's coroutine scope
 * @param e the error to handle
 * @return true if the error is handled, false otherwise
 */
suspend fun ViewModel.handleFailure(e: Throwable): Boolean {
    return when (e) {
        is RemoteLoginException -> {
            RemoteLoginManager.handleRemoteLogin()
            true
        }
        else -> false
    }
}
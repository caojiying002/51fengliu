package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import com.jiyingcao.a51fengliu.domain.exception.RemoteLoginException
import com.jiyingcao.a51fengliu.domain.model.ApiResult

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
        is CancellationException -> throw e
        is RemoteLoginException -> {
            RemoteLoginManager.handleRemoteLogin()
            true
        }
        else -> false
    }
}

/**
 * 处理ApiResult中的通用错误
 *
 * 目前处理：
 * - 远程登录错误（code=1003）
 *
 * @param result API调用结果
 * @return true表示错误已处理，false表示需要业务层继续处理
 */
suspend fun <T> ViewModel.handleApiResultFailure(result: ApiResult<T>): Boolean {
    return when (result) {
        is ApiResult.Success -> false
        is ApiResult.ApiError -> {
            // 检查是否为远程登录错误
            if (result.code == ApiException.CODE_REMOTE_LOGIN) {
                RemoteLoginManager.handleRemoteLogin()
                true
            } else {
                false
            }
        }
        is ApiResult.NetworkError,
        is ApiResult.UnknownError -> false
    }
}
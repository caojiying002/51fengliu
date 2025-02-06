package com.jiyingcao.a51fengliu.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/** 异地登录事件管理器 */
object RemoteLoginManager {
    private val _remoteLoginEvent = MutableSharedFlow<Unit>()
    val remoteLoginEvent = _remoteLoginEvent.asSharedFlow()

    private val _isHandlingRemoteLogin = AtomicBoolean(false)
    private val _networkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * ViewModel.launch 需要使用这个 CoroutineContext，
     * 发生异地登录时，才能取消所有同时进行的网络请求
     */
    val remoteLoginCoroutineContext: CoroutineContext get() = _networkScope.coroutineContext

    suspend fun handleRemoteLogin() {
        if (_isHandlingRemoteLogin.compareAndSet(false, true)) {
            _remoteLoginEvent.emit(Unit)
            _networkScope.coroutineContext[Job]?.cancelChildren()
        }
    }

    fun reset() {
        _isHandlingRemoteLogin.set(false)
    }
}
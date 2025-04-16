package com.jiyingcao.a51fengliu.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jiyingcao.a51fengliu.data.LoginEvent
import com.jiyingcao.a51fengliu.data.LoginStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 为LifecycleOwner添加登录状态监听
 */
fun LifecycleOwner.observeLoginState(
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (Boolean) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(state) {
            LoginStateManager.getInstance().isLoggedIn.collect { isLoggedIn ->
                action(isLoggedIn)
            }
        }
    }
}

/**
 * 为LifecycleOwner添加登录事件监听
 */
fun LifecycleOwner.observeLoginEvents(
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (LoginEvent) -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(state) {
            LoginStateManager.getInstance().loginEvents.collect { event ->
                action(event)
            }
        }
    }
}

/**
 * 为非UI组件提供监听登录状态的方法
 */
fun CoroutineScope.observeLoginState(action: suspend (Boolean) -> Unit) {
    launch {
        LoginStateManager.getInstance().isLoggedIn.collect { isLoggedIn ->
            action(isLoggedIn)
        }
    }
}

/**
 * 为非UI组件提供监听登录事件的方法
 */
fun CoroutineScope.observeLoginEvents(action: suspend (LoginEvent) -> Unit) {
    launch {
        LoginStateManager.getInstance().loginEvents.collect { event ->
            action(event)
        }
    }
}
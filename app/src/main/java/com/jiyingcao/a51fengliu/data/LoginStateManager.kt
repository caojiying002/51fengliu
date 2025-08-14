package com.jiyingcao.a51fengliu.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局登录状态变化事件
 */
sealed class LoginEvent {
    /**
     * 用户登录事件
     */
    object LoggedIn : LoginEvent()
    
    /**
     * 用户退出登录事件
     */
    object LoggedOut : LoginEvent()
}

/**
 * 登录状态管理器
 * 负责提供统一的登录状态监听接口
 */
@Singleton
class LoginStateManager @Inject constructor(private val tokenManager: TokenManager) {

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 登录状态管理使用了双流设计模式:
     * 
     * 1. 状态流(StateFlow): isLoggedIn
     *    - 表示持续性的登录状态(true/false)
     *    - 任何时刻都有一个确定值
     *    - 新订阅者会立即获得当前登录状态
     *    - 适合用于控制UI显示状态(如显示登录界面或已登录界面)
     *    - 使用distinctUntilChanged()避免重复发送相同状态
     *    - API语义：回答"用户现在是否已登录？"
     * 
     * 2. 事件流(SharedFlow): loginEvents
     *    - 表示离散性的登录/登出事件
     *    - 仅在状态实际变化时发送事件
     *    - 新订阅者只会收到订阅后发生的事件
     *    - 适合执行一次性操作(如显示登录成功提示、同步数据、清理缓存等)
     *    - API语义：回答"用户刚刚登录或登出了吗？"
     * 
     * 这种"状态下沉，事件上浮"的设计模式允许组件根据不同需求:
     * - 使用observeLoginState()获取并响应当前登录状态
     * - 使用observeLoginEvents()响应登录状态变化事件
     */

    /**
     * 登录状态流
     * true: 已登录
     * false: 未登录
     */
    val isLoggedIn: StateFlow<Boolean> = runBlocking {
        // 用正确的值初始化isLoggedIn流
        val initialValue = !tokenManager.getToken().isNullOrBlank()
        
        tokenManager.token
            .map { !it.isNullOrBlank() }
            .distinctUntilChanged()
            .stateIn(
                scope = managerScope,
                started = SharingStarted.Eagerly,
                initialValue = initialValue
            )
    }

    /**
     * 登录事件流 - 可用于接收登录/登出事件
     */
    private val _loginEvents = MutableSharedFlow<LoginEvent>()
    val loginEvents: SharedFlow<LoginEvent> = _loginEvents.asSharedFlow()

    init {
        // 监听登录状态变化并发送相应事件
        managerScope.launch {
            var previous: Boolean? = null
            isLoggedIn.collect { current ->
                previous?.let { it
                    if (current && !it) {
                        _loginEvents.emit(LoginEvent.LoggedIn)
                    } else if (!current && it) {
                        _loginEvents.emit(LoginEvent.LoggedOut)
                    }
                }
                previous = current
            }
        }
    }

}
package com.jiyingcao.a51fengliu.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jiyingcao.a51fengliu.data.LoginStateManager
import javax.inject.Inject
import com.jiyingcao.a51fengliu.ui.auth.AuthActivity

/**
 * 登录拦截器
 * 处理需要登录才能访问的功能
 * 
 * 企业应用实践：使用Activity Result API处理登录流程，确保导航意图不丢失
 */
class LoginInterceptor @Inject constructor(
    private val loginStateManager: LoginStateManager
) {

    private var loginLauncher: ActivityResultLauncher<LoginRequestData>? = null
    private var pendingAction: (() -> Unit)? = null

    /**
     * 为Fragment注册登录拦截器
     */
    fun register(fragment: Fragment) {
        loginLauncher = fragment.registerForActivityResult(LoginContract()) { result ->
            if (result) {
                // 登录成功，执行之前的操作
                pendingAction?.invoke()
            }
            // 清空待执行操作
            pendingAction = null
        }

        // 监听生命周期，防止内存泄漏
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                pendingAction = null
            }
        })
    }

    /**
     * 为Activity注册登录拦截器
     */
    fun register(activity: FragmentActivity) {
        loginLauncher = activity.registerForActivityResult(LoginContract()) { result ->
            if (result) {
                // 登录成功，执行之前的操作
                pendingAction?.invoke()
            }
            // 清空待执行操作
            pendingAction = null
        }

        // 监听生命周期，防止内存泄漏
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                pendingAction = null
            }
        })
    }

    /**
     * 执行需要登录拦截的操作
     *
     * @param action 需要执行的操作
     * @param extras 可选的传递给登录页面的参数
     */
    fun execute(action: () -> Unit) {
        // 同步检查登录状态
        val isLoggedIn = loginStateManager.isLoggedIn.value

        if (isLoggedIn) {
            // 已登录，直接执行
            action()
        } else {
            // 未登录，保存操作并跳转登录页
            pendingAction = action
            loginLauncher?.launch(LoginRequestData())
        }
    }

    /**
     * 登录请求数据
     */
    data class LoginRequestData(val extras: Bundle? = null)

    /**
     * 登录页面启动契约
     */
    private class LoginContract : ActivityResultContract<LoginRequestData, Boolean>() {

        override fun createIntent(context: Context, input: LoginRequestData): Intent =
            AuthActivity.createIntent(context).apply {
                input.extras?.let { putExtras(it) }
                // 添加标记，表示这是拦截后的登录
                putExtra(AuthActivity.EXTRA_IS_INTERCEPTED, true)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == Activity.RESULT_OK
        }
    }

}
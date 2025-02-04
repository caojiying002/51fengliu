package com.jiyingcao.a51fengliu

import android.app.Application
import android.content.Intent
import com.jiyingcao.a51fengliu.ActivityManager.activityLifecycleCallbacks
import com.jiyingcao.a51fengliu.ActivityManager.getCurrentActivity
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.ui.LoginActivity
import com.jiyingcao.a51fengliu.ui.dialog.RemoteLoginDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class App: Application() {

    /** 为应用全局事件处理创建专门的作用域 */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        registerActivityLifecycleCallbacks(EdgeToEdgeWindowInsetsCallbacks)
        initRemoteLoginHandler()
    }

    private fun initRemoteLoginHandler() {
        applicationScope.launch {
            RemoteLoginManager.remoteLoginEvent
                .collect {
                    // 使用 Context.startActivity 需要添加 FLAG_ACTIVITY_NEW_TASK
                    val intent = Intent(this@App, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

                    // 使用 Activity Result API 的对话框
                    getCurrentActivity()?.let {
                        RemoteLoginDialog().show(it.supportFragmentManager, "remote_login")
                    }
                }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel() // 清理资源
    }

    companion object {
        @JvmStatic lateinit var INSTANCE: App
    }
}
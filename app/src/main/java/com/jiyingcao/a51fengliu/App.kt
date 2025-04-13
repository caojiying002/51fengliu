package com.jiyingcao.a51fengliu

import android.app.Application
import com.jiyingcao.a51fengliu.ActivityManager.activityLifecycleCallbacks
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.ui.common.RemoteLoginActivity
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
                    RemoteLoginActivity.start(applicationContext)
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
package com.jiyingcao.a51fengliu

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.jiyingcao.a51fengliu.ActivityManager.activityLifecycleCallbacks
import com.jiyingcao.a51fengliu.coil.CoilConfig
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.ui.common.RemoteLoginActivity
import com.jiyingcao.a51fengliu.util.NotificationManagerHelper
import com.jiyingcao.a51fengliu.util.ProcessUtil
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@HiltAndroidApp
class App: Application() {

    /** 为应用全局事件处理创建专门的作用域，只在主进程创建 */
    private var applicationScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // 仅在主进程执行UI相关初始化
        if (ProcessUtil.isMainProcess(this)) {
            initApplicationScope()
            initCoil()
            initNotificationChannels()
            registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            registerActivityLifecycleCallbacks(EdgeToEdgeWindowInsetsCallbacks)
            initRemoteLoginHandler()
        }
    }

    private fun initApplicationScope() {
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    private fun initCoil() {
        SingletonImageLoader.setSafe { CoilConfig.createImageLoader(this) }
    }

    private fun initNotificationChannels() {
        NotificationManagerHelper.createNotificationChannels(this)
    }

    private fun initRemoteLoginHandler() {
        applicationScope?.launch {
            RemoteLoginManager.remoteLoginEvent
                .collect {
                    NotificationManagerHelper.sendRemoteLoginNotification(applicationContext)
                    RemoteLoginActivity.start(applicationContext)
                }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope?.cancel()
    }

    companion object {
        @JvmStatic lateinit var INSTANCE: App
    }
}
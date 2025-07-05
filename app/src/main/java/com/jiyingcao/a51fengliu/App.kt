package com.jiyingcao.a51fengliu

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.jiyingcao.a51fengliu.ActivityManager.activityLifecycleCallbacks
import com.jiyingcao.a51fengliu.coil.CoilConfig
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.ui.common.RemoteLoginActivity
import com.jiyingcao.a51fengliu.util.NotificationManagerHelper
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@HiltAndroidApp
class App: Application() {

    /** 为应用全局事件处理创建专门的作用域 */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        AppConfig.init(this)
        initCoil()
        initNotificationChannels()
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        registerActivityLifecycleCallbacks(EdgeToEdgeWindowInsetsCallbacks)
        initRemoteLoginHandler()
    }

    private fun initCoil() {
        SingletonImageLoader.setSafe { CoilConfig.createImageLoader(this) }
    }

    private fun initNotificationChannels() {
        NotificationManagerHelper.createNotificationChannels(this)
    }
    
    private fun initRemoteLoginHandler() {
        applicationScope.launch {
            RemoteLoginManager.remoteLoginEvent
                .collect {
                    NotificationManagerHelper.sendRemoteLoginNotification(applicationContext)
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
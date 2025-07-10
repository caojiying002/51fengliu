package com.jiyingcao.a51fengliu.startup

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.ui.common.RemoteLoginActivity
import com.jiyingcao.a51fengliu.util.NotificationManagerHelper
import com.jiyingcao.a51fengliu.util.ProcessUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RemoteLoginHandlerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (ProcessUtil.isMainProcess(context)) {
            // 获取ApplicationScope并启动监听
            val appScope = AppInitializer.getInstance(context)
                .initializeComponent(ApplicationScopeInitializer::class.java)
            
            appScope.launch {
                RemoteLoginManager.remoteLoginEvent
                    .collect {
                        NotificationManagerHelper.sendRemoteLoginNotification(context.applicationContext)
                        RemoteLoginActivity.start(context.applicationContext)
                    }
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(
            AppConfigInitializer::class.java,
            ApplicationScopeInitializer::class.java
        )
    }
}
package com.jiyingcao.a51fengliu.startup

import android.content.Context
import androidx.startup.Initializer
import com.jiyingcao.a51fengliu.util.NotificationManagerHelper
import com.jiyingcao.a51fengliu.util.ProcessUtil

class NotificationChannelsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (ProcessUtil.isMainProcess(context)) {
            NotificationManagerHelper.createNotificationChannels(context)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(AppConfigInitializer::class.java)
    }
}
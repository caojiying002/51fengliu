package com.jiyingcao.a51fengliu.startup

import android.content.Context
import androidx.startup.Initializer
import com.jiyingcao.a51fengliu.config.AppConfig

class AppConfigInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        AppConfig.init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return emptyList()
    }
}
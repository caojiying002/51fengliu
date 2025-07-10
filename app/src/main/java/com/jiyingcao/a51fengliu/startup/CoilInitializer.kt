package com.jiyingcao.a51fengliu.startup

import android.content.Context
import androidx.startup.Initializer
import com.jiyingcao.a51fengliu.coil.CoilConfig
import com.jiyingcao.a51fengliu.util.ProcessUtil
import coil3.SingletonImageLoader

class CoilInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (ProcessUtil.isMainProcess(context)) {
            SingletonImageLoader.setSafe { CoilConfig.createImageLoader(context) }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(AppConfigInitializer::class.java)
    }
}
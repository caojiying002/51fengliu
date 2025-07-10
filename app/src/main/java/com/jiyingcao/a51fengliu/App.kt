package com.jiyingcao.a51fengliu

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.jiyingcao.a51fengliu.ActivityManager.activityLifecycleCallbacks
import com.jiyingcao.a51fengliu.util.ProcessUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

@HiltAndroidApp
class App: Application() {

    /** 为应用全局事件处理创建专门的作用域，只在主进程创建 */
    private var applicationScope: CoroutineScope? = null

    fun getApplicationScope(): CoroutineScope? = applicationScope
    
    fun setApplicationScope(scope: CoroutineScope?) {
        applicationScope = scope
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // Android Startup 库会自动处理以下初始化：
        // - AppConfig.init(this)
        // - ApplicationScope 创建
        // - Coil 初始化
        // - 通知渠道创建
        // - 远程登录事件处理

        // 仅在主进程执行Activity生命周期回调注册（不适合用Startup库）
        if (ProcessUtil.isMainProcess(this)) {
            registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            registerActivityLifecycleCallbacks(EdgeToEdgeWindowInsetsCallbacks)
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
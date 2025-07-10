package com.jiyingcao.a51fengliu.startup

import android.content.Context
import androidx.startup.Initializer
import com.jiyingcao.a51fengliu.util.ProcessUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ApplicationScopeInitializer : Initializer<CoroutineScope> {
    override fun create(context: Context): CoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        
        // 在主进程中将scope设置到App实例中
        if (ProcessUtil.isMainProcess(context)) {
            // 由于这是在启动时调用，App.INSTANCE 还没有被设置
            // 所以我们直接返回scope，让其他initializer获取
        }
        
        return scope
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(AppConfigInitializer::class.java)
    }
}
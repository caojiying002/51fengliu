package com.jiyingcao.a51fengliu.config

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * 环境配置提供者
 * 负责确定当前应用的运行环境和配置
 */
@Deprecated(
    message = "Use generated BuildConfig.IS_DEBUG from appropriate sourceSet instead.\n" +
        "BuildEnvironment will be removed in a future release.",
    replaceWith = ReplaceWith("BuildConfig.IS_DEBUG")
)
object BuildEnvironment {
    private var isDebugMode: Boolean? = null

    /**
     * 判断应用是否处于调试模式
     * 该方法已弃用；请改用 BuildConfig.IS_DEBUG（不同 sourceSet 下由不同的 BuildConfig.kt 提供）
     */
    @Deprecated("Use BuildConfig.IS_DEBUG instead.")
    fun isDebug(context: Context): Boolean {
        if (isDebugMode == null) {
            isDebugMode = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }
        return isDebugMode!!
    }
}
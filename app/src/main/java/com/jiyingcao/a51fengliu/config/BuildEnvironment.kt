package com.jiyingcao.a51fengliu.config

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * 环境配置提供者
 * 负责确定当前应用的运行环境和配置
 */
object BuildEnvironment {
    private var isDebugMode: Boolean? = null

    /**
     * 判断应用是否处于调试模式
     * 使用 ApplicationInfo 标志来确定，这是企业级应用的标准方法
     */
    fun isDebug(context: Context): Boolean {
        if (isDebugMode == null) {
            isDebugMode = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }
        return isDebugMode!!
    }
}
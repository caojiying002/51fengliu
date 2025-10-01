package com.jiyingcao.a51fengliu.util

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * 调试相关工具类
 * 提供运行时判断调试模式的 Fallback 方案
 */
object DebugUtils {
    /**
     * 基于 ApplicationInfo.FLAG_DEBUGGABLE 判断是否为调试模式
     *
     * 作为运行时 Fallback 方案保留，用于特殊场景需要动态判断的情况
     *
     * 正常情况下应优先使用编译期常量 BuildConfig.IS_DEBUG，
     * 该常量在编译时确定，零运行时开销，且支持 ProGuard/R8 优化
     *
     * @param context Android Context
     * @return true 表示当前为 Debug 构建，false 表示 Release 构建
     */
    fun isDebugBuild(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}

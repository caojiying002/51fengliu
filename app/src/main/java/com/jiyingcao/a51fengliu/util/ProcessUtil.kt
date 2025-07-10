package com.jiyingcao.a51fengliu.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

/**
 * 进程相关工具类
 * 提供多种获取进程名的方式，确保在各种Android版本下的兼容性
 */
object ProcessUtil {
    private const val TAG = "ProcessUtil"

    /**
     * 判断当前是否为主进程
     *
     * @param context 应用上下文
     * @return true表示主进程，false表示子进程
     */
    fun isMainProcess(context: Context): Boolean {
        val currentProcessName = getCurrentProcessName(context)
        val packageName = context.packageName

        return when {
            currentProcessName == packageName -> {
                AppLogger.d(TAG, "Running in main process: $currentProcessName")
                true
            }
            currentProcessName.startsWith("$packageName:") -> {
                AppLogger.d(TAG, "Running in sub process: $currentProcessName")
                false
            }
            else -> {
                // 这种情况很少见，但需要处理
                AppLogger.w(TAG, "Unexpected process name: $currentProcessName, package: $packageName")
                // 保守策略：如果无法确定，假设是主进程
                true  // 直接返回 true
            }
        }
    }

    /**
     * 获取当前进程名称（公开方法，供调试使用）
     */
    fun getCurrentProcessNameForDebug(context: Context): String {
        return getCurrentProcessName(context)
    }

    /**
     * 获取进程类型描述
     */
    fun getProcessTypeDescription(context: Context): String {
        val processName = getCurrentProcessName(context)
        val packageName = context.packageName

        return when {
            processName == packageName -> "主进程"
            processName.startsWith("$packageName:") -> {
                val suffix = processName.substringAfter(":")
                "子进程: $suffix"
            }
            else -> "未知进程类型: $processName"
        }
    }
    
    /**
     * 获取当前进程名称
     * 使用多种策略确保获取成功，按照优先级依次尝试
     * 
     * @param context 应用上下文
     * @return 进程名称，如果所有方法都失败则返回包名作为fallback
     */
    private fun getCurrentProcessName(context: Context): String {
        // 策略1: Android P+ 使用官方API (最可靠)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return runCatching {
                android.app.Application.getProcessName()
            }.getOrElse { e ->
                AppLogger.w(TAG, "Failed to get process name via Application.getProcessName()", e)
                getCurrentProcessNameFallback(context)
            }
        }
        
        // 策略2: 较低版本使用fallback方法
        return getCurrentProcessNameFallback(context)
    }
    
    /**
     * Fallback方法：结合多种技术获取进程名
     */
    private fun getCurrentProcessNameFallback(context: Context): String {
        // 方法1: 尝试通过ActivityManager获取
        runCatching {
            getCurrentProcessNameViaActivityManager(context)
        }.getOrNull()?.let { processName ->
            if (processName.isNotBlank()) {
                return processName
            }
        }
        
        // 方法2: 尝试通过/proc/self/cmdline获取
        runCatching {
            getCurrentProcessNameViaCmdline()
        }.getOrNull()?.let { processName ->
            if (processName.isNotBlank()) {
                return processName
            }
        }
        
        // 最后的fallback: 返回包名
        AppLogger.w(TAG, "All methods failed to get process name, falling back to package name")
        return context.packageName
    }
    
    /**
     * 通过ActivityManager获取进程名（传统方法）
     */
    private fun getCurrentProcessNameViaActivityManager(context: Context): String? {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return null
            
        val runningProcesses = activityManager.runningAppProcesses ?: return null
        val myPid = Process.myPid()
        
        return runningProcesses.find { it.pid == myPid }?.processName
    }
    
    /**
     * 通过读取/proc/self/cmdline获取进程名
     * 这是最底层的方法，通常最可靠
     */
    private fun getCurrentProcessNameViaCmdline(): String? {
        return try {
            BufferedReader(FileReader("/proc/self/cmdline")).use { reader ->
                var processName = reader.readLine()
                processName = processName?.trim()
                // cmdline可能包含null字符，需要清理
                processName?.substringBefore('\u0000')
            }
        } catch (e: IOException) {
            AppLogger.d(TAG, "Failed to read process name from /proc/self/cmdline", e)
            null
        }
    }
}
package com.jiyingcao.a51fengliu.util

import android.app.ActivityManager
import android.content.Context
import android.os.Process

/**
 * 进程相关工具类
 */
object ProcessUtil {
    
    /**
     * 获取当前进程名称
     */
    private fun getCurrentProcessName(context: Context): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses
        val myPid = Process.myPid()
        
        runningProcesses?.forEach { processInfo ->
            if (processInfo.pid == myPid) {
                return processInfo.processName
            }
        }
        return ""
    }
    
    /**
     * 判断当前是否为主进程
     */
    fun isMainProcess(context: Context): Boolean {
        return context.packageName == getCurrentProcessName(context)
    }
}
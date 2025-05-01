package com.jiyingcao.a51fengliu.util

import android.util.Log
import com.jiyingcao.a51fengliu.config.AppConfig

/**
 * 应用日志工具类
 * 只在Debug构建中输出日志，Release构建中自动禁用
 */
object AppLogger {
    private const val DEFAULT_TAG = "51fengliu"
    
    // 基础日志方法
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog()) {
            if (throwable != null) Log.v(tag, message, throwable)
            else Log.v(tag, message)
        }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog()) {
            if (throwable != null) Log.d(tag, message, throwable)
            else Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog()) {
            if (throwable != null) Log.i(tag, message, throwable)
            else Log.i(tag, message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog()) {
            if (throwable != null) Log.w(tag, message, throwable)
            else Log.w(tag, message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (shouldLog()) {
            if (throwable != null) Log.e(tag, message, throwable)
            else Log.e(tag, message)
        }
    }
    
    // 使用默认标签的快捷方法
    fun v(message: String) = v(DEFAULT_TAG, message)
    fun d(message: String) = d(DEFAULT_TAG, message)
    fun i(message: String) = i(DEFAULT_TAG, message)
    fun w(message: String, throwable: Throwable? = null) = w(DEFAULT_TAG, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = e(DEFAULT_TAG, message, throwable)
    
    // 集中判断是否应该输出日志
    private fun shouldLog(): Boolean {
        return AppConfig.Debug.isLoggingEnabled()
        // 未来可以在这里添加更多的判断条件
        // 例如: && otherCondition
    }
}
package com.jiyingcao.a51fengliu.config

import android.content.Context

/**
 * 应用全局配置类
 * 集中管理所有的配置常量，按功能域分组
 */
object AppConfig {
    private lateinit var applicationContext: Context

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * 网络相关配置
     */
    object Network {
        const val BASE_URL = "https://127hei.info"
        const val BASE_IMAGE_URL = "https://s1.img115.xyz/info/picture/"
        const val CONNECT_TIMEOUT = 15L // 秒
        const val READ_TIMEOUT = 15L // 秒
        const val WRITE_TIMEOUT = 15L // 秒
    }

    /**
     * 存储相关配置
     */
    object Storage {
        const val IMAGE_SUB_FOLDER = "51fengliu"
        const val CACHE_DIR_NAME = "app_cache"
    }

    /**
     * 调试相关配置
     */
    object Debug {
        // 通过函数提供，确保 applicationContext 已初始化
        fun isLoggingEnabled(): Boolean = BuildEnvironment.isDebug(applicationContext)

        fun useDebugToken(): Boolean = BuildEnvironment.isDebug(applicationContext)

        const val DEBUG_TOKEN = ""
    }
}
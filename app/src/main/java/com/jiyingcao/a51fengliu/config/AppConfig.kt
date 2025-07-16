package com.jiyingcao.a51fengliu.config

import android.content.Context
import com.jiyingcao.a51fengliu.glide.HostInvariantGlideUrl

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
        const val BASE_URL = "https://65944fca1.n5api.xyz"
        const val BASE_IMAGE_URL = "https://s5.img719.xyz/info/picture/"
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L

        /**
         * 用于HTTP请求的User-Agent头
         * 后端API要求指定该值，否则会返回HTTP 403
         */
        const val USER_AGENT = "Dart/3.0 (dart:io)"

        /**
         * 用于HTTP请求的Referer头
         * 图片API需要验证请求来源，否则会下载失败（可能是为了防盗链）
         */
        const val REFERER = "https://ysalgfhqd3.com"

        /**
         * 创建一个带有主机无关缓存键的图片URL
         * 即使BASE_IMAGE_URL的主机部分变化，缓存仍然有效
         *
         * @param imagePath 图片路径（不包含基本URL）
         * @return 可以传递给Glide的HostInvariantGlideUrl
         */
        fun createImageUrl(imagePath: String): HostInvariantGlideUrl {
            return HostInvariantGlideUrl(BASE_IMAGE_URL + imagePath)
        }
    }

    /**
     * 存储相关配置
     */
    object Storage {
        const val IMAGE_SUB_FOLDER = "51fengliu"
        const val CACHE_DIR_NAME = "app_cache"
    }

    /**
     * UI功能相关配置
     */
    object UI {
        /**
         * 控制是否启用共享元素转场动画
         * 可以在发布时根据测试效果决定是否启用
         */
        const val SHARED_ELEMENT_TRANSITIONS_ENABLED = true
    }

    /**
     * 调试相关配置
     */
    object Debug {
        /**
         * 开发过程中可以手动切换是否使用调试Token的开关
         * 只在Debug构建中生效，Release构建始终不使用调试Token
         */
        private const val DEFAULT_USE_DEBUG_TOKEN = false

        /**
         * 用于开发测试的默认Token值
         * 只在Debug构建且DEFAULT_USE_DEBUG_TOKEN为true时使用
         */
        const val DEFAULT_DEBUG_TOKEN = ""

        /**
         * 开发过程中可以手动切换的HTTP日志开关
         * 只在Debug构建中生效，Release构建始终关闭日志
         */
        private const val DEFAULT_HTTP_LOGGING_ENABLED = true

        /**
         * 开发过程中可以手动切换的图片加载日志开关
         * 只在Debug构建中生效，Release构建始终关闭日志
         */
        private const val DEFAULT_IMAGE_LOADING_LOGGING_ENABLED = false

        /**
         * 开发过程中可以手动切换是否跳过大图查看功能限制的开关
         * 只在Debug构建中生效，Release构建始终检查限制
         */
        private const val DEFAULT_BYPASS_LARGE_IMAGE_CHECK = false

        /**
         * 全局日志开关
         * 只在Debug构建中返回true，Release构建始终返回false
         * 
         * @return 是否启用日志
         */
        fun isLoggingEnabled(): Boolean = BuildEnvironment.isDebug(applicationContext)

        /**
         * 控制是否使用调试用的Token
         * 只在Debug构建中生效，Release构建始终不使用调试Token
         * 
         * @return 是否使用调试Token
         */
        fun useDebugToken(): Boolean =
            DEFAULT_USE_DEBUG_TOKEN
                && DEFAULT_DEBUG_TOKEN.isNotBlank()
                && BuildEnvironment.isDebug(applicationContext)

        /**
         * 控制HTTP请求日志是否打印到控制台
         * 只在Debug构建中生效，Release构建始终不打印
         * 
         * @return 是否打印HTTP请求日志
         */
        fun isHttpLoggingEnabled(): Boolean = 
            DEFAULT_HTTP_LOGGING_ENABLED && BuildEnvironment.isDebug(applicationContext)

        /**
         * 控制图片加载请求日志是否打印到控制台
         * 只在Debug构建中生效，Release构建始终不打印
         * 
         * @return 是否打印图片加载日志
         */
        fun isImageLoadingLoggingEnabled(): Boolean = 
            DEFAULT_IMAGE_LOADING_LOGGING_ENABLED && BuildEnvironment.isDebug(applicationContext)

        /**
         * 控制是否跳过大图查看功能限制检查
         * 只在Debug构建中生效，Release构建始终检查限制
         * 
         * @return 是否跳过限制检查
         */
        fun bypassLargeImageCheck(): Boolean = 
            DEFAULT_BYPASS_LARGE_IMAGE_CHECK && BuildEnvironment.isDebug(applicationContext)
    }
}
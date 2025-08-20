package com.jiyingcao.a51fengliu.coil

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.jiyingcao.a51fengliu.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Coil3 配置类
 * 创建和配置 ImageLoader 实例，支持自定义请求头和主机无关的缓存键
 */
object CoilConfig {

    /**
     * 创建 ImageLoader 实例
     * 
     * @param context 应用上下文
     * @return 配置好的 ImageLoader 实例
     */
    fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = createOkHttpClient()))
                add(HostInvariantKeyer())
            }
            .logger(if (AppConfig.Debug.isImageLoadingLoggingEnabled()) DebugLogger() else null)
            .build()
    }

    /**
     * 创建配置好的 OkHttpClient
     * 添加图片专用请求头和日志拦截器
     */
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            // 增加两个图片专用请求头
            addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("Referer", AppConfig.Network.IMAGE_REFERER)
                    .header("User-Agent", AppConfig.Network.IMAGE_USER_AGENT)
                    .build()
                chain.proceed(requestWithHeaders)
            }

            // 只在需要时添加图片加载日志拦截器
            if (AppConfig.Debug.isImageLoadingLoggingEnabled()) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                })
            }
            
            // 设置超时参数
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(10, TimeUnit.SECONDS)
            writeTimeout(10, TimeUnit.SECONDS)
        }.build()
    }
}
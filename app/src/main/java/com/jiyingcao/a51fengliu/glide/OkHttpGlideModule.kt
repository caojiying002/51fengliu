package com.jiyingcao.a51fengliu.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.jiyingcao.a51fengliu.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.util.concurrent.TimeUnit.SECONDS

@GlideModule
class OkHttpGlideModule: AppGlideModule() {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            // 增加两个图片专用请求头
            addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("Referer", AppConfig.Network.REFERER)
                    .header("User-Agent", AppConfig.Network.USER_AGENT)
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
            connectTimeout(10, SECONDS)
            readTimeout(10, SECONDS)
            writeTimeout(10, SECONDS)
        }.build()
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient)
        )
    }
}
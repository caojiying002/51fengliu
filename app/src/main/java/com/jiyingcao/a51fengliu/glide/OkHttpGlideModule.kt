package com.jiyingcao.a51fengliu.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.jiyingcao.a51fengliu.api.BASE_URL
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.util.concurrent.TimeUnit.SECONDS

private const val DEBUG_IMAGE_LOADING = false

private const val REFERER = "https://ysalgfhqd3.com"
private const val USER_AGENT = "Dart/3.0 (dart:io)"
// User-Agent on Firefox, Windows
// "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"

@GlideModule
class OkHttpGlideModule: AppGlideModule() {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = when (DEBUG_IMAGE_LOADING) {
                    true -> HttpLoggingInterceptor.Level.HEADERS
                    false -> HttpLoggingInterceptor.Level.NONE
                }
            })
            // 添加拦截器增加两个图片专用请求头
            .addInterceptor { chain ->
                // 如果URL是图片请求（主URL为BASE_IMAGE_URL），则添加两个图片专用请求头
                // if (originalRequest.url.toString().startsWith(BASE_IMAGE_URL))
                val originalRequest = chain.request()
                val requestWithHeaders = originalRequest.newBuilder()
                    .header("Referer", REFERER)
                    .header("User-Agent", USER_AGENT)
                    .build()
                return@addInterceptor chain.proceed(requestWithHeaders)
            }
            .connectTimeout(10, SECONDS)
            .readTimeout(10, SECONDS)
            .writeTimeout(10, SECONDS)
            .build()

    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient)
        )
    }
}
package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.parse.ApiCallAdapterFactory
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.data.TokenManager
import java.util.concurrent.TimeUnit.SECONDS
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val okHttpClient by lazy {
        OkHttpClient.Builder().apply {
            // 后端API要求指定User-Agent，不然会返回HTTP 403
            addInterceptor { chain ->
                val requestWithHeaders = chain.request().newBuilder()
                    .header("User-Agent", AppConfig.Network.USER_AGENT)
                    // X-Requested-With头不强制使用，并且习惯上大写居多。这里加上是为了与官方APP行为保持一致
                    .header("x-requested-with", "XMLHttpRequest")
                    .build()
                chain.proceed(requestWithHeaders)
            }

            addInterceptor(
                if (AppConfig.Debug.useDebugToken()) {
                    // 开发阶段可以指定某个debug token避免登录，加速开发
                    DebugAuthInterceptor(
                        debugToken = AppConfig.Debug.DEFAULT_DEBUG_TOKEN,
                        enabled = true
                    )
                } else {
                    // 正式环境：从TokenManager中获取Token并添加到请求头中
                    AuthInterceptor(
                        TokenManager.getInstance()
                    )
                }
            )

            // Debug环境：按需打印网络日志
            if (AppConfig.Debug.isHttpLoggingEnabled()) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }

            // 超时设置
            connectTimeout(AppConfig.Network.CONNECT_TIMEOUT, SECONDS)
            readTimeout(AppConfig.Network.READ_TIMEOUT, SECONDS)
            writeTimeout(AppConfig.Network.WRITE_TIMEOUT, SECONDS)
        }.build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(AppConfig.Network.BASE_URL)
            .addCallAdapterFactory(ApiCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create(GsonInstance.gson).withStreaming())
            //.addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    /**
     * API服务实例
     * 提供所有后端API接口的访问入口
     */
    val apiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.data.TokenManager
import java.util.concurrent.TimeUnit.SECONDS
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val DEBUG_HTTP: Boolean = (true)

private val USE_DEBUG_TOKEN: Boolean = (false)
private val DEBUG_TOKEN: String = ""

const val BASE_URL = "https://910.da576.xyz/"   //"https://309.16dress.xyz/" //"https://903.16duty.xyz/"

object RetrofitClient {

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = when (DEBUG_HTTP) {
                    true -> HttpLoggingInterceptor.Level.BODY
                    false -> HttpLoggingInterceptor.Level.HEADERS
                }
            })
            .addInterceptor(
                if (USE_DEBUG_TOKEN) {
                    // 【只在测试环境下使用】可以手动指定一个固定的Token方便开发，不需要反复登录
                    DebugAuthInterceptor(
                        debugToken = DEBUG_TOKEN,
                        enabled = true
                    )
                } else {
                    // 正式环境：从TokenManager中获取Token并添加到请求头中
                    AuthInterceptor(
                        TokenManager.getInstance()
                    )
                }
            )
            .connectTimeout(15, SECONDS)
            .readTimeout(15, SECONDS)
            .writeTimeout(15, SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(GsonInstance.gson))
            //.addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    val apiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
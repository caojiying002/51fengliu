package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val DEBUG_HTTP: Boolean = (false)

const val BASE_URL = "https://309.16dress.xyz/" //"https://903.16duty.xyz/"

object RetrofitClient {

    val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = when (DEBUG_HTTP) {
                    true -> HttpLoggingInterceptor.Level.BODY
                    false -> HttpLoggingInterceptor.Level.HEADERS
                }
            })
            // 添加拦截器增加两个图片专用请求头
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                // 如果URL是图片请求（主URL为BASE_IMAGE_URL），则添加两个图片专用请求头
                if (originalRequest.url.toString().startsWith(BASE_IMAGE_URL)) {
                    val requestWithHeaders = originalRequest.newBuilder()
                        .header("Referer", BASE_URL)
                        //.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                        .build()
                    return@addInterceptor chain.proceed(requestWithHeaders)
                }
                return@addInterceptor chain.proceed(originalRequest)
            }
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
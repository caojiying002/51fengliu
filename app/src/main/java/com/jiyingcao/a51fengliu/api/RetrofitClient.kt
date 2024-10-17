package com.jiyingcao.a51fengliu.api

import com.google.net.cronet.okhttptransport.CronetInterceptor
import com.jiyingcao.a51fengliu.App
import java.util.concurrent.TimeUnit.SECONDS
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.chromium.net.CronetEngine
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val DEBUG_HTTP: Boolean = (true)

const val BASE_URL = "https://910.da576.xyz/"   //"https://309.16dress.xyz/" //"https://903.16duty.xyz/"

object RetrofitClient {

    private val cronetEngine by lazy {
        CronetEngine.Builder(App.INSTANCE)
            .enableHttp2(true)
            .enableQuic(true)
            .build()
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = when (DEBUG_HTTP) {
                    true -> HttpLoggingInterceptor.Level.BODY
                    false -> HttpLoggingInterceptor.Level.HEADERS
                }
            })
            // 开发期间手动添加token
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val urlEncodedPath = originalRequest.url.encodedPath

                val token = "eyJhbGciOiJIUzI1NiJ9.eyJsYXN0TG9naW4iOjE3MjgxOTg2MjIsInN1YiI6ImppeWluZ2NhbyIsImV4cCI6MTczMDg3NzAyMiwiaWF0IjoxNzI4MTk4NjIyLCJqdGkiOiIxMjQ4NDEzIn0.TMJOVRxfQiwmx_hLNAr1AExRxk4Pj8EKmmO3EI9QO6k"    // TODO 开发期间手动添加token
                if (urlEncodedPath == "/api/web/authUser/detail.json") {
                    val request = originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    return@addInterceptor chain.proceed(request)
                }
                chain.proceed(originalRequest)
            }
            // Cronet
            .addInterceptor(
                CronetInterceptor
                    .newBuilder(cronetEngine)
                    .build()
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
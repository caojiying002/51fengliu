package com.jiyingcao.a51fengliu.api

import java.util.concurrent.TimeUnit.SECONDS
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val DEBUG_HTTP: Boolean = (true)

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
            // 开发期间手动添加token
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val urlEncodedPath = originalRequest.url.encodedPath

                /* jiyingcao */
                //val token = "eyJhbGciOiJIUzI1NiJ9.eyJsYXN0TG9naW4iOjE3MzQ2Nzc5NTEsInN1YiI6ImppeWluZ2NhbyIsImV4cCI6MTczNzM1NjM1MSwiaWF0IjoxNzM0Njc3OTUxLCJqdGkiOiIxMjQ4NDEzIn0.NOf3T_sXfOGXxJ1ZJMM02NBVpF2cLdqwzbHd9nckGlc"
                /* caojiying */
                val token = "eyJhbGciOiJIUzI1NiJ9.eyJsYXN0TG9naW4iOjE3MzU2MjEyNTksInN1YiI6ImNhb2ppeWluZyIsImV4cCI6MTczODI5OTY1OSwiaWF0IjoxNzM1NjIxMjU5LCJqdGkiOiI4NzExMDQifQ.cyFkrQHDkppna_XTmmEh7A-UXPsTD6lR0fjdzR6GJyk"

                if (urlEncodedPath == "/api/web/authUser/detail.json"
                    || urlEncodedPath == "/api/web/info/favorite.json"
                    || urlEncodedPath == "/api/web/info/unfavorite.json"
                    /* 开发期间的测试条件：详情页带上Token，如不需要Token可以注释掉 */
//                    || urlEncodedPath == "/api/web/info/detail.json"
                    ) {
                    val request = originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    return@addInterceptor chain.proceed(request)
                }
                chain.proceed(originalRequest)
            }
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
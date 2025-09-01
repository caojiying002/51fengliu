package com.jiyingcao.a51fengliu.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.AuthInterceptor
import com.jiyingcao.a51fengliu.api.DebugAuthInterceptor
import com.jiyingcao.a51fengliu.api.parse.LoginResponseAdapter
import com.jiyingcao.a51fengliu.api.parse.ReportResponseAdapter
import com.jiyingcao.a51fengliu.api.parse.NoDataTypeAdapter
import com.jiyingcao.a51fengliu.api.response.LoginResponse
import com.jiyingcao.a51fengliu.api.response.ReportResponse
import com.jiyingcao.a51fengliu.api.response.NoData
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.data.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return OkHttpClient.Builder().apply {
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
                    AuthInterceptor(tokenManager)
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

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(LoginResponse::class.java, LoginResponseAdapter())
            .registerTypeAdapter(ReportResponse::class.java, ReportResponseAdapter())
            .registerTypeAdapter(NoData::class.java, NoDataTypeAdapter())
            .create()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(AppConfig.Network.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson).withStreaming())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
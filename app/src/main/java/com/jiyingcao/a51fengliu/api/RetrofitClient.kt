package com.jiyingcao.a51fengliu.api

import android.util.Log
import com.jiyingcao.a51fengliu.api.GsonInstance.gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun Retrofit.setUrl(url: String): Retrofit {
    return newBuilder().baseUrl(url).build()
}

@Deprecated("使用RetrofitClient2")
object RetrofitClient {

    private val DEBUG_HTTP: Boolean = (false /*&& BuildConfig.DEBUG*/)

    val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = when (DEBUG_HTTP) {
                    true -> HttpLoggingInterceptor.Level.BODY
                    false -> HttpLoggingInterceptor.Level.HEADERS
                }
            })
            .build()
    }

    /**
     * 用于动态切换URL
     *
     * 1. 从99khredira.xyz获取最新的URL
     * 2. 保存到本地
     * 3. 使用最新的URL
     * 4. 重置Retrofit
     * 5. 重置ApiService
     */
    var retrofitUrl: String = "Uninitialized"
        set(value) {
            field = value
            _retrofit = when (_retrofit) {
                null -> createRetrofit(value)
                else -> _retrofit!!.newBuilder().baseUrl(value).build()
            }
            _apiService = _retrofit!!.create(ApiService::class.java)
            Log.i("Retrofit", "Retrofit URL updated to ($value)")
        }

    private var _retrofit: Retrofit? = null

    private var _apiService: ApiService? = null
    val apiService: ApiService
        get() {
            if (_apiService == null) {
                // 通过retrofitUrl初始化_retrofit和_apiService
                retrofitUrl = BASE_URL_FIXED
            }
            return _apiService!!
        }

    private fun createRetrofit(baseUrl: String) =
         Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            //.addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()

    @Deprecated("")
    fun setUrl(urlFromWeb: String) {
        // TODO skip setting url if it's the same as the current one
        BASE_URL = when {
            urlFromWeb.isBlank() -> BASE_URL_FIXED
            else -> urlFromWeb
        }
        _retrofit = when (_retrofit) {
            null -> createRetrofit(BASE_URL)
            else -> _retrofit!!.newBuilder().baseUrl(BASE_URL).build()
        }
        _apiService = _retrofit!!.create(ApiService::class.java)
    }
}
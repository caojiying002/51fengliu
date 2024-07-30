package com.jiyingcao.a51fengliu.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

private const val TAG = "FetchLatestUrl"

/** May be invalid. 写死的URL, 可能会失效. */
const val BASE_URL_FIXED = "https://main.khappaa.xyz" // "https://随.74baoes.xyz"   // "https://m.74derty.xyz"

/** 地址获取页面 */
const val DYNAMIC_URL_FROM = "https://99khredira.xyz"

var BASE_URL: String = BASE_URL_FIXED
    set(value) {
        field = value
        Log.i(TAG, "BASE_URL updated to ($value)")
    }
//const val BASE_IMG_URL = BASE_URL

typealias SubUrl = String
typealias FullUrl = String

fun SubUrl.toFullUrl(host: String = BASE_URL): FullUrl = "$host$this"
//fun SubUrl.toImageFullUrl(host: String = BASE_IMG_URL): FullUrl = "$host$this"

/**
 * 获取最新的URL
 * 来源：https://api.appgetnewu.xyz/inter/nowurl/getnow
 */
fun fetchLatestUrlV2(
    url: String = "https://api.appgetnewu.xyz/inter/nowurl/getnow",
    okHttpClient: OkHttpClient = RetrofitClient.okHttpClient
): String? {
    val request = Request.Builder()
        .url(url)
        .build()

    try {
        Log.d(TAG, "fetching latest url...")
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "fetchLatestUrl: unexpected response $response")
            return null
        }

        val rawResult = response.body?.string()
        if (rawResult != null && rawResult.length > 1
            && rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
            // 去掉开头和结尾的引号
            val quotesRemoved = rawResult.substring(1, rawResult.length - 1)

            // 这里用java.net.URL的构造函数来检查URL是否合法，如果不合法会抛出MalformedURLException异常
            return URL(quotesRemoved).toString()
        }
    } catch (e: MalformedURLException) {
        Log.w(TAG, "不是合法的URL格式", e)
    }  catch (e: Exception) {
        Log.w(TAG, "fetchLatestUrlV2: failed", e)
    }

    return null
}

/**
 * 获取最新的URL
 * 来源：https://99khredira.xyz
 */
@Deprecated("使用fetchLatestUrlV2代替")
fun fetchLatestUrl(
    url: String = DYNAMIC_URL_FROM,
    okHttpClient: OkHttpClient = RetrofitClient.okHttpClient
): String? {
    val request = Request.Builder()
        .url(url)
        .build()

    try {
        Log.d(TAG, "fetching latest url...")
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "fetchLatestUrl: unexpected response $response")
            return null
        }

        response.body?.string()?.let { html ->
            return extractLatestUrl(html)
        }
    } catch (e: Exception) {
        Log.w(TAG, "fetchLatestUrl: failed", e)
    }

    return null
}

/**
 * 网页以如下格式返回最新的URL：
 * <div>https://70e4ns.xyz</div>
 */
@Deprecated("fetchLatestUrlV2不再需要jsoup从网页中提取URL")
private fun extractLatestUrl(html: String, pattern: String = "https:\\/\\/.*\\.xyz"): String? {
    val doc = Jsoup.parse(html)
    val urlDiv = doc.select("div")
        .firstOrNull { it.text().matches(pattern.toRegex()) }
    return urlDiv?.text()
}
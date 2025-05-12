package com.jiyingcao.a51fengliu.glide

import android.net.Uri
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers

/**
 * 自定义GlideUrl实现，生成主机(Host)无关的缓存键，因此可以在主URL发生变化时已有的图片缓存不会失效
 */
class HostInvariantGlideUrl(val originalUrl: String, headers: Headers = Headers.DEFAULT) : GlideUrl(originalUrl, headers) {
    private val cacheKey: String

    init {
        // 只使用路径(Path)和参数(Query)作为缓存键，忽略主机(Host)部分
        val uri = Uri.parse(originalUrl)
        cacheKey = uri.path + (uri.query?.let { "?$it" } ?: "")
    }

    override fun getCacheKey(): String = cacheKey

    /**
     * 返回原始URL字符串
     */
    override fun toString(): String = originalUrl
}
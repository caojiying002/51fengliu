package com.jiyingcao.a51fengliu.glide

import android.net.Uri
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers

/**
 * 自定义GlideUrl实现，生成不受主机部分变化影响的缓存键
 */
class HostInvariantGlideUrl(val originalUrl: String, headers: Headers = Headers.DEFAULT) : GlideUrl(originalUrl, headers) {
    private val cacheKey: String

    init {
        // 提取路径和查询参数作为缓存键
        val uri = Uri.parse(originalUrl)
        cacheKey = uri.path + (uri.query?.let { "?$it" } ?: "")
    }

    override fun getCacheKey(): String = cacheKey

    /**
     * 返回原始URL字符串
     */
    override fun toString(): String = originalUrl
}
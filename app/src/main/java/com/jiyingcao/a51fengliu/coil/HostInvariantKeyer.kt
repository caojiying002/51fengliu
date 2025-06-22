package com.jiyingcao.a51fengliu.coil

import android.net.Uri
import coil3.key.Keyer
import coil3.request.Options

/**
 * 主机无关的缓存键生成器
 * 生成主机(Host)无关的缓存键，因此可以在主URL发生变化时已有的图片缓存不会失效
 * 
 * 参考 HostInvariantGlideUrl 的实现逻辑
 */
class HostInvariantKeyer : Keyer<String> {

    override fun key(data: String, options: Options): String? {
        return try {
            // 只使用路径(Path)和参数(Query)作为缓存键，忽略主机(Host)部分
            val uri = Uri.parse(data)
            uri.path + (uri.query?.let { "?$it" } ?: "")
        } catch (e: Exception) {
            // 如果解析失败，返回null让Coil使用默认的缓存键
            null
        }
    }
}
package com.jiyingcao.a51fengliu.coil

import android.content.Context
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.util.AppLogger
import com.jiyingcao.a51fengliu.util.SaveImageResult
import com.jiyingcao.a51fengliu.util.saveImage
import com.jiyingcao.a51fengliu.util.needsStoragePermission
import com.jiyingcao.a51fengliu.util.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

/**
 * 使用 Coil 磁盘缓存保存图片到相册
 *
 * @param context 上下文
 * @param imageUrl 图片 URL
 * @param diskCacheKey 磁盘缓存Key
 * @param subFolder 子文件夹名称，默认使用 AppConfig 中配置的值
 * @param scope 可选的协程作用域，如果不提供则创建新的作用域
 */
fun coilSaveImageFromCache(
    context: Context,
    imageUrl: String,
    diskCacheKey: String,
    subFolder: String? = AppConfig.Storage.IMAGE_SUB_FOLDER,
    scope: CoroutineScope? = null
) {
    // 检查权限，避免无权限时的无效操作
    if (needsStoragePermission(context)) {
        context.showToast("需要存储权限才能保存图片")
        return
    }
    
    val actualScope = scope ?: CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    actualScope.launch(Dispatchers.IO) {
        try {
            // 获取Coil的磁盘缓存
            val diskCache = SingletonImageLoader.get(context).diskCache
            if (diskCache == null) {
                context.showToast("缓存不可用")
                return@launch
            }
            
            // 从磁盘缓存获取图片文件
            val snapshot = diskCache.openSnapshot(diskCacheKey)
            if (snapshot == null) {
                context.showToast("图片缓存不存在")
                return@launch
            }
            
            // 使用 use 确保资源正确释放
            snapshot.use { cacheSnapshot ->
                // 获取缓存文件
                val cacheFile = cacheSnapshot.data.toFile()
                
                // 提取文件名
                val fileName = extractFileNameFromUrl(imageUrl)
                
                // 保存图片
                val result = saveImage(context, cacheFile, subFolder, fileName)
                
                when (result) {
                    is SaveImageResult.Success -> context.showToast("图片已保存")
                    is SaveImageResult.Error -> {
                        context.showToast("图片保存失败: ${result.message}")
                        result.exception?.let { 
                            AppLogger.w("图片保存失败", it)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            context.showToast("保存图片时发生错误: ${e.message}")
            AppLogger.w("保存图片时发生错误", e)
        }
    }
}

/**
 * CoroutineScope 的扩展函数版本，作为语法糖
 */
fun CoroutineScope.coilSaveImageFromCache(
    context: Context,
    imageUrl: String,
    diskCacheKey: String,
    subFolder: String? = AppConfig.Storage.IMAGE_SUB_FOLDER
) = coilSaveImageFromCache(context, imageUrl, diskCacheKey, subFolder, this)

/**
 * 从URL中提取文件名
 */
private fun extractFileNameFromUrl(url: String): String {
    val path = try {
        URL(url).path
    } catch (e: Exception) {
        url
    }
    return File(path).name
}
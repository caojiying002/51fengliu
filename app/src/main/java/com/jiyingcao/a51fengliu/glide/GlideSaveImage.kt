package com.jiyingcao.a51fengliu.glide

import android.content.Context
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.util.ImageLoader
import kotlinx.coroutines.CoroutineScope

/**
 * 使用 Glide 下载图片并保存到相册
 *
 * @param context 上下文
 * @param imageUrl 图片 URL
 * @param subFolder 子文件夹名称，默认使用 AppConfig 中配置的值
 * @param scope 可选的协程作用域，如果不提供则创建新的作用域
 */
fun glideSaveImage(
    context: Context, 
    imageUrl: String, 
    subFolder: String? = AppConfig.Storage.IMAGE_SUB_FOLDER,
    scope: CoroutineScope? = null
) {
    // Use ImageLoader's saveToGallery instead
    ImageLoader.saveToGallery(context, imageUrl, subFolder, scope)
}

/**
 * CoroutineScope 的扩展函数版本，作为语法糖
 */
fun CoroutineScope.glideSaveImage(
    context: Context,
    imageUrl: String,
    subFolder: String? = AppConfig.Storage.IMAGE_SUB_FOLDER
) = ImageLoader.saveToGallery(context, imageUrl, subFolder, this)
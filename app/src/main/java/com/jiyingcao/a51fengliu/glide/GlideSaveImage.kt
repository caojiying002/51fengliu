package com.jiyingcao.a51fengliu.glide

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.util.SaveImageResult
import com.jiyingcao.a51fengliu.util.saveImage
import com.jiyingcao.a51fengliu.util.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

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
    val actualScope = scope ?: CoroutineScope(Dispatchers.IO)
    
    // 使用 URL 类提取文件路径
    val url = URL(imageUrl)
    val fileName = File(url.path).name

    // 使用 Glide 下载图片文件
    GlideApp.with(context)
        .downloadOnly()
        .load(imageUrl)
        .into(object : CustomTarget<File>() {
            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                actualScope.launch(Dispatchers.IO) {
                    val result = saveImage(context, resource, subFolder, fileName)
                    
                    // 切换到主线程但保持在同一个作用域内
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is SaveImageResult.Success -> context.showToast("图片已保存")
                            is SaveImageResult.Error -> {
                                context.showToast("图片保存失败: ${result.message}")
                                result.exception?.printStackTrace()
                            }
                        }
                    }
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                // 清除占位符或其他资源
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                context.showToast("图片下载失败")
            }
        })
}

/**
 * CoroutineScope 的扩展函数版本，作为语法糖
 */
fun CoroutineScope.glideSaveImage(
    context: Context,
    imageUrl: String,
    subFolder: String? = AppConfig.Storage.IMAGE_SUB_FOLDER
) = glideSaveImage(context, imageUrl, subFolder, this)
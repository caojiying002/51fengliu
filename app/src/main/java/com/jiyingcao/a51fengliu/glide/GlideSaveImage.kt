package com.jiyingcao.a51fengliu.glide

import android.content.ContentValues
import android.content.Context
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.jiyingcao.a51fengliu.util.saveImageLegacyAPI
import com.jiyingcao.a51fengliu.util.saveImageMediaStoreAPI
import com.jiyingcao.a51fengliu.util.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

private const val SUB_FOLDER = "51fengliu"

/**
 * 使用 Glide 下载图片并保存到相册
 *
 * @param context 上下文
 * @param imageUrl 图片 URL
 * @param subFolder 子文件夹名称
 */
fun glideSaveImage(context: Context, imageUrl: String, subFolder: String = SUB_FOLDER) {
    // 使用 URL 类提取文件路径
    val url = URL(imageUrl)
    val fileName = File(url.path).name

    // 使用 Glide 下载图片文件
    GlideApp.with(context)
        .downloadOnly()
        .load(imageUrl)
        .into(object : CustomTarget<File>() {
            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                // 使用协程将文件保存操作移到 IO 线程
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            saveImageMediaStoreAPI(context, resource.inputStream(), subFolder, fileName)
                        } else {
                            saveImageLegacyAPI(context, resource.inputStream(), subFolder, fileName)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        context.showToast("图片保存失败")
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
package com.jiyingcao.a51fengliu.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 图片保存结果
 */
sealed class SaveImageResult {
    data class Success(val uri: String) : SaveImageResult()
    data class Error(val message: String, val exception: Exception? = null) : SaveImageResult()
}

/**
 * 保存图片的统一方法 - File版本
 */
suspend fun saveImage(
    context: Context,
    file: File,
    subFolder: String? = null,
    fileName: String? = null,
    mimeType: String = "image/jpeg"
): SaveImageResult = withContext(Dispatchers.IO) {
    try {
        val actualFileName = fileName ?: "image_${System.currentTimeMillis()}.jpg"
        val actualFolder = subFolder?.takeIf { it.isNotEmpty() } ?: ""
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageMediaStoreAPI(context, file, actualFolder, actualFileName, mimeType)
        } else {
            saveImageLegacyAPI(context, file, actualFolder, actualFileName)
        }
    } catch (e: Exception) {
        SaveImageResult.Error("保存失败: ${e.message}", e)
    }
}

/**
 * 保存图片的统一方法 - InputStream版本
 * 注意：此方法不会关闭传入的输入流，调用者需要自行负责关闭
 */
suspend fun saveImage(
    context: Context,
    inputStream: InputStream,
    subFolder: String? = null,
    fileName: String? = null,
    mimeType: String = "image/jpeg"
): SaveImageResult = withContext(Dispatchers.IO) {
    try {
        // 创建临时文件
        val tempFile = File.createTempFile("temp_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        
        // 调用File版本
        saveImage(context, tempFile, subFolder, fileName, mimeType).also {
            // 清理临时文件
            tempFile.delete()
        }
    } catch (e: Exception) {
        SaveImageResult.Error("无法处理输入流: ${e.message}", e)
    }
}

/**
 * 保存图片的统一方法 - Bitmap版本
 */
suspend fun saveImage(
    context: Context,
    bitmap: Bitmap,
    subFolder: String? = null,
    fileName: String? = null,
    mimeType: String = "image/jpeg",
    quality: Int = 100
): SaveImageResult = withContext(Dispatchers.IO) {
    try {
        val tempFile = File.createTempFile("temp_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { output ->
            bitmap.compress(
                when (mimeType) {
                    "image/png" -> Bitmap.CompressFormat.PNG
                    "image/webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
                        Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }, 
                quality, 
                output
            )
        }
        
        // 调用File版本
        saveImage(context, tempFile, subFolder, fileName, mimeType).also {
            // 清理临时文件
            tempFile.delete()
        }
    } catch (e: Exception) {
        SaveImageResult.Error("无法保存Bitmap: ${e.message}", e)
    }
}

/**
 * MediaStore API实现 (Android Q及以上)
 */
private fun saveImageMediaStoreAPI(
    context: Context,
    file: File,
    subFolder: String,
    fileName: String,
    mimeType: String
): SaveImageResult {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + 
            (if (subFolder.isNotEmpty()) File.separator + subFolder else ""))
    }
    
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return SaveImageResult.Error("图片保存失败: Uri创建失败")
    
    // 将文件复制到目标位置
    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
        FileInputStream(file).use { inputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: return SaveImageResult.Error("图片保存失败: 无法打开输出流")
    
    // 通知系统相册更新
    MediaScannerConnection.scanFile(context, arrayOf(uri.toString()), arrayOf(mimeType), null)
    
    return SaveImageResult.Success(uri.toString())
}

/**
 * 传统API实现 (Android Q以下)
 */
private fun saveImageLegacyAPI(
    context: Context,
    file: File,
    subFolder: String,
    fileName: String
): SaveImageResult {
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val saveDir = if (subFolder.isEmpty()) picturesDir else File(picturesDir, subFolder)
    
    if (!saveDir.exists() && !saveDir.mkdirs()) {
        return SaveImageResult.Error("无法创建目录: ${saveDir.absolutePath}")
    }
    
    val saveFile = File(saveDir, fileName)
    try {
        FileOutputStream(saveFile).use { outputStream ->
            FileInputStream(file).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    } catch (e: Exception) {
        return SaveImageResult.Error("保存文件失败: ${e.message}", e)
    }
    
    // 通知系统相册更新
    MediaScannerConnection.scanFile(context, arrayOf(saveFile.absolutePath), null, null)
    
    return SaveImageResult.Success(saveFile.absolutePath)
}

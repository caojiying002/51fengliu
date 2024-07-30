package com.jiyingcao.a51fengliu.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun saveImageMediaStoreAPI(
    context: Context,
    inputStream: InputStream,
    subFolder: String,
    fileName: String,
) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + subFolder)
        //put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    if (uri != null) {
        // 通知系统相册更新
        context.contentResolver.openOutputStream(uri)
            ?.use { outputStream ->
                inputStream.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        //values.clear()
        //values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        //context.contentResolver.update(uri, values, null, null)
        context.showToast( "图片已保存到 $uri")
        //MediaScannerConnection.scanFile(context, arrayOf(uri.toString()), null, null)
    } else {
        context.showToast("图片保存失败: Uri为空")
    }
}

fun saveImageLegacyAPI(
    context: Context,
    inputStream: InputStream,
    subFolder: String,
    fileName: String,
) {
    val picturesDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES)
    val saveDir = File(picturesDir, subFolder)
    if (!saveDir.exists()) {
        saveDir.mkdirs()
    }
    val saveFile = File(saveDir, fileName)
    FileOutputStream(saveFile).also { outputStream ->
        inputStream.use { inputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    MediaScannerConnection.scanFile(context, arrayOf(saveFile.absolutePath), null, null)
    context.showToast( "图片已保存到 ${saveFile.absolutePath}")
}

fun saveBitmapToStorage(context: Context, bitmap: Bitmap) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "91Kuaihuo")
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
        }
    } ?: run {
        Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
    }
}

package com.jiyingcao.a51fengliu.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object StoragePermissionHelper {
    
    const val REQUEST_CODE_STORAGE_PERMISSION = 1002
    
    /**
     * 检查存储权限是否已授权
     * Android Q (API 29)及以上使用分区存储，无需权限
     * Android 6.0-9.0 (API 23-28)需要WRITE_EXTERNAL_STORAGE权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android Q及以上使用分区存储，无需权限
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0及以上需要动态权限
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 6.0以下版本权限在安装时授予
            true
        }
    }
    
    /**
     * 请求存储权限
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!hasStoragePermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            }
        }
    }
    
    /**
     * 从Fragment请求存储权限
     */
    fun requestStoragePermission(fragment: Fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            fragment.context?.let { context ->
                if (!hasStoragePermission(context)) {
                    fragment.requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_STORAGE_PERMISSION
                    )
                }
            }
        }
    }
    
    /**
     * 检查权限请求结果
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
    
    /**
     * 是否应该显示权限说明
     */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                   Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            false
        }
    }
    
    /**
     * 检查是否需要权限
     * 用于在保存图片前判断是否需要申请权限
     */
    fun needsPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
               Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }
}
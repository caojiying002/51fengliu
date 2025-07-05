package com.jiyingcao.a51fengliu.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.common.RemoteLoginActivity

object NotificationManagerHelper {
    
    private const val CHANNEL_ID_SECURITY = "security_channel"
    private const val CHANNEL_NAME_SECURITY = "账号安全"
    private const val CHANNEL_DESCRIPTION_SECURITY = "异地登录、账号安全相关通知"
    
    private const val NOTIFICATION_ID_REMOTE_LOGIN = 1001
    
    /**
     * 创建通知渠道
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val securityChannel = NotificationChannel(
                CHANNEL_ID_SECURITY,
                CHANNEL_NAME_SECURITY,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION_SECURITY
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(securityChannel)
        }
    }
    
    /**
     * 发送异地登录通知
     */
    fun sendRemoteLoginNotification(context: Context) {
        // 检查通知权限
        if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
            return
        }
        
        // 创建点击意图
        val intent = Intent(context, RemoteLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SECURITY)
            .setSmallIcon(R.drawable.ic_notification) // 需要确保这个图标存在
            .setContentTitle("账号安全提醒")
            .setContentText("检测到您的账号在其他设备登录，请确认是否为本人操作")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("检测到您的账号在其他设备登录，如非本人操作，请立即修改密码并检查账号安全。点击查看详情。")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        // 发送通知
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMOTE_LOGIN, notification)
        } catch (e: SecurityException) {
            // 权限被拒绝时的处理
            e.printStackTrace()
        }
    }
    
    /**
     * 取消异地登录通知
     */
    fun cancelRemoteLoginNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_REMOTE_LOGIN)
    }
    
    /**
     * 取消所有通知
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
package com.jiyingcao.a51fengliu

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**
 *
 * An empty implementation of [Application.ActivityLifecycleCallbacks].
 *
 * Override the methods you need.
 */
open class DefaultActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

}

/**
 * 启用EdgeToEdge，并且将WindowInsets的值设置给布局作为padding
 */
object EdgeToEdgeWindowInsetsCallbacks : DefaultActivityLifecycleCallbacks() {

    private const val ANDROID_R_ID_CONTENT = android.R.id.content

    /** 顶部Space控件ID，高度设置为系统状态栏高度 */
    private val SPACE_ID_SYSTEM_TOP_BAR_PADDING = R.id.system_top_bar_padding
    /** 底部Space控件ID，高度设置为系统导航条高度 */
    private val SPACE_ID_SYSTEM_BOTTOM_BAR_PADDING = R.id.system_bottom_bar_padding

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.apply {
            // 启用EdgeToEdge
            if (this is ComponentActivity) enableEdgeToEdge()

            // 设置状态栏图标和文字为浅色
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = true

            // 把状态栏高度设置给我们的布局作为padding
            val rootView = window.decorView.findViewById<View>(ANDROID_R_ID_CONTENT)
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                // 把[R.id.system_top_bar_padding]的高度设置为状态栏高度
                v.findViewById<View>(SPACE_ID_SYSTEM_TOP_BAR_PADDING)?.updateLayoutParams { height = systemBarsInsets.top }
                // 把[R.id.system_bottom_bar_padding]的高度设置为底部导航栏高度
                v.findViewById<View>(SPACE_ID_SYSTEM_BOTTOM_BAR_PADDING)?.updateLayoutParams { height = systemBarsInsets.bottom }

                ViewCompat.setOnApplyWindowInsetsListener(v, null)
                insets
            }
        }
    }
}
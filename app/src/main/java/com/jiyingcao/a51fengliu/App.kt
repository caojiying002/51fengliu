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

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        registerActivityLifecycleCallbacks(object : DefaultActivityLifecycleCallbacks() {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.apply {
                    // 启用EdgeToEdge
                    if (this is ComponentActivity) enableEdgeToEdge()

                    // 设置状态栏图标和文字为浅色
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    windowInsetsController.isAppearanceLightStatusBars = false

                    // 把状态栏高度设置给我们的布局作为padding
                    val rootView = window.decorView.findViewById<View>(android.R.id.content)
                    ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
                        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                        // 把[R.id.system_top_bar_padding]的高度设置为状态栏高度
                        findViewById<View>(R.id.system_top_bar_padding)?.layoutParams?.height = systemBarsInsets.top

                        ViewCompat.setOnApplyWindowInsetsListener(v, null)
                        insets
                    }
                }
            }
        })
    }

    companion object {
        @JvmStatic lateinit var INSTANCE: App
    }
}

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
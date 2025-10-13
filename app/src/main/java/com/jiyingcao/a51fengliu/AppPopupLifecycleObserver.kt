package com.jiyingcao.a51fengliu

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jiyingcao.a51fengliu.manager.AppPopupManager
import com.jiyingcao.a51fengliu.util.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * APP弹窗生命周期观察者
 * 监听应用进程的生命周期，在应用进入前台时触发弹窗检查
 */
@Singleton
class AppPopupLifecycleObserver @Inject constructor(
    private val appPopupManager: AppPopupManager
) : DefaultLifecycleObserver {

    /**
     * 当应用进程进入前台时调用
     * 触发弹窗检查和显示逻辑
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        AppLogger.d("AppPopupLifecycleObserver", "应用进入前台，开始检查弹窗")
        //appPopupManager.checkAndShowPopup()
    }
}
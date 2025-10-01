package com.jiyingcao.a51fengliu.manager

import com.jiyingcao.a51fengliu.ActivityManager
import com.jiyingcao.a51fengliu.repository.ConfigRepository
import com.jiyingcao.a51fengliu.ui.dialog.CommonDialog
import com.jiyingcao.a51fengliu.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * APP弹窗管理器
 * 负责获取和展示APP弹窗通知
 */
@Singleton
class AppPopupManager @Inject constructor(
    private val configRepository: ConfigRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * 检查并显示弹窗
     * 从服务器获取弹窗配置，如果enable为true则显示弹窗
     */
    fun checkAndShowPopup() {
        // 获取当前Activity
        val currentActivity = ActivityManager.getCurrentActivity()

        if (currentActivity == null) {
            AppLogger.d("AppPopupManager", "当前没有可用的Activity，跳过弹窗检查")
            return
        }

        if (currentActivity.isFinishing || currentActivity.isDestroyed) {
            AppLogger.d("AppPopupManager", "当前Activity正在结束或已销毁，跳过弹窗检查")
            return
        }

        // 请求API获取弹窗配置
        scope.launch {
            configRepository.getAppPopupNotice()
                .catch { exception ->
                    AppLogger.e("AppPopupManager", "获取弹窗配置失败", exception)
                }
                .collect { result ->
                    result.onSuccess { popupNotice ->
                        AppLogger.d("AppPopupManager", "获取弹窗配置成功: enable=${popupNotice.enable}, title=${popupNotice.title}")

                        // 检查是否启用弹窗
                        if (popupNotice.enable != true) {
                            AppLogger.d("AppPopupManager", "弹窗未启用，跳过显示")
                            return@onSuccess
                        }

                        // 检查内容是否为空
                        val content = popupNotice.content
                        if (content.isNullOrBlank()) {
                            AppLogger.w("AppPopupManager", "弹窗内容为空，跳过显示")
                            return@onSuccess
                        }

                        // 显示弹窗
                        showPopupDialog(
                            title = popupNotice.title,
                            content = content
                        )
                    }.onFailure { exception ->
                        AppLogger.e("AppPopupManager", "处理弹窗配置失败", exception)
                    }
                }
        }
    }

    /**
     * 显示弹窗对话框
     *
     * @param title 弹窗标题，为空时不显示标题
     * @param content 弹窗内容
     */
    private fun showPopupDialog(title: String?, content: String) {
        val currentActivity = ActivityManager.getCurrentActivity() ?: return

        if (currentActivity.isFinishing || currentActivity.isDestroyed) {
            AppLogger.w("AppPopupManager", "Activity已失效，无法显示弹窗")
            return
        }

        try {
            val dialog = if (!title.isNullOrBlank()) {
                // 有标题：使用newTitledInstance
                CommonDialog.newTitledInstance(
                    title = title,
                    message = content,
                    positiveButtonText = "确定",
                    cancelable = true
                )
            } else {
                // 无标题：使用newPromptInstance
                CommonDialog.newPromptInstance(
                    message = content,
                    buttonText = "确定",
                    cancelable = true
                )
            }

            dialog.show(currentActivity.supportFragmentManager, CommonDialog.TAG)
            AppLogger.d("AppPopupManager", "弹窗已显示")
        } catch (e: Exception) {
            AppLogger.e("AppPopupManager", "显示弹窗失败", e)
        }
    }
}
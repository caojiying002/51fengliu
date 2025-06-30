package com.jiyingcao.a51fengliu.ui

import androidx.core.view.isVisible
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.databinding.StatefulViewpager2RecyclerViewBinding

/**
 * Extension functions for the StatefulRefreshRecyclerViewBinding
 * to provide a consistent way to manage view states across the app.
 * 
 * StatefulRefreshRecyclerViewBinding的扩展函数，
 * 为整个应用提供一致的视图状态管理方式。
 */

/**
 * Shows the content view and hides loading and error views.
 * 
 * 显示内容视图并隐藏加载和错误视图。
 */
fun StatefulRefreshRecyclerViewBinding.showContentView() {
    contentLayout.isVisible = true
    errorLayout.isVisible = false
    loadingLayout.isVisible = false
}

/**
 * Shows the loading view and hides content and error views.
 * 
 * 显示加载视图并隐藏内容和错误视图。
 */
fun StatefulRefreshRecyclerViewBinding.showLoadingView() {
    contentLayout.isVisible = false
    errorLayout.isVisible = false
    loadingLayout.isVisible = true
}

/**
 * Shows the error view with default error message and hides content and loading views.
 * 
 * 显示带有默认错误消息的错误视图，并隐藏内容和加载视图。
 */
fun StatefulRefreshRecyclerViewBinding.showErrorView() {
    contentLayout.isVisible = false
    errorLayout.isVisible = true
    loadingLayout.isVisible = false
}

/**
 * Shows the error view with custom error message and optional retry action.
 * Hides content and loading views.
 *
 * 显示带有自定义错误消息和可选重试操作的错误视图。
 * 隐藏内容和加载视图。
 *
 * @param message The error message to display / 要显示的错误消息
 * @param retry Optional callback to invoke when the retry button is clicked / 点击重试按钮时调用的可选回调
 */
fun StatefulRefreshRecyclerViewBinding.showErrorView(
    message: String = "出错了，请稍后重试",
    retry: (() -> Unit)? = null
) {
    loadingLayout.isVisible = false
    contentLayout.isVisible = false
    errorLayout.isVisible = true

    tvError.text = message
    clickRetry.isVisible = retry != null
    clickRetry.setOnClickListener { retry?.invoke() }
}

/**
 * Shows the empty content view and hides the real content view.
 * Automatically calls [showContentView] to ensure the content layout is visible.
 * 
 * 显示空内容视图并隐藏实际内容视图。
 * 自动调用[showContentView]确保内容布局可见。
 */
fun StatefulRefreshRecyclerViewBinding.showEmptyContent() {
    showContentView()
    emptyContent.isVisible = true
    realContent.isVisible = false
}

/**
 * Shows the real content view and hides the empty content view.
 * Automatically calls [showContentView] to ensure the content layout is visible.
 * 
 * 显示实际内容视图并隐藏空内容视图。
 * 自动调用[showContentView]确保内容布局可见。
 */
fun StatefulRefreshRecyclerViewBinding.showRealContent() {
    showContentView()
    emptyContent.isVisible = false
    realContent.isVisible = true
}

/**
 * Extension functions for the StatefulViewpager2RecyclerViewBinding
 * to provide a consistent way to manage view states across the app.
 * 
 * StatefulViewpager2RecyclerViewBinding的扩展函数，
 * 为整个应用提供一致的视图状态管理方式。
 */

/**
 * Shows the content view and hides loading and error views.
 * 
 * 显示内容视图并隐藏加载和错误视图。
 */
fun StatefulViewpager2RecyclerViewBinding.showContentView() {
    contentLayout.isVisible = true
    errorLayout.isVisible = false
    loadingLayout.isVisible = false
}

/**
 * Shows the loading view and hides content and error views.
 * 
 * 显示加载视图并隐藏内容和错误视图。
 */
fun StatefulViewpager2RecyclerViewBinding.showLoadingView() {
    contentLayout.isVisible = false
    errorLayout.isVisible = false
    loadingLayout.isVisible = true
}

/**
 * Shows the error view with default error message and hides content and loading views.
 * 
 * 显示带有默认错误消息的错误视图，并隐藏内容和加载视图。
 */
fun StatefulViewpager2RecyclerViewBinding.showErrorView() {
    contentLayout.isVisible = false
    errorLayout.isVisible = true
    loadingLayout.isVisible = false
}

/**
 * Shows the error view with custom error message and optional retry action.
 * Hides content and loading views.
 *
 * 显示带有自定义错误消息和可选重试操作的错误视图。
 * 隐藏内容和加载视图。
 *
 * @param message The error message to display / 要显示的错误消息
 * @param retry Optional callback to invoke when the retry button is clicked / 点击重试按钮时调用的可选回调
 */
fun StatefulViewpager2RecyclerViewBinding.showErrorView(
    message: String = "出错了，请稍后重试",
    retry: (() -> Unit)? = null
) {
    loadingLayout.isVisible = false
    contentLayout.isVisible = false
    errorLayout.isVisible = true
    tvError.text = message
    clickRetry.isVisible = retry != null
    clickRetry.setOnClickListener { retry?.invoke() }
}

/**
 * Shows the empty content view and hides the real content view.
 * Automatically calls [showContentView] to ensure the content layout is visible.
 * 
 * 显示空内容视图并隐藏实际内容视图。
 * 自动调用[showContentView]确保内容布局可见。
 */
fun StatefulViewpager2RecyclerViewBinding.showEmptyContent() {
    showContentView()
    emptyContent.isVisible = true
    realContent.isVisible = false
}

/**
 * Shows the real content view and hides the empty content view.
 * Automatically calls [showContentView] to ensure the content layout is visible.
 * 
 * 显示实际内容视图并隐藏空内容视图。
 * 自动调用[showContentView]确保内容布局可见。
 */
fun StatefulViewpager2RecyclerViewBinding.showRealContent() {
    showContentView()
    emptyContent.isVisible = false
    realContent.isVisible = true
}
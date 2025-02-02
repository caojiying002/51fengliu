package com.jiyingcao.a51fengliu.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.jiyingcao.a51fengliu.R

// LoadingView的基础实现
class LoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var onCancelListener: (() -> Unit)? = null

    init {
        // 设置半透明黑色背景
        //setBackgroundColor(Color.parseColor("#66000000"))

        // 创建进度条
        val progressBar = ProgressBar(context)
        val layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        addView(progressBar, layoutParams)

        // 拦截触摸事件
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                onCancelListener?.invoke()
            }
            true
        }
    }

    fun setOnCancelListener(listener: () -> Unit) {
        onCancelListener = listener
    }
}

// ViewGroup的扩展函数
fun ViewGroup.showLoading(
    cancelable: Boolean = false,
    onCancel: (() -> Unit)? = null
): LoadingView {
    // 查找已存在的LoadingView或创建新的
    var loadingView = findViewById<LoadingView>(R.id.loading_view)

    if (loadingView == null) {
        loadingView = LoadingView(context).apply {
            id = R.id.loading_view
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )

            if (cancelable) {
                setOnCancelListener {
                    visibility = View.GONE
                    onCancel?.invoke()
                }
            }
        }
        addView(loadingView)
    }

    loadingView.visibility = View.VISIBLE
    return loadingView
}

// 扩展函数用于隐藏Loading
fun ViewGroup.hideLoading() {
    findViewById<LoadingView>(R.id.loading_view)?.visibility = View.GONE
}
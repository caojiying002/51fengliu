package com.jiyingcao.a51fengliu.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.jiyingcao.a51fengliu.R

class StatefulLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    enum class State { LOADING, CONTENT, ERROR, EMPTY }

    private var loadingView: View
    private var contentView: View
    private var errorView: View
    private var emptyView: View

    var currentState: State = State.CONTENT
        set(value) {
            field = value
            updateViewVisibility(value)
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StatefulLayout,
            0, 0).apply {
            try {
                @LayoutRes val loadingLayoutId: Int = getResourceId(R.styleable.StatefulLayout_loadingLayout, R.layout.stateful_default_loading)
                @LayoutRes val contentLayoutId = getResourceId(R.styleable.StatefulLayout_contentLayout, R.layout.stateful_default_content)
                @LayoutRes val errorLayoutId = getResourceId(R.styleable.StatefulLayout_errorLayout, R.layout.stateful_default_error)
                @LayoutRes val emptyLayoutId = getResourceId(R.styleable.StatefulLayout_emptyLayout, R.layout.stateful_default_empty)

                loadingView = LayoutInflater.from(context).inflate(loadingLayoutId, this@StatefulLayout, false)
                contentView = LayoutInflater.from(context).inflate(contentLayoutId, this@StatefulLayout, false)
                errorView = LayoutInflater.from(context).inflate(errorLayoutId, this@StatefulLayout, false)
                emptyView = LayoutInflater.from(context).inflate(emptyLayoutId, this@StatefulLayout, false)

                addView(loadingView)
                addView(contentView)
                addView(errorView)
                addView(emptyView)
                updateViewVisibility(currentState) // 显示默认视图
            } finally {
                recycle()
            }
        }
    }

    fun updateViewVisibility(state: State) {
        loadingView.visibility = if (state == State.LOADING) View.VISIBLE else View.GONE
        contentView.visibility = if (state == State.CONTENT) View.VISIBLE else View.GONE
        errorView.visibility = if (state == State.ERROR) View.VISIBLE else View.GONE
        emptyView.visibility = if (state == State.EMPTY) View.VISIBLE else View.GONE
    }

    // ---------- 以下为自定义布局方法 ----------

    fun getLoadingView() = loadingView

    fun setLoadingView(layoutResId: Int) {
        setLoadingView(LayoutInflater.from(context).inflate(layoutResId, this, false))
    }

    fun setLoadingView(view: View) {
        removeView(loadingView)
        loadingView = view
        addView(loadingView)
    }

    fun getContentView() = contentView

    fun setContentView(layoutResId: Int) {
        setContentView(LayoutInflater.from(context).inflate(layoutResId, this, false))
    }

    fun setContentView(view: View) {
        removeView(contentView)
        contentView = view
        addView(contentView)
    }

    fun getErrorView() = errorView

    fun setErrorView(layoutResId: Int) {
        setErrorView(LayoutInflater.from(context).inflate(layoutResId, this, false))
    }

    fun setErrorView(view: View) {
        removeView(errorView)
        errorView = view
        addView(errorView)
    }

    fun getEmptyView() = emptyView

    fun setEmptyView(layoutResId: Int) {
        setEmptyView(LayoutInflater.from(context).inflate(layoutResId, this, false))
    }

    fun setEmptyView(view: View) {
        removeView(emptyView)
        emptyView = view
        addView(emptyView)
    }
}

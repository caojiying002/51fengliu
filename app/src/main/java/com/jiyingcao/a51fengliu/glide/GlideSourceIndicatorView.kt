package com.jiyingcao.a51fengliu.glide

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * 自定义视图，包装ImageView并显示Glide加载来源指示器
 */
class GlideSourceIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView
    private val sourceIndicator: TextView

    // 定义指示器颜色
    private val networkColor = Color.RED
    private val cacheColor = Color.GREEN

    init {
        // 创建内部ImageView
        imageView = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        addView(imageView)

        // 创建指示器文本视图
        sourceIndicator = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, 8, 8, 0)
            }
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(4, 2, 4, 2)
            visibility = GONE // 默认隐藏
        }
        addView(sourceIndicator)
    }

    /**
     * 使用Glide加载图片，并显示来源指示器
     */
    fun loadImage(url: String, showSourceIndicator: Boolean = true) {
        if (!showSourceIndicator) {
            sourceIndicator.visibility = GONE
            GlideApp.with(context).load(url).into(imageView)
            return
        }

        GlideApp.with(context)
            .load(url)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    sourceIndicator.visibility = GONE
                    return false
                }

                @SuppressLint("SetTextI18n")
                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    when (dataSource) {
                        DataSource.REMOTE -> {
                            sourceIndicator.apply {
                                text = "NET"
                                setBackgroundColor(networkColor)
                                visibility = VISIBLE
                            }
                        }
                        DataSource.DATA_DISK_CACHE, DataSource.MEMORY_CACHE -> {
                            sourceIndicator.apply {
                                text = "CACHE"
                                setBackgroundColor(cacheColor)
                                visibility = VISIBLE
                            }
                        }
                        else -> {
                            sourceIndicator.apply {
                                text = "OTHER"
                                setBackgroundColor(Color.GRAY)
                                visibility = VISIBLE
                            }
                        }
                    }
                    return false
                }
            })
            .into(imageView)
    }

    /**
     * 获取内部ImageView，以便进行更多自定义设置
     */
    fun getInnerImageView(): ImageView = imageView
}
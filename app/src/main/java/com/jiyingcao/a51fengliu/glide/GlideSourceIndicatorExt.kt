package com.jiyingcao.a51fengliu.glide

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.ViewTreeObserver
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * Extension function for RequestBuilder to add source indicator to the ImageView
 * This allows maintaining the existing Glide call chain pattern
 */
fun <T> RequestBuilder<T>.withSourceIndicator(imageView: ImageView): RequestBuilder<T> {
    // Clear any existing overlay
    imageView.overlay.clear()

    return this.addListener(object : RequestListener<T> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<T?>,
            isFirstResource: Boolean
        ): Boolean {
            // Clear indicator on failure
            imageView.overlay.clear()
            return false // Return false to allow Glide to handle the failure
        }

        override fun onResourceReady(
            resource: T & Any,
            model: Any,
            target: Target<T?>?,
            dataSource: DataSource,
            isFirstResource: Boolean
        ): Boolean {
            // Add source indicator based on data source
            imageView.addSourceIndicator(dataSource)
            return false // Return false to allow Glide to display the resource
        }
    })
}

/**
 * Add source indicator with proper bounds adjustment
 */
private fun ImageView.addSourceIndicator(dataSource: DataSource?) {
    // Create the indicator
    val indicator = SourceIndicatorDrawable(context, dataSource)

    // If view is already laid out, set bounds immediately
    if (width > 0 && height > 0) {
        indicator.setBounds(0, 0, width, height)
        overlay.add(indicator)
    } else {
        // Otherwise, wait for layout
        viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (width > 0 && height > 0) {
                    viewTreeObserver.removeOnPreDrawListener(this)
                    indicator.setBounds(0, 0, width, height)
                    overlay.add(indicator)
                }
                return true
            }
        })
    }
}

/**
 * Custom drawable for the source indicator
 */
private class SourceIndicatorDrawable(
    context: Context,
    private val dataSource: DataSource?
) : Drawable() {

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.displayMetrics.density * 12 // 12sp
        color = Color.WHITE
    }
    private val bgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val textBounds = Rect()
    private val text: String
    private val bgColor: Int
    private val density = context.resources.displayMetrics.density

    init {
        // Determine text and color based on data source
        text = when (dataSource) {
            DataSource.REMOTE -> "NET"
            DataSource.DATA_DISK_CACHE -> "DISK"
            DataSource.MEMORY_CACHE -> "MEM"
            DataSource.LOCAL -> "LOCAL"
            DataSource.RESOURCE_DISK_CACHE -> "RES"
            else -> "?"
        }

        bgColor = when (dataSource) {
            DataSource.REMOTE -> Color.parseColor("#FF5722") // Orange for network
            DataSource.DATA_DISK_CACHE -> Color.parseColor("#4CAF50") // Green for disk cache
            DataSource.MEMORY_CACHE -> Color.parseColor("#2196F3") // Blue for memory cache
            DataSource.LOCAL, DataSource.RESOURCE_DISK_CACHE -> Color.parseColor("#9C27B0") // Purple for local/resource
            else -> Color.parseColor("#9E9E9E") // Gray for unknown
        }
    }

    override fun draw(canvas: Canvas) {
        // Calculate text size
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        // Add padding
        val padding = (4 * density).toInt()
        val width = textBounds.width() + padding * 2
        val height = textBounds.height() + padding * 2

        // Draw at top-right corner
        val left = bounds.right - width - padding
        val top = bounds.top + padding

        // Set background color
        bgPaint.color = bgColor

        // Draw background with rounded corners
        val cornerRadius = 4 * density
        val rect = RectF(
            left.toFloat(), top.toFloat(),
            (left + width).toFloat(), (top + height).toFloat()
        )
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // Draw text - adjust baseline to align text properly
        val textY = top + (height + textBounds.height()) / 2f - textBounds.bottom
        canvas.drawText(text, left + padding.toFloat(), textY, textPaint)
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
        bgPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        textPaint.colorFilter = colorFilter
        bgPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
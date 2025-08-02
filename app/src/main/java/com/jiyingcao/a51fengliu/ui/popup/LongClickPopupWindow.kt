package com.jiyingcao.a51fengliu.ui.popup

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.view.isVisible
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.util.dp

/**
 * 微信/QQ样式的长按弹窗
 * 特性：
 * - 深色背景，白色文字和图标
 * - 圆角矩形主体 + 三角形指示器
 * - 智能定位：优先显示在target上方，空间不足时显示在下方
 * - 三角形指示器自动调整方向和位置
 */
class LongClickPopupWindow private constructor(
    private val context: Context,
    private val contentView: View
) : PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    companion object {
        /**
         * 创建只有复制功能的弹窗
         */
        fun createCopyPopup(context: Context, onCopyClick: () -> Unit): LongClickPopupWindow {
            val inflater = LayoutInflater.from(context)
            val contentView = inflater.inflate(R.layout.layout_popup_menu, null)
            
            val popup = LongClickPopupWindow(context, contentView)
            
            // 设置复制点击事件
            contentView.findViewById<LinearLayout>(R.id.copyOption).setOnClickListener {
                onCopyClick()
                popup.dismiss()
            }
            
            return popup
        }
    }

    init {
        // 基础配置
        isFocusable = true
        isOutsideTouchable = true
        
        // 设置背景为透明，避免默认背景影响
        setBackgroundDrawable(null)
        
        // 设置动画（系统默认的弹窗动画）
        animationStyle = android.R.style.Animation_Dialog
    }

    /**
     * 在指定View的上方或下方显示弹窗
     * @param targetView 目标View
     * @param preferTop 是否优先显示在上方，默认true
     */
    fun showAroundView(targetView: View, preferTop: Boolean = true) {
        // 获取targetView在屏幕中的位置
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val targetX = location[0]
        val targetY = location[1]
        val targetWidth = targetView.width
        val targetHeight = targetView.height
        
        // 测量弹窗尺寸
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = contentView.measuredWidth
        val popupHeight = contentView.measuredHeight
        
        // 获取屏幕尺寸
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // 计算弹窗位置
        val position = calculateOptimalPosition(
            targetX, targetY, targetWidth, targetHeight,
            popupWidth, popupHeight,
            screenWidth, screenHeight,
            preferTop
        )
        
        // 设置三角形指示器
        setupTriangleIndicator(position.showAbove, targetX, targetWidth, position.x, popupWidth)
        
        // 显示弹窗
        showAtLocation(targetView, Gravity.NO_GRAVITY, position.x, position.y)
    }

    /**
     * 计算最佳显示位置
     */
    private fun calculateOptimalPosition(
        targetX: Int, targetY: Int, targetWidth: Int, targetHeight: Int,
        popupWidth: Int, popupHeight: Int,
        screenWidth: Int, screenHeight: Int,
        preferTop: Boolean
    ): PopupPosition {
        val margin = 8.dp // 弹窗与target的间距
        val screenMargin = 16.dp // 弹窗与屏幕边缘的最小间距
        
        // 计算水平位置（居中对齐，但要考虑屏幕边界）
        val idealX = targetX + (targetWidth - popupWidth) / 2
        val minX = screenMargin
        val maxX = screenWidth - popupWidth - screenMargin
        val finalX = idealX.coerceIn(minX, maxX)
        
        // 计算垂直位置
        val topY = targetY - popupHeight - margin
        val bottomY = targetY + targetHeight + margin
        
        val showAbove = if (preferTop) {
            // 优先显示在上方，但要检查空间是否足够
            topY >= screenMargin
        } else {
            // 优先显示在下方，但要检查空间是否足够
            bottomY + popupHeight <= screenHeight - screenMargin
        }
        
        val finalY = if (showAbove) topY else bottomY
        
        return PopupPosition(finalX, finalY, showAbove)
    }

    /**
     * 设置三角形指示器的显示状态和位置
     */
    private fun setupTriangleIndicator(showAbove: Boolean, targetX: Int, targetWidth: Int, popupX: Int, popupWidth: Int) {
        val triangleTop = contentView.findViewById<ImageView>(R.id.triangleIndicatorTop)
        val triangleBottom = contentView.findViewById<ImageView>(R.id.triangleIndicatorBottom)
        
        if (showAbove) {
            // 弹窗在target上方，显示底部的倒三角
            triangleTop.isVisible = false
            triangleBottom.isVisible = true
            
            // 计算三角形的偏移位置，使其指向target中心
            adjustTrianglePosition(triangleBottom, targetX, targetWidth, popupX, popupWidth)
        } else {
            // 弹窗在target下方，显示顶部的正三角
            triangleTop.isVisible = true
            triangleBottom.isVisible = false
            
            // 计算三角形的偏移位置，使其指向target中心
            adjustTrianglePosition(triangleTop, targetX, targetWidth, popupX, popupWidth)
        }
    }

    /**
     * 调整三角形指示器的水平位置，使其指向target中心
     */
    private fun adjustTrianglePosition(triangle: ImageView, targetX: Int, targetWidth: Int, popupX: Int, popupWidth: Int) {
        val targetCenterX = targetX + targetWidth / 2
        val popupCenterX = popupX + popupWidth / 2
        val offset = targetCenterX - popupCenterX
        
        // 限制偏移范围，确保三角形不会超出弹窗边界
        val maxOffset = (popupWidth - triangle.layoutParams.width) / 2 - 8.dp
        val finalOffset = offset.coerceIn(-maxOffset, maxOffset)
        
        // 设置偏移
        triangle.translationX = finalOffset.toFloat()
    }

    /**
     * 弹窗位置信息
     */
    private data class PopupPosition(
        val x: Int,
        val y: Int,
        val showAbove: Boolean
    )
}
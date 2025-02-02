package com.jiyingcao.a51fengliu.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * A FrameLayout that can be used as a mask layer to block touch events.
 * Similar to a Dialog or PopupWindow's background dimming effect.
 */
class MaskLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Callback interface for touch events
     */
    interface OnMaskTouchListener {
        /**
         * Called when the mask is touched
         * @return true if the touch event is consumed, false otherwise
         */
        fun onMaskTouch(): Boolean
    }

    private var maskTouchListener: OnMaskTouchListener? = null

    /**
     * Set whether to allow touch events to pass through
     */
    var touchable: Boolean = true

    init {
        // Set default background color (semi-transparent black)
        // 这里不使用遮罩颜色，作为透明的遮罩层
        //setBackgroundColor(0x80000000.toInt())

        // Ensure the layout is clickable to receive touch events
        isClickable = true
        isFocusable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!touchable) {
            return false
        }

        // Only handle ACTION_DOWN events
        if (event.action == MotionEvent.ACTION_DOWN) {
            return maskTouchListener?.onMaskTouch() ?: true
        }

        return super.onTouchEvent(event)
    }

    /**
     * Set touch event callback
     */
    fun setOnMaskTouchListener(listener: OnMaskTouchListener) {
        maskTouchListener = listener
    }

    /**
     * Remove touch event callback
     */
    fun removeOnMaskTouchListener() {
        maskTouchListener = null
    }
}
package com.jiyingcao.a51fengliu.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout

/**
 * 自定义FrameLayout，用于自动隐藏软键盘并清除EditText焦点
 * 当用户点击EditText以外的区域时（包括其他可点击控件），都会触发此行为
 */
class KeyboardDismissFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 需要监听的EditText列表
    private val watchedEditTexts = mutableListOf<EditText>()

    /**
     * 添加需要监听的EditText
     */
    fun addWatchedEditText(editText: EditText) {
        if (!watchedEditTexts.contains(editText)) {
            watchedEditTexts.add(editText)
        }
    }

    /**
     * 添加多个需要监听的EditText
     */
    fun addWatchedEditTexts(editTexts: List<EditText>) {
        for (editText in editTexts) {
            addWatchedEditText(editText)
        }
    }

    /**
     * 移除监听的EditText
     */
    fun removeWatchedEditText(editText: EditText) {
        watchedEditTexts.remove(editText)
    }

    /**
     * 清除所有监听的EditText
     */
    fun clearWatchedEditTexts() {
        watchedEditTexts.clear()
    }

    /**
     * 设置单个要监听的EditText
     * 此方法会清除之前添加的所有EditText
     */
    fun setWatchedEditText(editText: EditText) {
        watchedEditTexts.clear()
        watchedEditTexts.add(editText)
    }

    /**
     * 设置多个要监听的EditText
     * 此方法会清除之前添加的所有EditText
     */
    fun setWatchedEditTexts(editTexts: List<EditText>) {
        watchedEditTexts.clear()
        watchedEditTexts.addAll(editTexts)
    }

    /**
     * 查找布局中所有的EditText并自动添加到监听列表
     */
    fun watchAllEditTextsInLayout() {
        // 清除之前的列表
        watchedEditTexts.clear()

        // 递归查找所有EditText
        findEditTexts(this)
    }

    /**
     * 递归查找视图层次结构中的所有EditText
     */
    private fun findEditTexts(view: View) {
        if (view is EditText) {
            watchedEditTexts.add(view)
        } else if (view is FrameLayout) {
            for (i in 0 until view.childCount) {
                findEditTexts(view.getChildAt(i))
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // 获取当前获得焦点的EditText
            val focusedEditTexts = watchedEditTexts.filter { it.isFocused }

            if (focusedEditTexts.isNotEmpty()) {
                // 检查点击是否在所有获焦的EditText之外
                val touchX = ev.rawX.toInt()
                val touchY = ev.rawY.toInt()

                val clickedOutsideAll = focusedEditTexts.all { editText ->
                    val rect = Rect()
                    editText.getGlobalVisibleRect(rect)
                    !rect.contains(touchX, touchY)
                }

                if (clickedOutsideAll) {
                    // 隐藏键盘
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                    // 对每个获焦的EditText进行处理
                    for (editText in focusedEditTexts) {
                        // 隐藏键盘
                        imm.hideSoftInputFromWindow(editText.windowToken, 0)
                        // 清除焦点
                        editText.clearFocus()
                    }
                }
            }
        }

        // 继续正常的事件分发流程
        return super.dispatchTouchEvent(ev)
    }
}
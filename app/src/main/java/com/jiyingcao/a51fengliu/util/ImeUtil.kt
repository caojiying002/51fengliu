package com.jiyingcao.a51fengliu.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService

/**
 * 软键盘管理工具类
 */
object ImeUtil {
    /**
     * 显示软键盘
     */
    fun showIme(view: View) {
        view.requestFocus()
        val imm = view.context.getSystemService<InputMethodManager>()
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * 隐藏软键盘
     */
    fun hideIme(view: View) {
        val imm = view.context.getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * 切换软键盘显示状态
     */
    fun toggleIme(view: View) {
        val imm = view.context.getSystemService<InputMethodManager>()
        imm?.toggleSoftInput(0, 0)
    }

    /**
     * 判断软键盘是否显示
     */
    fun isImeShowing(view: View): Boolean {
        val imm = view.context.getSystemService<InputMethodManager>()
        return imm?.isActive(view) == true
    }

    /**
     * 强制显示软键盘
     */
    fun forceShowIme(editText: EditText) {
        editText.requestFocus()
        val imm = editText.context.getSystemService<InputMethodManager>()
        imm?.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
    }

    /**
     * 强制隐藏软键盘
     */
    fun forceHideIme(editText: EditText) {
        val imm = editText.context.getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(editText.windowToken, 0)
    }
}
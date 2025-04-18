package com.jiyingcao.a51fengliu.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.TextView
import com.jiyingcao.a51fengliu.R

/**
 * 确认对话框辅助类，提供类似微信风格的"标题+取消+确认"对话框
 */
class ConfirmDialogHelper(private val context: Context) {
    private var title: String = ""
    private var cancelButtonText: String = "取消"
    private var confirmButtonText: String = "确认"
    private var onCancelListener: (() -> Unit)? = null
    private var onConfirmListener: (() -> Unit)? = null
    private var dialog: Dialog? = null
    private var canceledOnTouchOutside: Boolean = false
    private var cancelable: Boolean = true

    /**
     * 设置对话框标题
     */
    fun setTitle(title: String): ConfirmDialogHelper {
        this.title = title
        return this
    }

    /**
     * 设置取消按钮文本
     */
    fun setCancelButtonText(text: String): ConfirmDialogHelper {
        this.cancelButtonText = text
        return this
    }

    /**
     * 设置确认按钮文本
     */
    fun setConfirmButtonText(text: String): ConfirmDialogHelper {
        this.confirmButtonText = text
        return this
    }

    /**
     * 设置点击外部是否可取消
     */
    fun setCanceledOnTouchOutside(cancelable: Boolean): ConfirmDialogHelper {
        this.canceledOnTouchOutside = cancelable
        return this
    }

    /**
     * 设置是否可以通过返回键取消对话框
     * 如果设置为false，用户按返回键将无法关闭对话框
     */
    fun setCancelable(cancelable: Boolean): ConfirmDialogHelper {
        this.cancelable = cancelable
        return this
    }

    /**
     * 设置取消按钮点击监听器
     */
    fun setOnCancelListener(listener: (() -> Unit)?): ConfirmDialogHelper {
        onCancelListener = listener
        return this
    }

    /**
     * 设置确认按钮点击监听器
     */
    fun setOnConfirmListener(listener: (() -> Unit)?): ConfirmDialogHelper {
        onConfirmListener = listener
        return this
    }

    /**
     * 显示对话框
     */
    fun show() {
        if (dialog?.isShowing == true) {
            return
        }

        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setContentView(R.layout.dialog_confirm)
            setCancelable(cancelable)
            setCanceledOnTouchOutside(canceledOnTouchOutside)

            // 设置标题
            findViewById<TextView>(R.id.tv_dialog_title)?.text = title

            // 设置按钮文本
            findViewById<TextView>(R.id.btn_cancel)?.text = cancelButtonText
            findViewById<TextView>(R.id.btn_confirm)?.text = confirmButtonText

            // 设置按钮点击监听器
            findViewById<TextView>(R.id.btn_cancel)?.setOnClickListener {
                onCancelListener?.invoke()
                dismiss()
            }

            findViewById<TextView>(R.id.btn_confirm)?.setOnClickListener {
                onConfirmListener?.invoke()
                dismiss()
            }

            setOnCancelListener {
                onCancelListener?.invoke()
            }

            show()
        }
    }

    /**
     * 关闭对话框
     */
    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    /**
     * 对话框是否正在显示
     */
    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }

    companion object {
        /**
         * 创建一个确认对话框
         */
        fun create(context: Context): ConfirmDialogHelper {
            return ConfirmDialogHelper(context)
        }
    }
}
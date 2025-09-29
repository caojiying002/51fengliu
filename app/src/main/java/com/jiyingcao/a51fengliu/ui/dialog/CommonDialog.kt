package com.jiyingcao.a51fengliu.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jiyingcao.a51fengliu.databinding.DialogCommonBinding

/**
 * 通用对话框
 * 支持可选标题、必选消息和单/双按钮配置
 * 可以替代ConfirmDialog和VipPromptDialog的功能
 */
class CommonDialog : DialogFragment() {
    private var _binding: DialogCommonBinding? = null
    private val binding get() = _binding!!

    // 标记对话框是否可取消
    private var cancellable = true

    // 对话框参数
    private var title: String = ""
    private var message: String = ""
    private var positiveButtonText: String = ""
    private var negativeButtonText: String = ""

    // 回调接口
    private var listener: OnCommonDialogListener? = null

    // 使用LifecycleObserver来管理返回手势拦截
    private lateinit var backPressManager: BackPressManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 从参数中获取设置
        arguments?.let {
            cancellable = it.getBoolean(ARG_CANCELABLE, true)
            title = it.getString(ARG_TITLE, "")
            message = it.getString(ARG_MESSAGE, "")
            positiveButtonText = it.getString(ARG_POSITIVE_TEXT, "")
            negativeButtonText = it.getString(ARG_NEGATIVE_TEXT, "")
        }

        // 设置dialog层面的可取消性
        isCancelable = cancellable

        // 初始化返回按键管理器
        backPressManager = BackPressManager(!cancellable, requireActivity())
        lifecycle.addObserver(backPressManager)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置标题（可选）
        if (title.isNotEmpty()) {
            // 设置并显示标题文本，使用Group是为了包含tv_title与tv_message之间的间距
            binding.tvTitle.text = title
            binding.titleGroup.isVisible = true
        } else {
            // 隐藏标题Group
            binding.titleGroup.isVisible = false
        }

        // 设置消息文本
        binding.tvMessage.text = message

        // 设置按钮文本和可见性
        binding.btnPositive.text = positiveButtonText

        if (negativeButtonText.isNotEmpty()) {
            binding.btnNegative.apply {
                text = negativeButtonText
                isVisible = true
            }
        } else {
            binding.btnNegative.isVisible = false
        }

        // 设置按钮点击事件
        binding.btnPositive.setOnClickListener {
            listener?.onPositive()
            dismiss()
        }

        binding.btnNegative.setOnClickListener {
            listener?.onNegative()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // 设置对话框宽度为屏幕宽度的85%
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            // 请求一个没有标题的窗口
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            // 设置是否可取消
            setCanceledOnTouchOutside(cancellable)
            setCancelable(cancellable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 设置对话框回调监听器
     */
    fun setOnCommonDialogListener(listener: OnCommonDialogListener): CommonDialog {
        this.listener = listener
        return this
    }

    companion object {
        const val TAG = "CommonDialog"
        private const val ARG_CANCELABLE = "arg_cancelable"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_POSITIVE_TEXT = "arg_positive_text"
        private const val ARG_NEGATIVE_TEXT = "arg_negative_text"

        /**
         * 创建通用对话框实例
         *
         * @param title 对话框标题，空字符串时不显示
         * @param message 对话框消息内容
         * @param positiveButtonText 确认按钮文本
         * @param negativeButtonText 取消按钮文本，空字符串时不显示
         * @param cancelable 是否允许通过返回键或者手势取消对话框，默认为true
         * @return CommonDialog实例
         */
        fun newInstance(
            title: String = "",
            message: String,
            positiveButtonText: String,
            negativeButtonText: String = "",
            cancelable: Boolean = true
        ): CommonDialog {
            return CommonDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_POSITIVE_TEXT, positiveButtonText)
                    putString(ARG_NEGATIVE_TEXT, negativeButtonText)
                    putBoolean(ARG_CANCELABLE, cancelable)
                }
            }
        }

        /**
         * 创建确认对话框（双按钮）
         * 兼容ConfirmDialog的使用方式
         */
        fun newConfirmInstance(
            message: String,
            positiveButtonText: String = "确定",
            negativeButtonText: String = "取消",
            cancelable: Boolean = true
        ): CommonDialog {
            return newInstance(
                title = "",
                message = message,
                positiveButtonText = positiveButtonText,
                negativeButtonText = negativeButtonText,
                cancelable = cancelable
            )
        }

        /**
         * 创建提示对话框（单按钮）
         * 兼容VipPromptDialog的使用方式
         */
        fun newPromptInstance(
            message: String,
            buttonText: String = "确定",
            cancelable: Boolean = false
        ): CommonDialog {
            return newInstance(
                title = "",
                message = message,
                positiveButtonText = buttonText,
                negativeButtonText = "",
                cancelable = cancelable
            )
        }

        /**
         * 创建带标题的对话框
         */
        fun newTitledInstance(
            title: String,
            message: String,
            positiveButtonText: String = "确定",
            negativeButtonText: String = "",
            cancelable: Boolean = true
        ): CommonDialog {
            return newInstance(
                title = title,
                message = message,
                positiveButtonText = positiveButtonText,
                negativeButtonText = negativeButtonText,
                cancelable = cancelable
            )
        }
    }

    /**
     * 管理返回按键拦截的生命周期观察者
     */
    private class BackPressManager(
        private val interceptBackPress: Boolean,
        private val activity: FragmentActivity
    ) : DefaultLifecycleObserver {

        private var backPressInterceptor: OnBackPressedCallback? = null

        override fun onCreate(owner: LifecycleOwner) {
            if (interceptBackPress) {
                backPressInterceptor = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // 什么都不做，从而阻止返回操作
                    }
                }
            }
        }

        override fun onResume(owner: LifecycleOwner) {
            backPressInterceptor?.let {
                activity.onBackPressedDispatcher.addCallback(owner, it)
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            backPressInterceptor?.remove()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            backPressInterceptor = null
        }
    }

    /**
     * 通用对话框回调接口
     */
    interface OnCommonDialogListener {
        fun onPositive()
        fun onNegative() = Unit // 可选实现，默认为空
    }
}
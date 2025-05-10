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
import androidx.fragment.app.DialogFragment
import com.jiyingcao.a51fengliu.databinding.DialogVipPromptBinding

/**
 * 显示VIP提示的对话框
 * 告知用户需要VIP会员或使用积分购买才能查看大图
 */
class VipPromptDialog : DialogFragment() {
    private var _binding: DialogVipPromptBinding? = null
    private val binding get() = _binding!!
    
    // 添加返回手势拦截器
    private var backPressInterceptor: OnBackPressedCallback? = null
    
    // 标记对话框是否可取消
    private var cancellable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 从参数中获取是否可取消的设置
        arguments?.let {
            cancellable = it.getBoolean(ARG_CANCELABLE, false)
        }
        
        // 设置dialog层面的可取消性
        isCancelable = cancellable
        
        // 如果不允许取消，创建一个拦截返回事件的回调
        if (!cancellable) {
            backPressInterceptor = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 什么都不做，从而阻止返回操作
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVipPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.clickConfirm.setOnClickListener {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        // 从Activity层面拦截返回侧滑手势
        backPressInterceptor?.let {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it)
        }
    }
    
    override fun onPause() {
        super.onPause()
        backPressInterceptor?.remove()
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
            // 设置不可取消（点击对话框外部或返回键不会关闭）
            setCanceledOnTouchOutside(cancellable)
            setCancelable(cancellable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressInterceptor = null
    }

    companion object {
        const val TAG = "VipPromptDialog"
        private const val ARG_CANCELABLE = "arg_cancelable"
        
        /**
         * 创建VIP提示对话框实例
         * 
         * @param cancelable 是否允许通过返回键或者手势取消对话框，默认为false
         * @return VipPromptDialog实例
         */
        fun newInstance(cancelable: Boolean = false): VipPromptDialog {
            return VipPromptDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_CANCELABLE, cancelable)
                }
            }
        }
    }
}
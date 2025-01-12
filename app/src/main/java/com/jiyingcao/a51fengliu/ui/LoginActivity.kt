package com.jiyingcao.a51fengliu.ui

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.databinding.ActivityLoginBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity


class LoginActivity: BaseActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTextViewSpans()
    }

    /**
     * 设置"立即注册"和"点此找回"两个文本的点击事件
     */
    private fun setupTextViewSpans() {
        setupClickableText(
            textView = binding.tvRegisterTip,
            fullText = "您还不是51风流会员，立即注册",
            clickablePart = "立即注册"
        ) {
            android.util.Log.d("LoginActivity", "跳转到注册页面")
        }

        setupClickableText(
            textView = binding.tvForgetPasswordTip,
            fullText = "忘记密码，点此找回",
            clickablePart = "点此找回"
        ) {
            android.util.Log.d("LoginActivity", "跳转到找回密码页面")
        }
    }

    /**
     * 为 TextView 设置可点击的高亮文本
     * @param textView 目标TextView
     * @param fullText 完整文本
     * @param clickablePart 需要高亮和点击的部分文本
     * @param onClick 点击事件处理
     */
    private fun setupClickableText(
        textView: TextView,
        fullText: String,
        clickablePart: String,
        onClick: () -> Unit
    ) {
        val spannableString = SpannableString(fullText)
        val start = fullText.indexOf(clickablePart)
        val end = start + clickablePart.length

        // 设置颜色和点击
        spannableString.apply {
            setSpan(
                ForegroundColorSpan(textView.context.getColor(R.color.text_strong)),
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onClick()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                    }
                },
                start,
                end,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.apply {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()
        }
    }
}
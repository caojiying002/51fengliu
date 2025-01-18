package com.jiyingcao.a51fengliu.ui

import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.databinding.ActivityLoginBinding
import com.jiyingcao.a51fengliu.repository.UserRepository
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.LoginEffect
import com.jiyingcao.a51fengliu.viewmodel.LoginErrorType
import com.jiyingcao.a51fengliu.viewmodel.LoginIntent
import com.jiyingcao.a51fengliu.viewmodel.LoginState
import com.jiyingcao.a51fengliu.viewmodel.LoginViewModel
import com.jiyingcao.a51fengliu.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch

class LoginActivity: BaseActivity() {
    private lateinit var binding: ActivityLoginBinding

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupTextViewSpans()
        setupViewModel()
        setupFlowCollectors()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            LoginViewModelFactory(
                UserRepository.getInstance(RetrofitClient.apiService)
            )
        )[LoginViewModel::class.java]
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is LoginState.Init -> {
                            clearErrorMessage()
                        }
                        is LoginState.Success -> {
                            clearErrorMessage()
                        }
                        is LoginState.Error -> {
                            showError(state.errorType, state.code)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is LoginEffect.ShowLoadingDialog -> showLoadingDialog()
                        is LoginEffect.DismissLoadingDialog -> dismissLoadingDialog()
                        is LoginEffect.ShowToast -> {
                            showToast(effect.message)
                        }
                        is LoginEffect.NavigateToMain -> {
                            navigateToMainActivity()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            viewModel.processIntent(LoginIntent.Login(username, password))
        }
    }

    private fun showError(errorType: LoginErrorType, code: Int) {
        clearErrorMessage()
        when (errorType) {
            is LoginErrorType.NamePassword -> {
                binding.tilUsername.error = errorType.name
                binding.tilPassword.error = errorType.password
            }
            is LoginErrorType.UnknownError -> {
                showToast(errorType.message)
            }
        }
    }

    private fun clearErrorMessage() {
        binding.tilUsername.error = null
        binding.tilPassword.error = null
    }

    private fun navigateToMainActivity() {
        lifecycleScope.launch {
            val savedToken = TokenManager.getInstance().getToken()
            showToast("登录成功，Token已保存: $savedToken")
        }
    }
    private fun showLoadingDialog() {}
    private fun dismissLoadingDialog() {}

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
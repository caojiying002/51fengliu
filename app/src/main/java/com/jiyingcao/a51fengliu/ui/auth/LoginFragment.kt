package com.jiyingcao.a51fengliu.ui.auth

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.databinding.FragmentLoginBinding
import com.jiyingcao.a51fengliu.ui.base.BaseFragment
import com.jiyingcao.a51fengliu.util.ImeUtil
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.LoginEffect
import com.jiyingcao.a51fengliu.viewmodel.LoginErrorType
import com.jiyingcao.a51fengliu.viewmodel.LoginIntent
import com.jiyingcao.a51fengliu.viewmodel.LoginState
import com.jiyingcao.a51fengliu.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : BaseFragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupTextViewSpans()
        setupFlowCollectors()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFlowCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is LoginState.Init -> {
                            clearErrorMessage()
                            enableLoginButton()
                        }
                        is LoginState.Loading -> {
                            disableLoginButton()
                        }
                        is LoginState.Success -> {
                            clearErrorMessage()
                            enableLoginButton()
                        }
                        is LoginState.Error -> {
                            showError(state.errorType, state.code)
                            enableLoginButton()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is LoginEffect.ShowToast -> {
                            requireContext().showToast(effect.message)
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
        binding.titleBar.titleBarBack.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnLogin.setOnClickListener { v ->
            ImeUtil.hideIme(v)

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
                requireContext().showToast(errorType.message)
            }
        }
    }

    private fun clearErrorMessage() {
        binding.tilUsername.error = null
        binding.tilPassword.error = null
    }

    // 在登录成功处理逻辑中
    private fun handleLoginSuccess() {
        requireActivity().apply {
            // 设置返回结果
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun navigateToMainActivity() {
        requireActivity().apply {
            showToast("登录成功")
            // 设置返回结果
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun enableLoginButton() {
        binding.btnLogin.isEnabled = true
    }

    private fun disableLoginButton() {
        binding.btnLogin.isEnabled = false
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
            findNavController().navigate(R.id.action_login_to_register)
        }

        setupClickableText(
            textView = binding.tvForgetPasswordTip,
            fullText = "忘记密码，点此找回",
            clickablePart = "点此找回"
        ) {
            findNavController().navigate(R.id.action_login_to_forgot_password)
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
                ForegroundColorSpan(textView.context.getColor(R.color.primary)),
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

    companion object {
        private const val TAG = "LoginFragment"
    }
}
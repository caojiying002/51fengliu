package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jiyingcao.a51fengliu.databinding.ActivityPostInfoBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity

class PostInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityPostInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTitlebar()
        setupKeyboardDismiss()
        setupClickListeners()
    }

    private fun setupTitlebar() {
        binding.titleBar.titleBarBack.text = "发布信息"
        binding.titleBar.titleBarBack.setOnClickListener {
            finish()
        }
    }

    private fun setupKeyboardDismiss() {
        // Automatically find and watch all EditTexts within the KeyboardDismissFrameLayout
        binding.keyboardDismissLayout.watchAllEditTextsInLayout()
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            // TODO: Implement submit logic
        }
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, PostInfoActivity::class.java)
        }

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}

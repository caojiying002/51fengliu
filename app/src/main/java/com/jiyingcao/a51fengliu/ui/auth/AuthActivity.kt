package com.jiyingcao.a51fengliu.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jiyingcao.a51fengliu.databinding.ActivityAuthBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity

class AuthActivity: BaseActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
        private const val TAG = "AuthActivity"

        const val EXTRA_IS_INTERCEPTED = "extra_is_intercepted"

        @JvmStatic
        fun createIntent(context: Context) = Intent(context, AuthActivity::class.java)

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}
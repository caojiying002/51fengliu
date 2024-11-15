package com.jiyingcao.a51fengliu.ui

import android.os.Bundle
import com.jiyingcao.a51fengliu.databinding.ActivityLoginBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity

class LoginActivity: BaseActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}
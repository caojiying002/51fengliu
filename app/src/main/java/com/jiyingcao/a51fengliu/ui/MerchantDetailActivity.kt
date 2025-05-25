package com.jiyingcao.a51fengliu.ui

import android.os.Bundle
import com.jiyingcao.a51fengliu.databinding.ActivityMerchantDetailBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity

class MerchantDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityMerchantDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMerchantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}
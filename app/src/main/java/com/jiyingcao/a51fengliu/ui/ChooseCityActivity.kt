package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiyingcao.a51fengliu.databinding.ActivityChooseCityBinding
import com.jiyingcao.a51fengliu.ui.adapter.CityAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.City
import com.jiyingcao.a51fengliu.util.provinceList
import com.jiyingcao.a51fengliu.viewmodel.ChooseCityEffect
import com.jiyingcao.a51fengliu.viewmodel.ChooseCityIntent
import com.jiyingcao.a51fengliu.viewmodel.ChooseCityViewModel
import com.jiyingcao.a51fengliu.viewmodel.ChooseCityState
import kotlinx.coroutines.launch

class ChooseCityActivity: BaseActivity() {
    private lateinit var binding: ActivityChooseCityBinding

    private lateinit var cityAdapter: CityAdapter

    private val viewModel: ChooseCityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseCityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()
        setupRecyclerView()
        setupFlowCollectors()
    }

    override fun onBackPressed() {
        if (viewModel.state.value is ChooseCityState.CityList) {
            viewModel.processIntent(ChooseCityIntent.BackToProvince)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupClickListeners() {
        binding.titleBar.titleBarBack.setOnClickListener { this.onBackPressed() }
    }

    private fun setupRecyclerView() {
        cityAdapter = CityAdapter().apply {
            setOnItemClickListener { _, _, position ->
                when (viewModel.state.value) {
                    is ChooseCityState.ProvinceList -> {
                        viewModel.processIntent(ChooseCityIntent.SelectProvince(position))
                    }
                    is ChooseCityState.CityList -> {
                        viewModel.processIntent(ChooseCityIntent.SelectCity(position))
                    }
                }
            }
        }
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = cityAdapter
        }
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    ChooseCityState.ProvinceList -> {
                        updateTitle("请选择省市")
                        updateList(provinceList)    // 直接使用全局常量 provinceList
                    }
                    is ChooseCityState.CityList -> {
                        updateTitle(state.province.name)
                        updateList(state.cities)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ChooseCityEffect.CitySelected -> {
                        setResultOkAndFinish(effect.city)
                    }
                }
            }
        }
    }

    private fun updateList(cities: List<City>) {
        cityAdapter.submitList(cities)
        // TODO 数据更新后，重置列表滚动位置到顶部
        //binding.recyclerView.scrollToPosition(0)
    }

    private fun updateTitle(title: String) {
        binding.titleBar.titleBarBack.text = title
    }

    private fun setResultOkAndFinish(city: City) {
        Log.d(TAG, "City selected: ${city.name}, code = ${city.code}")
        setResult(RESULT_OK, Intent().apply {
            putExtra("CITY_CODE", city.code)
        })
        finish()
    }

    companion object {
        private const val TAG = "ChooseCityActivity"

        @JvmStatic
        fun createIntent(context: Context) =
            Intent(context, ChooseCityActivity::class.java).apply {
                // putExtra("ITEM_DATA", itemData)
            }

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}
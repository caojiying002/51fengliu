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
import com.jiyingcao.a51fengliu.viewmodel.ChooseCityEffect
import com.jiyingcao.a51fengliu.viewmodel.ChooseCityIntent
import com.jiyingcao.a51fengliu.viewmodel.ChooseCityViewModel
import kotlinx.coroutines.launch

class ChooseCityActivity: BaseActivity() {
    private lateinit var binding: ActivityChooseCityBinding

    private lateinit var cityAdapter: CityAdapter

    private val viewModel: ChooseCityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseCityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.titleBar.titleBarBack.setOnClickListener { this.onBackPressed() }

        cityAdapter = CityAdapter().apply {
            setOnItemClickListener { _, _, position ->
                val state = viewModel.state.value
                if (state.isProvinceLevel) {
                    viewModel.processIntent(ChooseCityIntent.SelectProvince(position))
                } else {
                    viewModel.processIntent(ChooseCityIntent.SelectCity(position))
                }
            }
        }
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = cityAdapter
        }

        setupFlowCollectors()

        viewModel.processIntent(ChooseCityIntent.Load)
    }

    override fun onBackPressed() {
        if (!viewModel.state.value.isProvinceLevel) {
            viewModel.processIntent(ChooseCityIntent.BackToProvince)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                // 设置标题
                binding.titleBar.titleBarBack.text = state.title
                // 设置列表
                updateList(if (state.isProvinceLevel) state.provinceList else state.cityList)
            }
        }
        lifecycleScope.launch {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ChooseCityEffect.CitySelected -> {
                        val selectedCity = effect.city
                        Log.d(TAG, "City selected: ${selectedCity.name}, code = ${selectedCity.code}")
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("CITY_CODE", selectedCity.code)
                        })
                        finish()
                    }
                }
            }
        }
    }

    @Deprecated("use setupFlowCollectors()")
    private fun setupFlowCollectors0() {
        // 观察当前是否在省级选择
        lifecycleScope.launch {
            viewModel.isProvinceLevelFlow.collect { isProvinceLevel ->
                if (isProvinceLevel) {
                    updateTitle("请选择省市")
                } else {
                    viewModel.getSelectedProvince()?.let { updateTitle(it.name) }
                }
            }
        }
        // 观察并显示当前列表（自动在省级和市级列表间切换）
        lifecycleScope.launch {
            viewModel.currentListFlow.collect { cities ->
                updateList(cities)
            }
        }
        // 观察选中的城市
        lifecycleScope.launch {
            viewModel.selectedCity.collect { city ->
                city?.let {
                    // 城市被选中，进行下一步操作
                    Log.d(TAG, "City selected: ${it.name}, code = ${it.code}")
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("CITY_CODE", it.code)
                    })
                    finish()
                }
            }
        }
    }

    private fun updateList(cities: List<City>) {
        // 更新 RecyclerView 的数据
        // 在 RecyclerView 的点击监听中调用 viewModel.onItemClick(city)
        cityAdapter.submitList(cities)
        // TODO 数据更新后，重置列表滚动位置到顶部
        //binding.recyclerView.scrollToPosition(0)
    }

    private fun updateTitle(title: String) {
        binding.titleBar.titleBarBack.text = title
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
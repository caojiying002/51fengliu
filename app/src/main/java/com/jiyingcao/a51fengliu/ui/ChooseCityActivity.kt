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
        setupClickListeners()
        setupRecyclerView()
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

    private fun setupClickListeners() {
        binding.titleBar.titleBarBack.setOnClickListener { this.onBackPressed() }
    }

    private fun setupRecyclerView() {
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
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                updateTitle(state.title)
                updateList(if (state.isProvinceLevel) state.provinceList else state.cityList)
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
        // 更新 RecyclerView 的数据
        // 在 RecyclerView 的点击监听中调用 viewModel.onItemClick(city)
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
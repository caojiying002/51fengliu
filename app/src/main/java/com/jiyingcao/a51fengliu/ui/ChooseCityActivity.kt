package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiyingcao.a51fengliu.databinding.ActivityChooseCityBinding
import com.jiyingcao.a51fengliu.ui.adapter.CityAdapter
import com.jiyingcao.a51fengliu.ui.adapter.CityAdapter2
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

    private lateinit var cityAdapter: CityAdapter2

    /** 省级列表的滚动位置，点击进入市级列表时保存，从市级列表返回时恢复滚动位置 */
    private var provinceScrollPosition = 0

    private val viewModel: ChooseCityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseCityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBackPressedCallback()
        setupClickListeners()
        setupRecyclerView()
        setupFlowCollectors()
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.state.value is ChooseCityState.CityList) {
                    viewModel.processIntent(ChooseCityIntent.BackToProvince)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.titleBar.titleBarBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        cityAdapter = CityAdapter2().apply {
            setOnItemClickListener { _, position ->
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
            // 禁用动画
            itemAnimator = null
        }
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    ChooseCityState.ProvinceList -> {
                        updateTitle("请选择省市")
                        updateList(provinceList)
                    }
                    is ChooseCityState.CityList -> {
                        // 从省到市之前，保存省列表的滚动位置以供将来恢复
                        provinceScrollPosition = (binding.recyclerView.layoutManager as LinearLayoutManager)
                            .findFirstVisibleItemPosition()
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
        cityAdapter.submitList(cities) {
            // After list update is complete
            if (viewModel.state.value is ChooseCityState.CityList) {
                // 市级列表滚动到顶端
                binding.recyclerView.scrollToPosition(0)
            } else {
                // 省级列表恢复之前的滚动位置
                binding.recyclerView.scrollToPosition(provinceScrollPosition)
            }
        }
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
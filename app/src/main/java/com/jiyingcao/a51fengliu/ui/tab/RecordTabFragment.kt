package com.jiyingcao.a51fengliu.ui.tab

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.databinding.FragmentCityTabPagerBinding
import com.jiyingcao.a51fengliu.ui.ChooseCityActivity
import com.jiyingcao.a51fengliu.ui.widget.ViewPager2TouchInterceptor
import com.jiyingcao.a51fengliu.util.AppLogger
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.CitySelectionViewModel
import kotlinx.coroutines.launch

class RecordTabFragment : Fragment() {
    private var _binding: FragmentCityTabPagerBinding? = null
    private val binding get() = _binding!!
    private val tabTitles = listOf("最新发布", "一周热门", "本月热门", "上月热门")

    /**
     * Shared by all [CityRecordListFragment]s.
     *
     * 由所有 [CityRecordListFragment] 共享。
     */
    private val citySelectionViewModel: CitySelectionViewModel by activityViewModels {
        CitySelectionViewModel.Factory(App.INSTANCE.dataStore)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCityTabPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupTabLayout()
        setupClickListeners()
        //setupNestedScrolling() // 不需要，用嵌套布局解决滑动冲突了

        viewLifecycleOwner.lifecycleScope.launch {
            //viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                citySelectionViewModel.selectedCity.collect { cityCode ->
                    displayCity(cityCode)
                }
            //}
        }
    }

    /**
     * 设置嵌套滑动，解决ViewPager2与内部RecyclerView的滑动冲突
     */
    private fun setupNestedScrolling() {
        // 在ViewPager2层次设置触摸拦截器
        ViewPager2TouchInterceptor.setupWithViewPager(binding.viewPager)
        
        // 额外设置嵌套滚动视图处理
        ViewPager2TouchInterceptor.setupNestedScrollableViews(binding.viewPager)
        
        // 日志输出
        AppLogger.d(TAG, "ViewPager2TouchInterceptor setup complete")
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViewPager() {
        val adapter = CityRecordsTabAdapter(this)
        with (binding.viewPager) {
            this.adapter = adapter
            offscreenPageLimit = 3
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    // 不再需要手动更新生命周期
                }
            })
        }
    }

    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private fun setupClickListeners() {
        binding.titleBarChooseCity.setOnClickListener { v ->
            chooseCityLauncher.launch(ChooseCityActivity.createIntent(v.context))
        }
    }

    private fun displayCity(cityCode: String?) {
        binding.titleBarChooseCity.text = cityCode?.to2LevelName() ?: "选择地区"
    }

    /**
     * 处理[ChooseCityActivity]返回的城市代码。
     * 通知[CityRecordListFragment]们更新数据。
     * TODO remove this method
     */
    private fun handleCityCode(cityCode: String) {
        // 在这里处理返回的城市代码
        // 例如: 更新UI, 保存到数据库等
        App.INSTANCE.showToast("City code selected: $cityCode")
        /*cityCode?.let {
                if (it == this.cityCode) return

                // 清空数据
                recordAdapter.submitList(emptyList())
                hasDataLoaded = false
                currentPage = 0

                this.cityCode = it
                viewModel.fetchCityDataByPage(it, "publish", 1)
            }*/
    }

    private val chooseCityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val cityCode = result.data?.getStringExtra("CITY_CODE")
            AppLogger.d("registerForActivityResult", "City code selected: $cityCode")
            cityCode?.let {
                App.INSTANCE.showToast("City code selected: $cityCode")
                citySelectionViewModel.setCity(it)
            }
        }
    }

    inner class CityRecordsTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = tabTitles.size

        override fun createFragment(position: Int): Fragment {
            val sort: String = when (position) {
                0 -> "publish"
                1 -> "weekly"
                2 -> "monthly"
                3 -> "lastMonth"
                else -> "publish"
            }
            return CityRecordListFragment.newInstance(sort)
        }
    }

    companion object {
        private const val TAG = "CityRecordsFragment"
    }
}
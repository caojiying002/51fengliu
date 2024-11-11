package com.jiyingcao.a51fengliu.ui.tab

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.ChooseCityActivity
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.CityRecordsViewModel
import kotlinx.coroutines.launch

class CityRecordsFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var titleBarChooseCity: TextView
    private val tabTitles = listOf("最新发布", "一周热门", "本月热门", "上月热门")

    /**
     * Shared by all [CityRecordsSubFragment]s.
     *
     * 由所有 [CityRecordsSubFragment] 共享。
     */
    private val viewModel: CityRecordsViewModel by activityViewModels {
        CityRecordsViewModel.Factory(App.INSTANCE.dataStore)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_city_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        setupViewPager()
        setupTabLayout()
        setupClickListeners(view)

        viewLifecycleOwner.lifecycleScope.launch {
            //viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCity.collect { cityCode ->
                    displayCity(cityCode)
                }
            //}
        }
    }

    override fun onResume() {
        super.onResume()
        updateChildFragmentLifecycle(viewPager.currentItem)
    }

    override fun onPause() {
        super.onPause()
        updateChildFragmentLifecycle(-1) // 传入一个无效的位置，确保所有子Fragment都处于STARTED状态
    }

    private fun updateChildFragmentLifecycle(position: Int) {
        val fragmentManager = childFragmentManager
        var fragmentTransaction: FragmentTransaction? = null

        fragmentManager.fragments.forEachIndexed { index, fragment ->
            if (fragment is CityRecordsSubFragment) {
                if (fragmentTransaction == null)
                    fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction!!.setMaxLifecycle(fragment, if (index == position) Lifecycle.State.RESUMED else Lifecycle.State.STARTED)
            }
        }
        fragmentTransaction?.commitNowAllowingStateLoss()
    }

    private fun setupViewPager() {
        val adapter = CityRecordsTabAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 3

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateChildFragmentLifecycle(position)
            }
        })
    }

    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private fun setupClickListeners(view: View) {
        titleBarChooseCity = view.findViewById(R.id.title_bar_choose_city)
        titleBarChooseCity.setOnClickListener { v ->
            chooseCityLauncher.launch(ChooseCityActivity.createIntent(v.context))
        }
    }

    private fun displayCity(cityCode: String?) {
        titleBarChooseCity.text = cityCode?.to2LevelName() ?: "选择地区"
    }

    /**
     * 处理[ChooseCityActivity]返回的城市代码。
     * 通知[CityRecordsSubFragment]们更新数据。
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
            Log.d("registerForActivityResult", "City code selected: $cityCode")
            cityCode?.let {
                App.INSTANCE.showToast("City code selected: $cityCode")
                viewModel.setCity(it)
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
            return CityRecordsSubFragment.newInstance(sort)
        }
    }
}
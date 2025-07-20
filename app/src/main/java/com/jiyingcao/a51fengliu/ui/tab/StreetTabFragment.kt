package com.jiyingcao.a51fengliu.ui.tab

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.ChooseCityActivity
import com.jiyingcao.a51fengliu.util.AppLogger
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.CityRecordsViewModel
import kotlinx.coroutines.launch

class StreetTabFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var titleBarChooseCity: TextView
    private val tabTitles = listOf("最新发布", "一周热门", "本月热门", "上月热门")

    /**
     * Shared by all [StreetListFragment]s.
     *
     * 由所有 [StreetListFragment] 共享。
     */
    private val viewModel: CityRecordsViewModel by activityViewModels {
        CityRecordsViewModel.Factory(App.INSTANCE.dataStore)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_street_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        setupViewPager()
        setupTabLayout()
        setupClickListeners(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCity.collect { cityCode ->
                displayCity(cityCode)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun setupViewPager() {
        val adapter = StreetTabAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 3

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 不再需要手动更新生命周期
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

    private val chooseCityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val cityCode = result.data?.getStringExtra("CITY_CODE")
            AppLogger.d("registerForActivityResult", "City code selected: $cityCode")
            cityCode?.let {
                App.INSTANCE.showToast("City code selected: $cityCode")
                viewModel.setCity(it)
            }
        }
    }

    inner class StreetTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = tabTitles.size

        override fun createFragment(position: Int): Fragment {
            val sort: String = when (position) {
                0 -> "publish"
                1 -> "weekly"
                2 -> "monthly"
                3 -> "lastMonth"
                else -> "publish"
            }
            return StreetListFragment.newInstance(sort, position)
        }
    }
    
    companion object {
        private const val TAG = "StreetTabFragment"
    }
}
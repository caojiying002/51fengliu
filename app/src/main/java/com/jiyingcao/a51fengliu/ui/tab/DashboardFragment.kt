package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jiyingcao.a51fengliu.R

class DashboardFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private val tabTitles = listOf("最新发布", "一周热门", "本月热门", "上月热门")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.common_pager_with_tabs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        setupViewPager()
        setupTabLayout()
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

    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        updateChildFragmentLifecycle(viewPager.currentItem)
    }

    override fun onPause() {
        super.onPause()
        updateChildFragmentLifecycle(-1) // 传入一个无效的位置，确保所有子Fragment都处于STARTED状态
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
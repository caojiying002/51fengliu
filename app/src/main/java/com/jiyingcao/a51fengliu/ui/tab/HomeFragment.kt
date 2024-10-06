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

class HomeFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private val tabTitles = listOf("热门", "最新")

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
        val adapter = HomeTabAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

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
            if (fragment is HomeSubFragment) {
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

    inner class HomeTabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = tabTitles.size

        override fun createFragment(position: Int): Fragment {
            val sort: String = when (position) {
                0 -> "daily"
                1 -> "publish"
                else -> "daily"
            }
            return HomeSubFragment.newInstance(sort)
        }
    }
}
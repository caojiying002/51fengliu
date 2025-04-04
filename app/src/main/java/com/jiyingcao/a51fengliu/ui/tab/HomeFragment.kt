package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.SearchActivity

class HomeFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private val tabTitles = listOf("热门", "最新")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        setupViewPager()
        setupTabLayout()
        setupClickListeners(view)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun setupViewPager() {
        val adapter = HomeTabAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

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
        view.findViewById<View>(R.id.title_bar_search)?.setOnClickListener { v ->
            startActivity(SearchActivity.createIntent(v.context))
        }
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
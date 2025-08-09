package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.databinding.ActivityFavoriteBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.tab.FavoriteRecordsFragment
import com.jiyingcao.a51fengliu.ui.tab.FavoriteStreetsFragment

class FavoriteActivity : BaseActivity() {
    private lateinit var binding: ActivityFavoriteBinding
    private val tabTitles = listOf("信息收藏", "攻略手册")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupTitleBar()
        setupViewPager()
        setupTabLayout()
    }

    private fun setupTitleBar() {
        binding.titleBar.titleBarBack.text = getString(R.string.my_favorite)
        binding.titleBar.titleBarBack.setOnClickListener { finish() }
    }

    private fun setupViewPager() {
        val adapter = FavoriteTabAdapter(this)
        with(binding.viewPager) {
            this.adapter = adapter
            offscreenPageLimit = 1
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    // ViewPager2 自动管理 Fragment 生命周期，无需手动处理
                }
            })
        }
    }

    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    inner class FavoriteTabAdapter(activity: FavoriteActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = tabTitles.size

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FavoriteRecordsFragment()  // 信息收藏
                1 -> FavoriteStreetsFragment()  // 攻略手册（暗巷收藏）
                else -> throw IllegalArgumentException("Unknown fragment position: $position")
            }
        }
    }

    companion object {
        private const val TAG = "FavoriteActivity"

        @JvmStatic
        fun createIntent(context: Context) = Intent(context, FavoriteActivity::class.java)

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}
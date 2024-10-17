package com.jiyingcao.a51fengliu.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.tab.*

class MainActivity : BaseActivity() {

    private lateinit var tabHome: View
    private lateinit var tabCityRecords: View
    private lateinit var tabNotifications: View
    private lateinit var tabProfile: View

    private var homeFragment: Fragment? = null
    private var cityRecordsFragment: Fragment? = null
    private var notificationsFragment: Fragment? = null
    private var profileFragment: Fragment? = null

    private var currentTabTag: String = TAG_HOME

    companion object {
        // Fragment tags
        private const val TAG_PREFIX = "MAIN_ACTIVITY_TAB_"
        private const val TAG_HOME = "${TAG_PREFIX}HOME"
        private const val TAG_CITY_RECORDS = "${TAG_PREFIX}CITY_RECORDS"
        private const val TAG_NOTIFICATIONS = "${TAG_PREFIX}NOTIFICATIONS"
        private const val TAG_PROFILE = "${TAG_PREFIX}PROFILE"

        // Saved instance state keys
        private const val KEY_CURRENT_TAB = "CURRENT_TAB"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabHome = findViewById(R.id.tabHome)
        tabCityRecords = findViewById(R.id.tabCityRecords)
        tabNotifications = findViewById(R.id.tabNotifications)
        tabProfile = findViewById(R.id.tabProfile)

        setupTabs()

        // 从 savedInstanceState 恢复当前选中的 Tab，或使用默认值
        currentTabTag = savedInstanceState?.getString(KEY_CURRENT_TAB) ?: TAG_HOME
        // 加载相应的 Fragment 并更新 Tab 状态
        loadFragment(currentTabTag)
        updateTabStates(currentTabTag)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存当前选中的 Tab
        outState.putString(KEY_CURRENT_TAB, currentTabTag)
    }

    private fun setupTabs() {
        setupTab(tabHome, R.drawable.ic_home, "首页", TAG_HOME)
        setupTab(tabCityRecords, R.drawable.ic_search, "信息", TAG_CITY_RECORDS)
        setupTab(tabNotifications, R.drawable.ic_notification, "商家", TAG_NOTIFICATIONS)
        setupTab(tabProfile, R.drawable.ic_profile, "我的", TAG_PROFILE)
    }

    private fun setupTab(tabView: View, iconResId: Int, text: String, tag: String) {
        tabView.findViewById<ImageView>(R.id.tabIcon).setImageResource(iconResId)
        tabView.findViewById<TextView>(R.id.tabText).text = text
        tabView.setOnClickListener {
            loadFragment(tag)
            updateTabStates(tag)
        }
    }

    private fun updateTabStates(selectedTag: String) {
        updateTabState(tabHome, selectedTag == TAG_HOME)
        updateTabState(tabCityRecords, selectedTag == TAG_CITY_RECORDS)
        updateTabState(tabNotifications, selectedTag == TAG_NOTIFICATIONS)
        updateTabState(tabProfile, selectedTag == TAG_PROFILE)
        currentTabTag = selectedTag
    }

    @SuppressLint("ResourceType")
    private fun updateTabState(tabView: View, isSelected: Boolean) {
        val colorStateList = ContextCompat.getColorStateList(this, R.drawable.tab_color_selector)
        tabView.isSelected = isSelected
        tabView.findViewById<ImageView>(R.id.tabIcon).imageTintList = colorStateList
        tabView.findViewById<TextView>(R.id.tabText).setTextColor(colorStateList)
    }

    private fun loadFragment(tag: String) {
        supportFragmentManager.beginTransaction().apply {
            // 只隐藏和管理我们自己的 Fragment，避免影响第三方添加的 Fragment，例如权限请求库
            supportFragmentManager.fragments.forEach { fragment ->
                if (fragment.tag?.startsWith(TAG_PREFIX) == true) {
                    hide(fragment)
                    setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                }
            }

            // 获取或创建目标 Fragment
            var targetFragment = supportFragmentManager.findFragmentByTag(tag)
            if (targetFragment == null) {
                targetFragment = when (tag) {
                    TAG_HOME -> HomeFragment().also { homeFragment = it }
                    TAG_CITY_RECORDS -> CityRecordsFragment().also { cityRecordsFragment = it }
                    TAG_NOTIFICATIONS -> NotificationsFragment().also { notificationsFragment = it }
                    TAG_PROFILE -> ProfileFragment().also { profileFragment = it }
                    else -> throw IllegalArgumentException("Unknown fragment tag: $tag")
                }
                add(R.id.fragmentContainer, targetFragment, tag)
            } else {
                // 更新 Activity 持有的引用
                when (tag) {
                    TAG_HOME -> homeFragment = targetFragment
                    TAG_CITY_RECORDS -> cityRecordsFragment = targetFragment
                    TAG_NOTIFICATIONS -> notificationsFragment = targetFragment
                    TAG_PROFILE -> profileFragment = targetFragment
                }
                show(targetFragment)
            }

            // 将所需的 Fragment 设置为 RESUMED 状态
            setMaxLifecycle(targetFragment, Lifecycle.State.RESUMED)
        }.commitNowAllowingStateLoss()
    }
}
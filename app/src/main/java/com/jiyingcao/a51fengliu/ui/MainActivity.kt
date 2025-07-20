package com.jiyingcao.a51fengliu.ui

import android.content.Intent
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
import com.jiyingcao.a51fengliu.util.AppLogger

class MainActivity : BaseActivity() {

    private lateinit var tabHome: View
    private lateinit var tabRecord: View
    private lateinit var tabStreet: View
    private lateinit var tabMerchant: View
    private lateinit var tabProfile: View

    private var homeFragment: Fragment? = null
    private var recordTabFragment: Fragment? = null
    private var streetTabFragment: Fragment? = null
    private var merchantListFragment: Fragment? = null
    private var profileFragment: Fragment? = null

    private var currentTabTag: String = TAG_HOME

    companion object {
        // Fragment tags
        private const val TAG_PREFIX = "MAIN_ACTIVITY_TAB_"
        private const val TAG_HOME = "${TAG_PREFIX}HOME"
        private const val TAG_RECORD = "${TAG_PREFIX}RECORD"
        private const val TAG_STREET = "${TAG_PREFIX}STREET"
        private const val TAG_MERCHANT = "${TAG_PREFIX}MERCHANT"
        private const val TAG_PROFILE = "${TAG_PREFIX}PROFILE"

        // Saved instance state keys
        private const val KEY_CURRENT_TAB = "CURRENT_TAB"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabHome = findViewById(R.id.tabHome)
        tabRecord = findViewById(R.id.tabRecord)
        tabStreet = findViewById(R.id.tabStreet)
        tabMerchant = findViewById(R.id.tabMerchant)
        tabProfile = findViewById(R.id.tabProfile)

        setupTabs()

        // 检查是否是重新登录
        currentTabTag =
            if (intent.getBooleanExtra("IS_RELOGIN", false)) {
                TAG_PROFILE
            } else {
                // 从 savedInstanceState 恢复当前选中的 Tab，或使用默认值
                savedInstanceState?.getString(KEY_CURRENT_TAB) ?: TAG_HOME
            }
        // 加载相应的 Fragment 并更新 Tab 状态
        loadFragment(currentTabTag)
        updateTabStates(currentTabTag)
        AppLogger.d("MainActivity", "onCreate() called, this = $this")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AppLogger.d("MainActivity", "onNewIntent() called")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存当前选中的 Tab
        outState.putString(KEY_CURRENT_TAB, currentTabTag)
    }

    private fun setupTabs() {
        setupTab(tabHome, R.drawable.ic_home, "首页", TAG_HOME)
        setupTab(tabRecord, R.drawable.ic_record, "信息", TAG_RECORD)
        setupTab(tabStreet, R.drawable.ic_street, "暗巷", TAG_STREET)
        setupTab(tabMerchant, R.drawable.ic_merchant, "商家", TAG_MERCHANT)
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
        updateTabState(tabRecord, selectedTag == TAG_RECORD)
        updateTabState(tabStreet, selectedTag == TAG_STREET)
        updateTabState(tabMerchant, selectedTag == TAG_MERCHANT)
        updateTabState(tabProfile, selectedTag == TAG_PROFILE)
        currentTabTag = selectedTag
    }

    private fun updateTabState(tabView: View, isSelected: Boolean) {
        val colorStateList = ContextCompat.getColorStateList(this, R.color.selector_main_tab_color)
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
                    TAG_RECORD -> RecordTabFragment().also { recordTabFragment = it }
                    TAG_STREET -> StreetTabFragment().also { streetTabFragment = it }
                    TAG_MERCHANT -> MerchantListFragment().also { merchantListFragment = it }
                    TAG_PROFILE -> ProfileFragment().also { profileFragment = it }
                    else -> throw IllegalArgumentException("Unknown fragment tag: $tag")
                }
                add(R.id.fragmentContainer, targetFragment, tag)
            } else {
                // 更新 Activity 持有的引用
                when (tag) {
                    TAG_HOME -> homeFragment = targetFragment
                    TAG_RECORD -> recordTabFragment = targetFragment
                    TAG_STREET -> streetTabFragment = targetFragment
                    TAG_MERCHANT -> merchantListFragment = targetFragment
                    TAG_PROFILE -> profileFragment = targetFragment
                }
                show(targetFragment)
            }

            // 将所需的 Fragment 设置为 RESUMED 状态
            setMaxLifecycle(targetFragment, Lifecycle.State.RESUMED)
        }.commitNowAllowingStateLoss()
    }
}
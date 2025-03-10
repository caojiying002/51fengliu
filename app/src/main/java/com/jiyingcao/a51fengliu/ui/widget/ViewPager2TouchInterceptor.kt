package com.jiyingcao.a51fengliu.ui.widget

import android.util.Log
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlin.math.abs

/**
 * ViewPager2 与嵌套 RecyclerView 的滑动冲突解决工具类
 * 
 * 这个工具类通过编程方式解决 ViewPager2 内嵌 RecyclerView 时的滑动冲突问题，特别是
 * 防止垂直滚动 RecyclerView 时意外触发 ViewPager2 的水平页面切换，无需修改 XML 布局。
 * 
 * 使用方法：在 Fragment 的 onViewCreated 中调用 ViewPager2TouchInterceptor.setupWithViewPager(viewPager)
 * 
 * -----------------------------------------------------------------------
 * 
 * Utility class for resolving scrolling conflicts between ViewPager2 and nested RecyclerViews
 * 
 * This utility class programmatically resolves touch conflicts between ViewPager2 and its nested RecyclerViews,
 * specifically preventing accidental horizontal page switching when scrolling RecyclerViews vertically,
 * without requiring XML layout changes.
 * 
 * Usage: Call ViewPager2TouchInterceptor.setupWithViewPager(viewPager) in your Fragment's onViewCreated method.
 */
object ViewPager2TouchInterceptor {
    
    private const val TAG = "VP2TouchInterceptor"
    
    // 增加垂直方向的优先级系数，使垂直判断更敏感
    // Increase vertical bias coefficient to make vertical detection more sensitive
    private const val VERTICAL_BIAS = 1.5f
    
    // 在开始滑动后的一段时间内，更积极地阻止ViewPager2横向滑动
    // More aggressively prevent ViewPager2 horizontal scrolling during this initial period
    private const val SCROLL_LOCK_DURATION_MS = 200L
    
    // 方向判断的阈值系数，用于确定垂直或水平滑动
    // Direction determination threshold coefficient, used to determine vertical or horizontal scrolling
    private const val DIRECTION_DETERMINATION_FACTOR = 1.2f
    
    /**
     * 设置 ViewPager2 的触摸冲突处理
     * 
     * @param viewPager ViewPager2 实例
     * 
     * Setup touch conflict handling for ViewPager2
     * 
     * @param viewPager The ViewPager2 instance
     */
    fun setupWithViewPager(viewPager: ViewPager2) {
        // 获取 ViewPager2 内部的 RecyclerView
        // Get access to the internal RecyclerView of ViewPager2
        val recyclerView = findViewPagerInternalRecyclerView(viewPager)
        
        recyclerView?.let {
            it.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                private var initialX = 0f
                private var initialY = 0f
                private var lastX = 0f
                private var lastY = 0f
                private var touchDownTime = 0L
                private var isVerticalScrolling = false
                private var isHorizontalScrolling = false
                private val touchSlop = ViewConfiguration.get(it.context).scaledTouchSlop
                
                override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                    when (e.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            // 新触摸序列开始时重置所有标记
                            // Reset all flags at start of new touch sequence
                            initialX = e.x
                            initialY = e.y
                            lastX = e.x
                            lastY = e.y
                            isVerticalScrolling = false
                            isHorizontalScrolling = false
                            touchDownTime = System.currentTimeMillis()
                            
                            // 初始化时启用 ViewPager2 滑动
                            // Initially enable ViewPager2 swiping
                            viewPager.isUserInputEnabled = true
                        }
                        
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val diffX = e.x - initialX
                            val diffY = e.y - initialY
                            
                            // 计算速度以检测快速滑动手势
                            // Calculate velocity to detect flick gestures
                            val deltaX = e.x - lastX
                            val deltaY = e.y - lastY
                            lastX = e.x
                            lastY = e.y
                            
                            // 应用垂直偏差使垂直检测更敏感
                            // Apply vertical bias to make vertical detection more sensitive
                            val biasedDiffY = diffY * VERTICAL_BIAS
                            
                            // 检查是否仍在滚动锁定期内
                            // Check if we're still in the scroll lock period
                            val isInLockPeriod = (System.currentTimeMillis() - touchDownTime) < SCROLL_LOCK_DURATION_MS
                            
                            // 仅在尚未设置任何标记时检测滚动方向
                            // Detect the scroll direction only if neither flag is set yet
                            if (!isVerticalScrolling && !isHorizontalScrolling) {
                                // 检查是否移动足够距离以视为滚动
                                // Check if we've moved enough to consider it a scroll
                                if (abs(diffY) > touchSlop || abs(diffX) > touchSlop) {
                                    // 根据距离和速度确定滚动方向
                                    // Determine scroll direction based on both distance and velocity
                                    if (abs(biasedDiffY) > abs(diffX) || abs(deltaY) > abs(deltaX) * DIRECTION_DETERMINATION_FACTOR) {
                                        isVerticalScrolling = true
                                        viewPager.isUserInputEnabled = false
                                        Log.d(TAG, "检测到垂直滚动，禁用 ViewPager2")
                                    } else if (abs(diffX) > abs(diffY) * DIRECTION_DETERMINATION_FACTOR) {
                                        isHorizontalScrolling = true
                                        viewPager.isUserInputEnabled = true
                                        Log.d(TAG, "检测到水平滚动，启用 ViewPager2")
                                    }
                                }
                            }
                            
                            // 如果在锁定期内且正在垂直移动，则保持 ViewPager 禁用状态
                            // If we're within the lock period and moving vertically, keep ViewPager disabled
                            if (isInLockPeriod && abs(diffY) > touchSlop && !isHorizontalScrolling) {
                                viewPager.isUserInputEnabled = false
                            }
                            
                            // 处理确定处于垂直滚动模式的情况
                            // Handle the case when we're definitely in vertical scrolling mode
                            if (isVerticalScrolling) {
                                viewPager.isUserInputEnabled = false
                            }
                            
                            // 在水平滚动期间，确保 ViewPager 启用
                            // During horizontal scrolling, make sure ViewPager is enabled
                            if (isHorizontalScrolling) {
                                viewPager.isUserInputEnabled = true
                            }
                            
                            // 额外的安全措施，防止 ViewPager2 在对角线滚动中接管
                            // Additional safety to prevent ViewPager2 from taking over on diagonal scrolls
                            if (!isHorizontalScrolling && abs(diffY) > touchSlop) {
                                viewPager.isUserInputEnabled = false
                            }
                            
                            // 检查当前页面中的 RecyclerView 是否可以垂直滚动
                            // Check if RecyclerView in current page can scroll vertically
                            val currentPageRecyclerView = findVerticalScrollableView(viewPager)
                            if (currentPageRecyclerView != null && abs(diffY) > touchSlop) {
                                viewPager.isUserInputEnabled = false
                            }
                        }
                        
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            // 触摸序列结束时重置标记
                            // Reset flags at end of touch sequence
                            isVerticalScrolling = false
                            isHorizontalScrolling = false
                            
                            // 触摸序列结束后重新启用 ViewPager2
                            // Re-enable ViewPager2 after touch sequence ends
                            viewPager.isUserInputEnabled = true
                        }
                    }
                    return false
                }
                
                override fun onTouchEvent(rv: RecyclerView, e: android.view.MotionEvent) {}
                
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }
    }
    
    /**
     * 查找 ViewPager2 内部的 RecyclerView
     * Find the internal RecyclerView of ViewPager2
     */
    private fun findViewPagerInternalRecyclerView(viewPager: ViewPager2): RecyclerView? {
        return (viewPager.getChildAt(0) as? RecyclerView)
    }
    
    /**
     * 检查 ViewPager2 当前页面是否含有可垂直滚动的视图
     * Check if the current page of ViewPager2 contains vertically scrollable views
     */
    private fun findVerticalScrollableView(viewPager: ViewPager2): RecyclerView? {
        // 获取当前页面视图
        // Current page in ViewPager2
        val currentItemView = findCurrentItemView(viewPager)
        
        if (currentItemView != null) {
            return findRecyclerViewInHierarchy(currentItemView)
        }
        
        return null
    }
    
    /**
     * 获取 ViewPager2 当前页面的根视图
     * Get the root view of the current page in ViewPager2
     */
    private fun findCurrentItemView(viewPager: ViewPager2): View? {
        val recyclerView = findViewPagerInternalRecyclerView(viewPager)
        
        return recyclerView?.findViewHolderForAdapterPosition(viewPager.currentItem)?.itemView
    }
    
    /**
     * 在视图层次结构中查找 RecyclerView
     * Find RecyclerView in view hierarchy
     */
    private fun findRecyclerViewInHierarchy(view: View): RecyclerView? {
        // 检查是否是 RecyclerView 并且可以垂直滚动
        // Check if it's a RecyclerView with vertical scrolling
        if (view is RecyclerView && view.canScrollVertically(1)) {
            return view
        }
        
        // 检查是否是 SmartRefreshLayout
        // Check if it's a SmartRefreshLayout
        if (view is SmartRefreshLayout) {
            // 检查 SmartRefreshLayout 是否包含任何可滚动的子视图
            // Check if SmartRefreshLayout contains any scrollable child
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is RecyclerView && child.canScrollVertically(1)) {
                    return child
                }
            }
        }
        
        // 在 ViewGroup 层次结构中递归搜索
        // Recursively search in ViewGroup hierarchy
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findRecyclerViewInHierarchy(view.getChildAt(i))
                if (result != null) {
                    return result
                }
            }
        }
        
        return null
    }
}
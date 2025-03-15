package com.jiyingcao.a51fengliu.ui.widget

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.simple.SimpleMultiListener
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
    
    // 最大锁定时间，防止ViewPager2永久锁定
    // Maximum lock time to prevent ViewPager2 from being permanently locked
    private const val MAX_LOCK_DURATION_MS = 1000L
    
    // 方向判断的阈值系数，用于确定垂直或水平滑动
    // Direction determination threshold coefficient, used to determine vertical or horizontal scrolling
    private const val DIRECTION_DETERMINATION_FACTOR = 1.2f
    
    // 处理自动重置ViewPager2状态的Handler
    // Handler for automatically resetting ViewPager2 state
    private val handler = Handler(Looper.getMainLooper())
    
    // 储存ViewPager2和其状态重置任务的映射
    // Store mapping between ViewPager2 and its reset task
    private val resetRunnables = mutableMapOf<ViewPager2, Runnable>()
    
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
                
                // 创建自动重置ViewPager2状态的Runnable
                // Create Runnable for automatically resetting ViewPager2 state
                val resetRunnable = Runnable {
                    viewPager.isUserInputEnabled = true
                    Log.d(TAG, "自动重置 ViewPager2 状态 (超时)")
                }
                
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // 移除之前的重置任务
                            // Remove previous reset task
                            handler.removeCallbacks(resetRunnable)
                            resetRunnables[viewPager] = resetRunnable
                            
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
                        
                        MotionEvent.ACTION_MOVE -> {
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
                            
                            // 如果已经确定是水平滑动，保持ViewPager2启用
                            // If we've already determined it's horizontal scrolling, keep ViewPager2 enabled
                            if (isHorizontalScrolling) {
                                viewPager.isUserInputEnabled = true
                                return false
                            }
                            
                            // 如果已经确定是垂直滑动，禁用ViewPager2
                            // If we've already determined it's vertical scrolling, disable ViewPager2
                            if (isVerticalScrolling) {
                                viewPager.isUserInputEnabled = false
                                
                                // 设置自动重置定时器
                                // Set auto-reset timer
                                handler.removeCallbacks(resetRunnable)
                                handler.postDelayed(resetRunnable, MAX_LOCK_DURATION_MS)
                                
                                return false
                            }
                            
                            // 确定滑动方向
                            // Determine scrolling direction
                            if (abs(diffX) > touchSlop || abs(diffY) > touchSlop) {
                                if (abs(biasedDiffY) > abs(diffX) || abs(deltaY) > abs(deltaX) * DIRECTION_DETERMINATION_FACTOR) {
                                    // 垂直滑动
                                    // Vertical scrolling
                                    isVerticalScrolling = true
                                    viewPager.isUserInputEnabled = false
                                    Log.d(TAG, "检测到垂直滚动，禁用 ViewPager2")
                                    
                                    // 设置自动重置定时器
                                    // Set auto-reset timer
                                    handler.removeCallbacks(resetRunnable)
                                    handler.postDelayed(resetRunnable, MAX_LOCK_DURATION_MS)
                                    
                                } else if (abs(diffX) > abs(diffY) * DIRECTION_DETERMINATION_FACTOR) {
                                    // 水平滑动
                                    // Horizontal scrolling
                                    isHorizontalScrolling = true
                                    viewPager.isUserInputEnabled = true
                                    Log.d(TAG, "检测到水平滚动，启用 ViewPager2")
                                }
                            }
                        }
                        
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            // 触摸序列结束时重置标记
                            // Reset flags at end of touch sequence
                            isVerticalScrolling = false
                            isHorizontalScrolling = false
                            
                            // 取消自动重置定时器
                            // Cancel auto-reset timer
                            handler.removeCallbacks(resetRunnable)
                            
                            // 触摸序列结束后重新启用 ViewPager2
                            // Re-enable ViewPager2 after touch sequence ends
                            viewPager.isUserInputEnabled = true
                            Log.d(TAG, "触摸结束，重置 ViewPager2 状态")
                        }
                    }
                    return false
                }
                
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                    // 在触摸事件中进一步确保状态正确
                    // Further ensure state correctness in touch events
                    if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) {
                        // 触摸结束一定要启用ViewPager2
                        // Definitely enable ViewPager2 when touch ends
                        handler.removeCallbacks(resetRunnable)
                        viewPager.isUserInputEnabled = true
                    }
                }
                
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                    // 不做任何处理
                    // Do nothing
                }
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
     * 在指定的 ViewPager2 内部搜索当前页面中的所有 SmartRefreshLayout 和 RecyclerView，
     * 并为它们设置触摸事件监听，以更好地处理嵌套滚动冲突。
     * 
     * Search for all SmartRefreshLayouts and RecyclerViews in the current page of the specified
     * ViewPager2, and set touch event listeners for them to better handle nested scrolling conflicts.
     * 
     * @param viewPager 需要处理的ViewPager2实例
     */
    fun setupNestedScrollableViews(viewPager: ViewPager2) {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // 获取当前页面视图
                val recyclerView = findViewPagerInternalRecyclerView(viewPager)
                val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                val itemView = viewHolder?.itemView
                
                if (itemView is ViewGroup) {
                    setupViewGroupTouchListeners(itemView, viewPager)
                }
            }
        })
    }
    
    /**
     * 在ViewGroup层次结构中递归设置触摸监听
     * Recursively set touch listeners in ViewGroup hierarchy
     */
    private fun setupViewGroupTouchListeners(viewGroup: ViewGroup, viewPager: ViewPager2) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            
            if (child is SmartRefreshLayout) {
                setupSmartRefreshLayoutTouchListener(child, viewPager)
            } else if (child is RecyclerView) {
                setupRecyclerViewTouchListener(child, viewPager)
            } else if (child is ViewGroup) {
                setupViewGroupTouchListeners(child, viewPager)
            }
        }
    }
    
    /**
     * 为SmartRefreshLayout设置触摸监听
     * Set touch listener for SmartRefreshLayout
     */
    private fun setupSmartRefreshLayoutTouchListener(refreshLayout: SmartRefreshLayout, viewPager: ViewPager2) {
        // SmartRefreshLayout已经很好地处理了触摸事件，但我们可以添加额外的监听
        // SmartRefreshLayout already handles touch events well, but we can add extra listeners
        refreshLayout.setOnMultiListener(object : SimpleMultiListener() {
            override fun onLoadMore(refreshLayout: com.scwang.smart.refresh.layout.api.RefreshLayout) {
                // 加载更多时禁用ViewPager2滑动
                // Disable ViewPager2 swiping when loading more
                viewPager.isUserInputEnabled = false
            }
            
            override fun onRefresh(refreshLayout: com.scwang.smart.refresh.layout.api.RefreshLayout) {
                // 下拉刷新时禁用ViewPager2滑动
                // Disable ViewPager2 swiping when refreshing
                viewPager.isUserInputEnabled = false
            }
            
            override fun onStateChanged(
                refreshLayout: com.scwang.smart.refresh.layout.api.RefreshLayout,
                oldState: com.scwang.smart.refresh.layout.constant.RefreshState,
                newState: com.scwang.smart.refresh.layout.constant.RefreshState
            ) {
                // 刷新或加载更多结束时恢复ViewPager2滑动
                // Restore ViewPager2 swiping when refresh or load more ends
                if (newState == com.scwang.smart.refresh.layout.constant.RefreshState.None) {
                    viewPager.isUserInputEnabled = true
                }
            }
        })
    }
    
    /**
     * 为RecyclerView设置触摸监听
     * Set touch listener for RecyclerView
     */
    private fun setupRecyclerViewTouchListener(recyclerView: RecyclerView, viewPager: ViewPager2) {
        // 已经由主方法setupWithViewPager处理
        // Already handled by main method setupWithViewPager
    }
}
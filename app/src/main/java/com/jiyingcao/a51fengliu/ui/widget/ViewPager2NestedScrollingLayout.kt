package com.jiyingcao.a51fengliu.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlin.math.abs

/**
 * ViewPager2 与嵌套 RecyclerView 的滑动冲突解决方案
 * 
 * 这个自定义布局用于解决 ViewPager2 内嵌 RecyclerView 时的滑动冲突问题，特别是
 * 防止垂直滚动 RecyclerView 时意外触发 ViewPager2 的水平页面切换。
 * 
 * 使用方法：在 XML 布局中，将 SmartRefreshLayout 或 RecyclerView 嵌套在本布局中。
 * 
 * -----------------------------------------------------------------------
 * 
 * Solution for scrolling conflicts between ViewPager2 and nested RecyclerViews
 * 
 * This custom layout resolves touch conflicts between ViewPager2 and its nested RecyclerViews,
 * specifically preventing accidental horizontal page switching when scrolling RecyclerViews vertically.
 * 
 * Usage: In XML layouts, wrap your SmartRefreshLayout or RecyclerView with this layout.
 */
class ViewPager2NestedScrollingLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "VP2NestedScroll"
        
        // 增加垂直方向的优先级系数，使垂直判断更敏感
        // Increase vertical bias coefficient to make vertical detection more sensitive
        private const val VERTICAL_BIAS = 1.5f
        
        // 在开始滑动后的一段时间内，更积极地阻止ViewPager2横向滑动
        // More aggressively prevent ViewPager2 horizontal scrolling during this initial period
        private const val SCROLL_LOCK_DURATION_MS = 200L
        
        // 方向判断的阈值系数，用于确定垂直或水平滑动
        // Direction determination threshold coefficient, used to determine vertical or horizontal scrolling
        private const val DIRECTION_DETERMINATION_FACTOR = 1.2f
        
        // 默认禁用调试日志
        // Debug logs disabled by default
        private var debugLogsEnabled = false
        
        /**
         * 设置是否启用调试日志
         * Enable or disable debug logging
         * 
         * @param enabled true to enable debug logs, false to disable
         */
        fun setDebugLogsEnabled(enabled: Boolean) {
            debugLogsEnabled = enabled
        }
        
        /**
         * 内部日志打印方法
         * Internal logging method
         */
        private fun log(message: String) {
            if (debugLogsEnabled) {
                Log.d(TAG, message)
            }
        }
    }

    // 触摸阈值，滑动必须超过这个距离才被视为有效滑动
    // Touch slop is the minimum distance to consider a touch as a scroll
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    
    // 标记是否处于垂直滚动状态
    // Flag to track if we're in a vertical scroll
    private var isVerticalScrolling = false
    
    // 标记是否处于水平滚动状态
    // Flag to track if we're in a horizontal scroll
    private var isHorizontalScrolling = false
    
    // 初始触摸坐标
    // Initial touch coordinates
    private var initialX = 0f
    private var initialY = 0f
    
    // 上一次触摸坐标，用于计算滑动速度
    // Last touch coordinates for velocity calculation
    private var lastX = 0f
    private var lastY = 0f
    
    // 记录最后一次触摸按下的时间戳，用于实现滚动锁定周期
    // Store time of last touch down to enforce scroll locking period
    private var touchDownTime = 0L
    
    // 缓存父级 ViewPager2 引用，避免重复查找
    // Parent ViewPager2 reference to avoid repeated lookups
    private var cachedViewPager: ViewPager2? = null
    
    // 缓存子级 RecyclerView 引用，避免重复查找
    // Child RecyclerView reference to avoid repeated lookups
    private var cachedRecyclerView: RecyclerView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 当附加到窗口时缓存引用
        // Cache references when attached to window
        cachedViewPager = findParentViewPager()
        cachedRecyclerView = findChildRecyclerView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 当从窗口分离时清除引用
        // Clear references when detached from window
        cachedViewPager = null
        cachedRecyclerView = null
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        handleTouch(ev)
        return super.onInterceptTouchEvent(ev)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        handleTouch(ev)
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 处理触摸事件，判断滑动方向并相应地启用/禁用 ViewPager2 滑动
     * Handle touch events, determine scroll direction, and enable/disable ViewPager2 swiping accordingly
     */
    private fun handleTouch(ev: MotionEvent): Boolean {
        val viewPager = cachedViewPager ?: findParentViewPager()
        
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 新触摸序列开始时重置所有标记
                // Reset all flags at start of new touch sequence
                initialX = ev.x
                initialY = ev.y
                lastX = ev.x
                lastY = ev.y
                isVerticalScrolling = false
                isHorizontalScrolling = false
                touchDownTime = System.currentTimeMillis()
                
                // 初始化时启用 ViewPager2 滑动
                // Initially enable ViewPager2 swiping
                viewPager?.isUserInputEnabled = true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val diffX = ev.x - initialX
                val diffY = ev.y - initialY
                
                // 计算速度以检测快速滑动手势
                // Calculate velocity to detect flick gestures
                val deltaX = ev.x - lastX
                val deltaY = ev.y - lastY
                lastX = ev.x
                lastY = ev.y
                
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
                            viewPager?.isUserInputEnabled = false
                            log("检测到垂直滚动，禁用 ViewPager2")
                        } else if (abs(diffX) > abs(diffY) * DIRECTION_DETERMINATION_FACTOR) {
                            isHorizontalScrolling = true
                            viewPager?.isUserInputEnabled = true
                            log("检测到水平滚动，启用 ViewPager2")
                        }
                    }
                }
                
                // 如果在锁定期内且正在垂直移动，则保持 ViewPager 禁用状态
                // If we're within the lock period and moving vertically, keep ViewPager disabled
                if (isInLockPeriod && abs(diffY) > touchSlop && !isHorizontalScrolling) {
                    viewPager?.isUserInputEnabled = false
                }
                
                // 处理确定处于垂直滚动模式的情况
                // Handle the case when we're definitely in vertical scrolling mode
                if (isVerticalScrolling) {
                    viewPager?.isUserInputEnabled = false
                }
                
                // 在水平滚动期间，确保 ViewPager 启用
                // During horizontal scrolling, make sure ViewPager is enabled
                if (isHorizontalScrolling) {
                    viewPager?.isUserInputEnabled = true
                }
                
                // 额外的安全措施，防止 ViewPager2 在对角线滚动中接管
                // Additional safety to prevent ViewPager2 from taking over on diagonal scrolls
                if (!isHorizontalScrolling && abs(diffY) > touchSlop) {
                    viewPager?.isUserInputEnabled = false
                }
                
                // 检查子 RecyclerView 是否可以垂直滚动
                // Check if child RecyclerView can scroll vertically
                val recyclerView = cachedRecyclerView ?: findChildRecyclerView()
                if (recyclerView?.canScrollVertically(if (diffY < 0) 1 else -1) == true) {
                    // 如果 RecyclerView 可以在用户移动的方向上滚动，则禁用 ViewPager2
                    // If RecyclerView can scroll in the direction user is moving, disable ViewPager2
                    if (abs(diffY) > touchSlop) {
                        viewPager?.isUserInputEnabled = false
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 触摸序列结束时重置标记
                // Reset flags at end of touch sequence
                isVerticalScrolling = false
                isHorizontalScrolling = false
                
                // 触摸序列结束后重新启用 ViewPager2
                // Re-enable ViewPager2 after touch sequence ends
                viewPager?.isUserInputEnabled = true
            }
        }
        
        return false
    }

    /**
     * 查找父级 ViewPager2
     * Find parent ViewPager2
     */
    private fun findParentViewPager(): ViewPager2? {
        var parent = parent
        while (parent != null) {
            if (parent is ViewPager2) {
                cachedViewPager = parent
                return parent
            }
            parent = parent.parent
        }
        return null
    }
    
    /**
     * 查找子级 RecyclerView 或包含 RecyclerView 的 SmartRefreshLayout
     * Find a child RecyclerView or SmartRefreshLayout that contains a RecyclerView
     */
    private fun findChildRecyclerView(viewGroup: ViewGroup = this): RecyclerView? {
        if (cachedRecyclerView != null) {
            return cachedRecyclerView
        }
        
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            
            if (child is RecyclerView) {
                cachedRecyclerView = child
                return child
            } else if (child is SmartRefreshLayout) {
                // 检查 SmartRefreshLayout 是否包含 RecyclerView
                // Check if SmartRefreshLayout contains a RecyclerView
                for (j in 0 until child.childCount) {
                    val refreshChild = child.getChildAt(j)
                    if (refreshChild is RecyclerView) {
                        cachedRecyclerView = refreshChild
                        return refreshChild
                    }
                }
            } else if (child is ViewGroup) {
                val result = findChildRecyclerView(child)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }
}
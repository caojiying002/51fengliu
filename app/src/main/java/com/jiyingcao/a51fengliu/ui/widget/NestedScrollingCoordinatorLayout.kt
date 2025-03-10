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
 * A custom layout that helps coordinate nested scrolling between ViewPager2 and RecyclerView
 * to avoid accidental horizontal swipes when scrolling vertically in a nested RecyclerView
 */
class NestedScrollingCoordinatorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "NestedScrollingCoord"
        
        // 增加垂直方向的优先级系数，使垂直判断更敏感
        private const val VERTICAL_BIAS = 1.5f
        
        // 在开始滑动后的一段时间内，更积极地阻止ViewPager2横向滑动
        private const val SCROLL_LOCK_DURATION_MS = 200L
    }

    // Touch slop is the minimum distance to consider a touch as a scroll
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    
    // Flag to track if we're in a vertical scroll
    private var isVerticalScrolling = false
    
    // Flag to track if we're in a horizontal scroll
    private var isHorizontalScrolling = false
    
    // Initial touch coordinates
    private var initialX = 0f
    private var initialY = 0f
    
    // Last touch coordinates for velocity calculation
    private var lastX = 0f
    private var lastY = 0f
    
    // Store time of last touch down to enforce scroll locking period
    private var touchDownTime = 0L
    
    // Parent ViewPager2 reference to avoid repeated lookups
    private var cachedViewPager: ViewPager2? = null
    
    // Child RecyclerView reference to avoid repeated lookups
    private var cachedRecyclerView: RecyclerView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Cache references when attached to window
        cachedViewPager = findParentViewPager()
        cachedRecyclerView = findChildRecyclerView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
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

    private fun handleTouch(ev: MotionEvent): Boolean {
        val viewPager = cachedViewPager ?: findParentViewPager()
        
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Reset all flags at start of new touch sequence
                initialX = ev.x
                initialY = ev.y
                lastX = ev.x
                lastY = ev.y
                isVerticalScrolling = false
                isHorizontalScrolling = false
                touchDownTime = System.currentTimeMillis()
                
                // Initially enable ViewPager2 swiping
                viewPager?.isUserInputEnabled = true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val diffX = ev.x - initialX
                val diffY = ev.y - initialY
                
                // Calculate velocity to detect flick gestures
                val deltaX = ev.x - lastX
                val deltaY = ev.y - lastY
                lastX = ev.x
                lastY = ev.y
                
                // Apply vertical bias to make vertical detection more sensitive
                val biasedDiffY = diffY * VERTICAL_BIAS
                
                // Check if we're still in the scroll lock period
                val isInLockPeriod = (System.currentTimeMillis() - touchDownTime) < SCROLL_LOCK_DURATION_MS
                
                // Detect the scroll direction only if neither flag is set yet
                if (!isVerticalScrolling && !isHorizontalScrolling) {
                    // Check if we've moved enough to consider it a scroll
                    if (abs(diffY) > touchSlop || abs(diffX) > touchSlop) {
                        // Determine scroll direction based on both distance and velocity
                        if (abs(biasedDiffY) > abs(diffX) || abs(deltaY) > abs(deltaX) * 1.2f) {
                            isVerticalScrolling = true
                            viewPager?.isUserInputEnabled = false
                            Log.d(TAG, "Detected vertical scrolling, disabling ViewPager2")
                        } else if (abs(diffX) > abs(diffY) * 1.2f) {
                            isHorizontalScrolling = true
                            viewPager?.isUserInputEnabled = true
                            Log.d(TAG, "Detected horizontal scrolling, enabling ViewPager2")
                        }
                    }
                }
                
                // If we're within the lock period and moving vertically, keep ViewPager disabled
                if (isInLockPeriod && abs(diffY) > touchSlop && !isHorizontalScrolling) {
                    viewPager?.isUserInputEnabled = false
                }
                
                // Handle the case when we're definitely in vertical scrolling mode
                if (isVerticalScrolling) {
                    viewPager?.isUserInputEnabled = false
                }
                
                // During horizontal scrolling, make sure ViewPager is enabled
                if (isHorizontalScrolling) {
                    viewPager?.isUserInputEnabled = true
                }
                
                // Additional safety to prevent ViewPager2 from taking over on diagonal scrolls
                if (!isHorizontalScrolling && abs(diffY) > touchSlop) {
                    viewPager?.isUserInputEnabled = false
                }
                
                // Check if child RecyclerView can scroll vertically
                val recyclerView = cachedRecyclerView ?: findChildRecyclerView()
                if (recyclerView?.canScrollVertically(if (diffY < 0) 1 else -1) == true) {
                    // If RecyclerView can scroll in the direction user is moving, disable ViewPager2
                    if (abs(diffY) > touchSlop) {
                        viewPager?.isUserInputEnabled = false
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Reset flags at end of touch sequence
                isVerticalScrolling = false
                isHorizontalScrolling = false
                
                // Re-enable ViewPager2 after touch sequence ends
                viewPager?.isUserInputEnabled = true
            }
        }
        
        return false
    }

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
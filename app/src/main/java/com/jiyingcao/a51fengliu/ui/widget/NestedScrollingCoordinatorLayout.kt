package com.jiyingcao.a51fengliu.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
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

    // Touch slop is the minimum distance to consider a touch as a scroll
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    
    // Flag to track if we're in a vertical scroll
    private var isVerticalScrolling = false
    
    // Initial touch coordinates
    private var initialX = 0f
    private var initialY = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                isVerticalScrolling = false
                
                // Find the parent ViewPager2
                val viewPager = findParentViewPager()
                viewPager?.isUserInputEnabled = true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val diffX = ev.x - initialX
                val diffY = ev.y - initialY
                
                // Check if we're scrolling vertically by comparing x and y movement
                if (!isVerticalScrolling && abs(diffY) > abs(diffX) && abs(diffY) > touchSlop) {
                    isVerticalScrolling = true
                    
                    // Find the ViewPager2 and disable user input while vertical scrolling
                    val viewPager = findParentViewPager()
                    viewPager?.isUserInputEnabled = false
                    
                    return false // Let the child handle the event
                } else if (!isVerticalScrolling && abs(diffX) > abs(diffY) && abs(diffX) > touchSlop) {
                    isVerticalScrolling = false
                    
                    // Horizontal scroll, let ViewPager2 handle it
                    val viewPager = findParentViewPager()
                    viewPager?.isUserInputEnabled = true
                    
                    return false // Let the event propagate to ViewPager2
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isVerticalScrolling = false
                
                // Re-enable ViewPager2 after touch sequence ends
                val viewPager = findParentViewPager()
                viewPager?.isUserInputEnabled = true
            }
        }
        
        return super.onInterceptTouchEvent(ev)
    }

    private fun findParentViewPager(): ViewPager2? {
        var parent = parent
        while (parent != null) {
            if (parent is ViewPager2) {
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
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            
            if (child is RecyclerView) {
                return child
            } else if (child is SmartRefreshLayout) {
                // Check if SmartRefreshLayout contains a RecyclerView
                for (j in 0 until child.childCount) {
                    val refreshChild = child.getChildAt(j)
                    if (refreshChild is RecyclerView) {
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
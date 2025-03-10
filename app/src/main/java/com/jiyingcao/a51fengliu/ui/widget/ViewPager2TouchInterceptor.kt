package com.jiyingcao.a51fengliu.ui.widget

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.scwang.smart.refresh.layout.SmartRefreshLayout

/**
 * Utility class to help resolve touch conflicts between ViewPager2 and nested RecyclerView
 */
object ViewPager2TouchInterceptor {
    
    /**
     * Finds a RecyclerView inside a SmartRefreshLayout in the ViewPager2's current page
     * and adds touch conflict resolution.
     *
     * @param viewPager The ViewPager2 that contains pages
     */
    fun setupWithViewPager(viewPager: ViewPager2) {
        // Get access to the internal RecyclerView of ViewPager2
        val recyclerView = findViewPagerInternalRecyclerView(viewPager)
        
        recyclerView?.let {
            it.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                private var initialX = 0f
                private var initialY = 0f
                
                override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                    when (e.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            initialX = e.x
                            initialY = e.y
                            // Enable ViewPager2 swiping on touch down
                            viewPager.isUserInputEnabled = true
                            rv.parent.requestDisallowInterceptTouchEvent(false)
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dx = e.x - initialX
                            val dy = e.y - initialY
                            
                            // Check if we're scrolling vertically
                            if (Math.abs(dy) > Math.abs(dx)) {
                                // If the current page has a vertical RecyclerView or SmartRefreshLayout,
                                // disable ViewPager2 swiping
                                val hasVerticalScroll = hasVerticalScrollableView(viewPager)
                                viewPager.isUserInputEnabled = !hasVerticalScroll
                                if (hasVerticalScroll) {
                                    rv.parent.requestDisallowInterceptTouchEvent(true)
                                }
                            }
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            // Re-enable ViewPager2 swiping
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
    
    private fun findViewPagerInternalRecyclerView(viewPager: ViewPager2): RecyclerView? {
        return (viewPager.getChildAt(0) as? RecyclerView)
    }
    
    private fun hasVerticalScrollableView(viewPager: ViewPager2): Boolean {
        // Current page in ViewPager2
        val currentItemView = findCurrentItemView(viewPager)
        
        if (currentItemView != null) {
            return findVerticalScrollableViewInHierarchy(currentItemView)
        }
        
        return false
    }
    
    private fun findCurrentItemView(viewPager: ViewPager2): View? {
        val recyclerView = findViewPagerInternalRecyclerView(viewPager)
        
        return recyclerView?.findViewHolderForAdapterPosition(viewPager.currentItem)?.itemView
    }
    
    private fun findVerticalScrollableViewInHierarchy(view: View): Boolean {
        // Check if it's a RecyclerView with vertical scrolling
        if (view is RecyclerView && view.canScrollVertically(1)) {
            return true
        }
        
        // Check if it's a SmartRefreshLayout
        if (view is SmartRefreshLayout) {
            // Check if SmartRefreshLayout contains any scrollable child
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is RecyclerView && child.canScrollVertically(1)) {
                    return true
                }
            }
        }
        
        // Recursively search in ViewGroup hierarchy
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (findVerticalScrollableViewInHierarchy(view.getChildAt(i))) {
                    return true
                }
            }
        }
        
        return false
    }
}
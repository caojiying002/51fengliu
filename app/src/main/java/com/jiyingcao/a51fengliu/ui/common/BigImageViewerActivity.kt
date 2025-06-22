package com.jiyingcao.a51fengliu.ui.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.config.AppConfig.Network.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.databinding.ActivityBigImageViewerBinding
import com.jiyingcao.a51fengliu.glide.glideSaveImage
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.setContentViewWithSystemBarPaddings
import com.jiyingcao.a51fengliu.util.vibrate
import com.jiyingcao.a51fengliu.R
import coil3.load
import coil3.request.placeholder
import coil3.request.error
import io.getstream.photoview.PhotoView

class BigImageViewerActivity : BaseActivity() {
    private lateinit var binding: ActivityBigImageViewerBinding
    private val mAdapter = ImagePagerAdapter()
    private var clickedImageIndex: Int = 0 // 用户在DetailActivity中点击的图片索引
    private var currentImageIndex: Int = 0 // 当前显示的图片索引

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBigImageViewerBinding.inflate(layoutInflater)
        setContentViewWithSystemBarPaddings(binding.root)
        
        // 获取用户点击的图片索引，用于返回时的共享元素转场
        clickedImageIndex = intent.getIntExtra("CLICKED_IMAGE_INDEX", 0)
        currentImageIndex = intent.getIntExtra("INDEX", 0)
        
        setupBackPressHandler()
        setupViewPager2()
        displayImagesFromIntent(intent)
        updateCurrentPhotoViewTransitionName()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        displayImagesFromIntent(intent)
    }

    /**
     * 设置返回按键处理器，使用新的OnBackPressedDispatcher API
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithSharedElementTransition()
            }
        })
    }


    /**
     * 统一的返回处理方法，包含共享元素转场逻辑
     */
    private fun finishWithSharedElementTransition() {
        // 检查是否应该使用共享元素转场
        if (AppConfig.UI.SHARED_ELEMENT_TRANSITIONS_ENABLED && shouldUseSharedElementTransition()) {
            // 更新当前显示PhotoView的transition name以匹配当前显示的图片
            val currentPhotoView = getCurrentPhotoView()
            currentPhotoView?.transitionName = "shared_image_$currentImageIndex"

            // 设置返回时的共享元素数据
            val resultIntent = Intent().apply {
                putExtra("RETURN_IMAGE_INDEX", currentImageIndex)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finishAfterTransition()
        } else {
            finish()
        }
    }

    /**
     * 判断是否应该使用共享元素转场返回
     * 需要检查当前显示的图片是否已加载完成
     */
    private fun shouldUseSharedElementTransition(): Boolean {
        // 获取当前显示的PhotoView
        val currentPhotoView = getCurrentPhotoView() ?: return false
        
        // 检查图片是否已加载完成
        return isImageFullyLoaded(currentPhotoView)
    }

    /**
     * 获取当前显示的PhotoView
     */
    private fun getCurrentPhotoView(): PhotoView? {
        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(currentImageIndex) as? ImagePagerAdapter.ImageViewHolder
        return viewHolder?.photoView
    }

    /**
     * 检查PhotoView中的图片是否已完全加载
     */
    private fun isImageFullyLoaded(photoView: PhotoView): Boolean {
        val drawable = photoView.drawable
        if (drawable == null) return false
        
        // 检查是否是placeholder或错误图片
        val placeholderDrawable = getDrawable(R.drawable.placeholder)
        val errorDrawable = getDrawable(R.drawable.image_broken)
        
        return drawable != placeholderDrawable && drawable != errorDrawable
    }

    private fun setupViewPager2() {
        // 设置预加载的图片数量，最多不超过3
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                //title = "${position + 1}/${mAdapter.getItemCount()}"
                currentImageIndex = position
                
                // 更新当前页面PhotoView的transition name
                updateCurrentPhotoViewTransitionName()
            }
        })
        binding.viewPager.adapter = mAdapter
    }

    /**
     * 更新当前显示的PhotoView的transition name
     * 清除其他PhotoView的transitionName，确保只有当前PhotoView有transitionName
     */
    private fun updateCurrentPhotoViewTransitionName() {
        // 延迟执行，确保页面切换完成
        binding.viewPager.post {
            // 清除所有PhotoView的transitionName
            clearAllPhotoViewTransitionNames()
            
            // 设置当前PhotoView的transitionName
            val currentPhotoView = getCurrentPhotoView()
            currentPhotoView?.transitionName = "shared_image_$currentImageIndex"
        }
    }

    /**
     * 清除所有PhotoView的transitionName，避免多个View使用相同的transitionName
     */
    private fun clearAllPhotoViewTransitionNames() {
        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView
        recyclerView?.let { rv ->
            // 遍历所有可见的ViewHolder
            for (i in 0 until rv.childCount) {
                val child = rv.getChildAt(i)
                val viewHolder = rv.getChildViewHolder(child) as? ImagePagerAdapter.ImageViewHolder
                viewHolder?.photoView?.transitionName = null
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun displayImagesFromIntent(intent: Intent?) {
        val images = intent?.getStringArrayListExtra("IMAGES") ?: return
        val currentIndex = intent.getIntExtra("INDEX", 0)

        mAdapter.apply {
            imageUrls.clear()
            imageUrls.addAll(images)
            notifyDataSetChanged()
        }
        binding.viewPager.setCurrentItem(currentIndex, false)
    }

    inner class ImagePagerAdapter() : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {
        val imageUrls: MutableList<String> = mutableListOf()
        val context: Context = this@BigImageViewerActivity

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val photoView: PhotoView = itemView.findViewById(R.id.photo_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val holder = ImageViewHolder(
                LayoutInflater.from(context).inflate(R.layout.page_photo_view, parent, false)
            )
            holder.photoView.setOnViewTapListener { _, _, _ ->
                finishWithSharedElementTransition()
            }
            return holder
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val imageUrl = BASE_IMAGE_URL + imageUrls[position]

            holder.photoView.setOnLongClickListener {
                vibrate(context)
                lifecycleScope.glideSaveImage(context, imageUrl)
                true
            }

            // 使用Coil3加载图片
            holder.photoView.load(imageUrl) {
                placeholder(R.drawable.placeholder)
                error(R.drawable.image_broken)
            }
        }

        override fun getItemCount(): Int = imageUrls.size
    }
}

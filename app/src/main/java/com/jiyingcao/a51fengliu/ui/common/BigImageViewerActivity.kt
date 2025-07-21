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
import coil3.SingletonImageLoader
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.config.AppConfig.Network.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.databinding.ActivityBigImageViewerBinding
import com.jiyingcao.a51fengliu.coil.coilSaveImageFromCache
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.setContentViewWithSystemBarPaddings
import com.jiyingcao.a51fengliu.util.vibrate
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.StoragePermissionHelper
import com.jiyingcao.a51fengliu.util.needsStoragePermission
import com.jiyingcao.a51fengliu.R
import coil3.load
import coil3.request.placeholder
import coil3.request.error
import coil3.request.ImageRequest
import coil3.request.target
import io.getstream.photoview.PhotoView

class BigImageViewerActivity : BaseActivity() {
    private lateinit var binding: ActivityBigImageViewerBinding
    private val mAdapter = ImagePagerAdapter()
    private var clickedImageIndex: Int = 0 // 用户在DetailActivity中点击的图片索引
    private var currentImageIndex: Int = 0 // 当前显示的图片索引
    
    // 追踪图片加载状态的Map，key为图片URL，value为是否加载成功
    private val imageLoadingStates = mutableMapOf<String, Boolean>()
    
    // 追踪图片磁盘缓存Key的Map，key为图片URL，value为diskCacheKey
    private val imageDiskCacheKeys = mutableMapOf<String, String>()
    
    // 标记是否已经开始了postponed的转场动画
    private var hasStartedPostponedTransition = false
    
    // 待保存的图片信息，在权限授予后使用
    private var pendingSaveImageUrl: String? = null
    private var pendingSaveDiskCacheKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBigImageViewerBinding.inflate(layoutInflater)
        setContentViewWithSystemBarPaddings(binding.root)
        
        if (AppConfig.UI.SHARED_ELEMENT_TRANSITIONS_ENABLED) {
            // 暂停进入转场动画，等待图片加载完成
            postponeEnterTransition()

            // 设置超时机制，防止无限等待
            binding.viewPager.postDelayed({
                startPostponedTransitionIfNeeded()
            }, 3000) // 3秒超时
        }
        
        // 获取用户点击的图片索引，用于返回时的共享元素转场
        clickedImageIndex = intent.getIntExtra("CLICKED_IMAGE_INDEX", 0)
        currentImageIndex = intent.getIntExtra("INDEX", 0)
        
        setupBackPressedCallback()
        setupViewPager2()
        displayImagesFromIntent(intent)
        
        // 设置初始的transitionName
        updateViewPagerTransitionName()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        displayImagesFromIntent(intent)
    }

    /**
     * 设置返回按键处理器，使用新的OnBackPressedDispatcher API
     */
    private fun setupBackPressedCallback() {
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
            // 确保ViewPager2的transitionName对应当前显示的图片
            updateViewPagerTransitionName()

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
        // 获取当前显示的图片URL
        val currentImageUrl = getCurrentImageUrl() ?: return false
        
        // 检查图片是否已加载完成
        return isImageFullyLoaded(currentImageUrl)
    }

    /**
     * 获取当前显示的图片URL
     */
    private fun getCurrentImageUrl(): String? {
        if (mAdapter.imageUrls.isEmpty() || currentImageIndex >= mAdapter.imageUrls.size) {
            return null
        }
        return BASE_IMAGE_URL + mAdapter.imageUrls[currentImageIndex]
    }

    /**
     * 检查指定URL的图片是否已完全加载
     * @param imageUrl 图片的完整URL
     * @return true如果图片已加载完成，false如果仍在加载或加载失败
     */
    private fun isImageFullyLoaded(imageUrl: String): Boolean {
        return imageLoadingStates[imageUrl] == true
    }

    /**
     * 开始postponed的转场动画（如果还没有开始的话）
     */
    private fun startPostponedTransitionIfNeeded() {
        if (AppConfig.UI.SHARED_ELEMENT_TRANSITIONS_ENABLED && !hasStartedPostponedTransition) {
            hasStartedPostponedTransition = true
            startPostponedEnterTransition()
        }
    }

    /**
     * 检查当前显示的图片是否已加载完成，如果是则开始转场动画
     */
    private fun checkCurrentImageLoadedAndStartTransition() {
        if (!AppConfig.UI.SHARED_ELEMENT_TRANSITIONS_ENABLED)
            return
        
        val currentImageUrl = getCurrentImageUrl()
        if (currentImageUrl != null && isImageFullyLoaded(currentImageUrl)) {
            startPostponedTransitionIfNeeded()
        }
    }

    /**
     * 更新ViewPager2的transitionName以匹配当前显示的图片
     */
    private fun updateViewPagerTransitionName() {
        binding.viewPager.transitionName = "shared_image_$currentImageIndex"
    }

    private fun setupViewPager2() {
        // 设置预加载的图片数量，最多不超过3
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                //title = "${position + 1}/${mAdapter.getItemCount()}"
                currentImageIndex = position
                
                // 更新ViewPager2的transitionName以匹配当前显示的图片
                updateViewPagerTransitionName()
            }
        })
        binding.viewPager.adapter = mAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun displayImagesFromIntent(intent: Intent?) {
        val images = intent?.getStringArrayListExtra("IMAGES") ?: return
        val currentIndex = intent.getIntExtra("INDEX", 0)

        // 清空之前的加载状态
        imageLoadingStates.clear()
        imageDiskCacheKeys.clear()

        mAdapter.apply {
            imageUrls.clear()
            imageUrls.addAll(images)
            notifyDataSetChanged()
        }
        binding.viewPager.setCurrentItem(currentIndex, false)
        currentImageIndex = currentIndex
        
        // 只有在启用转场动画时才延迟检查当前图片是否已加载
        if (AppConfig.UI.SHARED_ELEMENT_TRANSITIONS_ENABLED) {
            // 延迟检查当前图片是否已加载（可能在缓存中）
            binding.viewPager.post {
                checkCurrentImageLoadedAndStartTransition()
            }
        }
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
                
                // 检查图片是否已加载成功
                if (imageLoadingStates[imageUrl] == true) {
                    // 获取磁盘缓存Key
                    val diskCacheKey = imageDiskCacheKeys[imageUrl]
                    if (diskCacheKey != null) {
                        // 使用新的权限处理方法保存图片到相册
                        saveImageToAlbum(imageUrl, diskCacheKey)
                    } else {
                        context.showToast("图片缓存不可用")
                    }
                } else {
                    context.showToast("请等待图片加载完成")
                }
                true
            }

            // 初始化加载状态为false
            imageLoadingStates[imageUrl] = false

            // 使用Coil加载图片并监听加载状态
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .target(holder.photoView)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.image_broken)
                .listener(
                    onStart = {
                        // 开始加载
                        imageLoadingStates[imageUrl] = false
                    },
                    onSuccess = { _, result ->
                        // 加载成功
                        imageLoadingStates[imageUrl] = true
                        
                        // 保存磁盘缓存Key
                        result.diskCacheKey?.let { diskCacheKey ->
                            imageDiskCacheKeys[imageUrl] = diskCacheKey
                        }
                        
                        // 如果这是当前显示的图片，检查是否应该开始转场动画
                        if (AppConfig.UI.SHARED_ELEMENT_TRANSITIONS_ENABLED && position == currentImageIndex) {
                            checkCurrentImageLoadedAndStartTransition()
                        }
                    },
                    onError = { _, _ ->
                        // 加载失败，也启动转场动画，避免无限等待
                        imageLoadingStates[imageUrl] = false
                        
                        // 如果这是当前显示的图片，也要开始转场动画
                        if (AppConfig.UI.SHARED_ELEMENT_TRANSITIONS_ENABLED && position == currentImageIndex) {
                            startPostponedTransitionIfNeeded()
                        }
                    }
                )
                .build()

            // 执行图片加载请求
            SingletonImageLoader.get(context).enqueue(request)
        }

        override fun getItemCount(): Int = imageUrls.size
    }
    
    /**
     * 保存图片到相册，包含权限检查和申请
     */
    private fun saveImageToAlbum(imageUrl: String, diskCacheKey: String) {
        if (needsStoragePermission(this)) {
            // 需要权限，待获取权限成功后保存
            pendingSaveImageUrl = imageUrl
            pendingSaveDiskCacheKey = diskCacheKey
            
            if (StoragePermissionHelper.shouldShowRequestPermissionRationale(this)) {
                showToast("需要存储权限才能保存图片到相册")
            }
            StoragePermissionHelper.requestStoragePermission(this)
        } else {
            // 已有权限，直接保存
            lifecycleScope.coilSaveImageFromCache(this, imageUrl, diskCacheKey)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        StoragePermissionHelper.handlePermissionResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                // 获取到权限，执行待保存的操作
                val imageUrl = pendingSaveImageUrl
                val diskCacheKey = pendingSaveDiskCacheKey
                if (imageUrl != null && diskCacheKey != null) {
                    lifecycleScope.coilSaveImageFromCache(this, imageUrl, diskCacheKey)
                }
                pendingSaveImageUrl = null
                pendingSaveDiskCacheKey = null
            },
            onDenied = {
                showToast("没有存储权限，无法保存图片")
                pendingSaveImageUrl = null
                pendingSaveDiskCacheKey = null
            }
        )
    }
}
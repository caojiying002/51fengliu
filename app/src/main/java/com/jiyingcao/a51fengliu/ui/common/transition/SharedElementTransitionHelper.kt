package com.jiyingcao.a51fengliu.ui.common.transition

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.isVisible
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.config.AppConfig.Network.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import coil3.request.placeholder
import coil3.request.error
import coil3.request.target
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.util.dp

/**
 * 共享元素转场Helper
 * 
 * 封装所有图片加载和共享元素转场相关逻辑，提供高度可复用的API
 * 适用于任何需要展示图片网格并支持转场到BigImageViewerActivity的场景
 * 
 * 使用案例:
 * ```
 * // 在Activity中
 * private val transitionHelper = SharedElementTransitionHelper(this)
 * 
 * // 加载图片
 * transitionHelper.loadImagesIntoGrid(
 *     imageContainer = binding.imageContainer,
 *     imageUrls = record.getPictures(),
 *     cornerRadius = 4.dp,
 *     onImageClick = { clickedIndex, allImages ->
 *         // 可以在这里添加权限检查等业务逻辑
 *         if (hasPermission()) {
 *             transitionHelper.startImageViewer(allImages, clickedIndex)
 *         }
 *     }
 * )
 * ```
 */
class SharedElementTransitionHelper(private val activity: Activity) {
    
    // 图片加载状态管理器
    private val imageLoadingManager = ImageLoadingManager()
    
    /**
     * 图片网格配置
     */
    data class ImageGridConfig(
        val cornerRadius: Int = 4.dp,
        val maxImageCount: Int = 4,
        val placeholder: Int = R.drawable.placeholder,
        val errorDrawable: Int = R.drawable.image_broken
    )
    
    /**
     * 为图片网格容器加载图片并设置转场动画
     * 
     * @param imageContainer 包含4个ImageView的容器 (image_0, image_1, image_2, image_3)
     * @param imageUrls 图片URL列表（相对路径）
     * @param config 图片网格配置
     * @param onImageClick 图片点击回调，参数为(点击的图片索引, 所有图片URL列表)
     */
    fun loadImagesIntoGrid(
        imageContainer: android.view.ViewGroup,
        imageUrls: List<String>,
        config: ImageGridConfig = ImageGridConfig(),
        onImageClick: ((clickedIndex: Int, allImages: List<String>) -> Unit)? = null
    ) {
        if (imageUrls.isEmpty()) {
            imageContainer.isVisible = false
            return
        }

        // 清空之前的加载状态
        imageLoadingManager.clear()
        imageContainer.isVisible = true

        for (index in 0 until config.maxImageCount) {
            val imageView = getImageViewByIndex(imageContainer, index) ?: continue
            
            // 设置共享元素转场名称
            imageView.transitionName = "shared_image_$index"
            
            val imageUrl = imageUrls.getOrNull(index)
            if (imageUrl.isNullOrBlank()) {
                imageView.isVisible = false
                continue
            }

            imageView.isVisible = true
            val fullUrl = BASE_IMAGE_URL + imageUrl

            // 加载图片
            loadImageWithStatusTracking(imageView, fullUrl, config)
            
            // 设置点击事件
            imageView.setOnClickListener {
                onImageClick?.invoke(index, imageUrls)
            }
        }
    }

    /**
     * 启动BigImageViewerActivity，根据图片加载状态自动决定是否使用共享元素转场
     * 
     * @param imageUrls 所有图片URL列表
     * @param clickedIndex 用户点击的图片索引
     * @param imageContainer 图片容器，用于获取共享元素
     */
    fun startImageViewer(
        imageUrls: List<String>, 
        clickedIndex: Int,
        imageContainer: android.view.ViewGroup? = null
    ) {
        val intent = Intent(activity, BigImageViewerActivity::class.java).apply {
            putStringArrayListExtra("IMAGES", ArrayList(imageUrls))
            putExtra("INDEX", clickedIndex)
            putExtra("CLICKED_IMAGE_INDEX", clickedIndex)
        }

        if (shouldUseSharedElementTransition(imageUrls[clickedIndex]) && imageContainer != null) {
            val sharedElements = createSharedElementPairs(imageContainer, imageUrls)
            if (sharedElements.isNotEmpty()) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    *sharedElements.toTypedArray()
                )
                activity.startActivity(intent, options.toBundle())
                return
            }
        }
        
        // 回退到普通启动
        activity.startActivity(intent)
    }

    /**
     * 创建便捷的扩展函数，进一步简化API使用
     */
    fun ImageView.loadAndSetupTransition(
        imageUrl: String,
        index: Int,
        allImages: List<String>,
        config: ImageGridConfig = ImageGridConfig(),
        onImageClick: (() -> Unit)? = null
    ) {
        transitionName = "shared_image_$index"
        val fullUrl = BASE_IMAGE_URL + imageUrl
        
        loadImageWithStatusTracking(this, fullUrl, config)
        
        setOnClickListener {
            onImageClick?.invoke()
        }
    }

    // ==================== 私有方法 ====================

    private fun loadImageWithStatusTracking(
        imageView: ImageView, 
        fullUrl: String, 
        config: ImageGridConfig
    ) {
        imageLoadingManager.setLoading(fullUrl)

        val request = ImageRequest.Builder(activity)
            .data(fullUrl)
            .target(imageView)
            .placeholder(config.placeholder)
            .error(config.errorDrawable)
            .transformations(RoundedCornersTransformation(config.cornerRadius.toFloat()))
            .listener(
                onStart = {
                    imageLoadingManager.setLoading(fullUrl)
                },
                onSuccess = { _, _ ->
                    imageLoadingManager.setLoaded(fullUrl)
                },
                onError = { _, _ ->
                    imageLoadingManager.setError(fullUrl)
                }
            )
            .build()

        SingletonImageLoader.get(activity).enqueue(request)
    }

    private fun shouldUseSharedElementTransition(subUrl: String): Boolean {
        if (!AppConfig.UI.SHARED_ELEMENT_TRANSITIONS_ENABLED) return false
        
        // 检查图片是否加载成功
        val fullUrl = BASE_IMAGE_URL + subUrl
        return  imageLoadingManager.isLoaded(fullUrl)
    }

    private fun createSharedElementPairs(
        imageContainer: ViewGroup,
        imageUrls: List<String>
    ): List<Pair<View, String>> {
        val sharedElements = mutableListOf<Pair<View, String>>()
        
        for (i in 0 until minOf(4, imageUrls.size)) {
            val imageView = getImageViewByIndex(imageContainer, i)
            if (imageView?.isVisible == true) {
                sharedElements.add(Pair.create(imageView, "shared_image_$i"))
            }
        }
        
        return sharedElements
    }

    private fun getImageViewByIndex(container: ViewGroup, index: Int): ImageView? {
        return when (index) {
            0 -> container.findViewById(R.id.image_0)
            1 -> container.findViewById(R.id.image_1)
            2 -> container.findViewById(R.id.image_2)
            3 -> container.findViewById(R.id.image_3)
            else -> null
        }
    }
}

/**
 * 图片加载状态管理器
 * 独立封装图片加载状态追踪逻辑，便于测试和复用
 */
private class ImageLoadingManager {
    private val loadingStates = mutableMapOf<String, LoadingState>()
    
    private enum class LoadingState {
        LOADING, LOADED, ERROR
    }
    
    fun setLoading(url: String) {
        loadingStates[url] = LoadingState.LOADING
    }
    
    fun setLoaded(url: String) {
        loadingStates[url] = LoadingState.LOADED
    }
    
    fun setError(url: String) {
        loadingStates[url] = LoadingState.ERROR
    }
    
    fun isLoaded(url: String): Boolean {
        return loadingStates[url] == LoadingState.LOADED
    }
    
    fun clear() {
        loadingStates.clear()
    }
}

// ==================== Activity扩展函数 ====================

/**
 * Activity扩展函数，提供最简洁的API
 */
fun Activity.createImageTransitionHelper(): SharedElementTransitionHelper {
    return SharedElementTransitionHelper(this)
}

/**
 * 便捷方法：直接为RecordInfo类型加载图片
 */
fun SharedElementTransitionHelper.loadRecordImages(
    imageContainer: ViewGroup,
    record: RecordInfo,
    onImageClick: (clickedIndex: Int) -> Unit = {}
) {
    loadImagesIntoGrid(
        imageContainer = imageContainer,
        imageUrls = record.getPictures(),
        onImageClick = { clickedIndex, allImages ->
            onImageClick(clickedIndex)
        }
    )
}

/**
 * 便捷方法：直接为Merchant类型加载图片
 */
fun SharedElementTransitionHelper.loadMerchantImages(
    imageContainer: ViewGroup,
    merchant: Merchant,
    onImageClick: (clickedIndex: Int) -> Unit = {}
) {
    loadImagesIntoGrid(
        imageContainer = imageContainer,
        imageUrls = merchant.getPictures(),
        onImageClick = { clickedIndex, allImages ->
            onImageClick(clickedIndex)
        }
    )
}
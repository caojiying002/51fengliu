package com.jiyingcao.a51fengliu.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.glide.HostInvariantGlideUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 统一的图片加载工具类
 * 所有图片加载都必须通过此类进行，确保使用 HostInvariantGlideUrl
 */
object ImageLoader {

    /**
     * 加载图片到 ImageView，支持圆角
     *
     * @param imageView 目标ImageView
     * @param url 图片URL（相对或绝对URL）
     * @param cornerRadius 圆角半径，单位dp，默认4dp
     * @param placeholder 占位图资源ID
     * @param errorImage 错误图资源ID
     * @param centerCrop 是否使用CenterCrop缩放模式
     * @param useTransition 是否使用过渡动画
     */
    fun load(
        imageView: ImageView,
        url: String,
        cornerRadius: Int = 4,
        placeholder: Int = R.drawable.placeholder,
        errorImage: Int = R.drawable.image_broken,
        centerCrop: Boolean = true,
        useTransition: Boolean = false
    ) {
        val glideUrl = createGlideUrl(url)
        val requestBuilder = GlideApp.with(imageView.context)
            .load(glideUrl)
            .placeholder(placeholder)
            .error(errorImage)

        // 添加转换
        if (centerCrop && cornerRadius > 0) {
            // 对于同时需要centerCrop和圆角的情况，使用apply()方法链式调用
            requestBuilder.centerCrop().transform(RoundedCorners(cornerRadius.dp))
        } else if (centerCrop) {
            requestBuilder.centerCrop()
        } else if (cornerRadius > 0) {
            requestBuilder.transform(RoundedCorners(cornerRadius.dp))
        }

        // 应用过渡效果
        if (useTransition) {
            requestBuilder.transition(DrawableTransitionOptions.withCrossFade())
        }

        requestBuilder.into(imageView)
    }

    /**
     * 加载图片到 ImageView 并添加一个请求监听器
     *
     * @param imageView 目标ImageView
     * @param url 图片URL（相对或绝对URL）
     * @param cornerRadius 圆角半径，单位dp
     * @param placeholder 占位图资源ID
     * @param errorImage 错误图资源ID
     * @param listener 请求监听器
     * @param centerCrop 是否使用CenterCrop缩放模式
     * @param useTransition 是否使用过渡动画
     */
    fun load(
        imageView: ImageView,
        url: String,
        listener: RequestListener<Drawable>,
        cornerRadius: Int = 4,
        placeholder: Int = R.drawable.placeholder,
        errorImage: Int = R.drawable.image_broken,
        centerCrop: Boolean = true,
        useTransition: Boolean = false
    ) {
        val glideUrl = createGlideUrl(url)
        val requestBuilder = GlideApp.with(imageView.context)
            .load(glideUrl)
            .placeholder(placeholder)
            .error(errorImage)
            .listener(listener)

        // 添加转换
        if (centerCrop && cornerRadius > 0) {
            // 对于同时需要centerCrop和圆角的情况，使用apply()方法链式调用
            requestBuilder.centerCrop().transform(RoundedCorners(cornerRadius.dp))
        } else if (centerCrop) {
            requestBuilder.centerCrop()
        } else if (cornerRadius > 0) {
            requestBuilder.transform(RoundedCorners(cornerRadius.dp))
        }

        // 应用过渡效果
        if (useTransition) {
            requestBuilder.transition(DrawableTransitionOptions.withCrossFade())
        }

        requestBuilder.into(imageView)
    }

    /**
     * 简化版的加载方法，用于大图查看器等场景
     *
     * @param imageView 目标ImageView
     * @param url 图片URL（相对或绝对URL）
     * @param listener 可选的请求监听器
     */
    fun loadOriginal(
        imageView: ImageView,
        url: String,
        listener: RequestListener<Drawable>? = null
    ) {
        val glideUrl = createGlideUrl(url)
        val requestBuilder = GlideApp.with(imageView.context)
            .load(glideUrl)

        if (listener != null) {
            requestBuilder.listener(listener)
        }

        requestBuilder.into(imageView)
    }

    /**
     * 居中裁剪加载方法，用于报告对话框等场景
     *
     * @param imageView 目标ImageView
     * @param url 图片URL（相对或绝对URL）
     */
    fun loadCenterCrop(
        imageView: ImageView,
        url: String
    ) {
        val glideUrl = createGlideUrl(url)
        GlideApp.with(imageView.context)
            .load(glideUrl)
            .centerCrop()
            .into(imageView)
    }

    /**
     * 下载图片文件
     *
     * @param context 上下文
     * @param url 图片URL（相对或绝对URL）
     * @param target 自定义目标，用于处理下载的文件
     */
    fun downloadOnly(
        context: Context,
        url: String,
        target: CustomTarget<File>
    ) {
        val glideUrl = createGlideUrl(url)
        GlideApp.with(context)
            .downloadOnly()
            .load(glideUrl)
            .into(target)
    }

    /**
     * 保存图片到相册
     *
     * @param context 上下文
     * @param imageUrl 图片URL（相对或绝对URL）
     * @param subFolder 子文件夹名称
     * @param scope 可选的协程作用域
     */
    fun saveToGallery(
        context: Context,
        imageUrl: String,
        subFolder: String? = AppConfig.Storage.IMAGE_SUB_FOLDER,
        scope: CoroutineScope? = null
    ) {
        val actualScope = scope ?: CoroutineScope(Dispatchers.IO)
        val glideUrl = createGlideUrl(imageUrl)
        
        // 提取文件名
        val fileName = extractFileNameFromUrl(imageUrl)
        
        downloadOnly(
            context,
            imageUrl,
            object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: com.bumptech.glide.request.transition.Transition<in File>?) {
                    actualScope.launch(Dispatchers.IO) {
                        val result = saveImage(context, resource, subFolder, fileName)
                        
                        withContext(Dispatchers.Main) {
                            when (result) {
                                is SaveImageResult.Success -> context.showToast("图片已保存")
                                is SaveImageResult.Error -> {
                                    context.showToast("图片保存失败: ${result.message}")
                                    result.exception?.printStackTrace()
                                }
                            }
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 清除占位符或其他资源
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    context.showToast("图片下载失败")
                }
            }
        )
    }

    /**
     * CoroutineScope 的扩展函数版本，作为语法糖
     */
    fun CoroutineScope.saveToGallery(
        context: Context,
        imageUrl: String,
        subFolder: String? = AppConfig.Storage.IMAGE_SUB_FOLDER
    ) = saveToGallery(context, imageUrl, subFolder, this)

    /**
     * 创建 HostInvariantGlideUrl 对象
     * 
     * @param url 原始URL，可以是相对路径或完整URL
     * @return HostInvariantGlideUrl 实例
     */
    private fun createGlideUrl(url: String): HostInvariantGlideUrl {
        // 确保URL是完整的URL
        val fullUrl = if (url.startsWith("http")) {
            url
        } else {
            AppConfig.Network.BASE_IMAGE_URL + url
        }
        return HostInvariantGlideUrl(fullUrl)
    }

    /**
     * 从URL中提取文件名
     */
    private fun extractFileNameFromUrl(url: String): String {
        val path = try {
            java.net.URL(url).path
        } catch (e: Exception) {
            url
        }
        return java.io.File(path).name
    }
}
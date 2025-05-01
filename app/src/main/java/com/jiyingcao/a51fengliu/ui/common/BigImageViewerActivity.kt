package com.jiyingcao.a51fengliu.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jiyingcao.a51fengliu.config.AppConfig.Network.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.databinding.ActivityBigImageViewerBinding
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.glide.glideSaveImage
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.setContentViewWithSystemBarPaddings
import com.jiyingcao.a51fengliu.util.vibrate
import com.jiyingcao.a51fengliu.R
import io.getstream.photoview.PhotoView
import kotlin.collections.set
import kotlin.math.min

class BigImageViewerActivity : BaseActivity() {
    private lateinit var binding: ActivityBigImageViewerBinding
    private val mAdapter = ImagePagerAdapter()

    private val mySharedElementCallback = object : SharedElementCallback() {
        var currentPosition = 0

        override fun onMapSharedElements(
            names: List<String>,
            sharedElements: MutableMap<String, View>
        ) {
            // 获取当前页面的PhotoView
            val internalRecyclerView = binding.viewPager.getChildAt(0) as RecyclerView
            val currentPhotoView = internalRecyclerView.findViewHolderForAdapterPosition(currentPosition)
                ?.itemView?.findViewById<PhotoView>(R.id.photo_view)

            if (currentPhotoView != null) {
                // 更新共享元素映射
                sharedElements.clear()
                sharedElements["image$currentPosition"] = currentPhotoView
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBigImageViewerBinding.inflate(layoutInflater)
        setContentViewWithSystemBarPaddings(binding.root)

        // 延迟共享元素转场
        supportPostponeEnterTransition()

        setEnterSharedElementCallback(mySharedElementCallback)

        setupViewPager2()

        displayImagesFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        displayImagesFromIntent(intent)
    }

    override fun finishAfterTransition() {
        val intent = Intent().apply {
            putExtra("RESULT_INDEX", binding.viewPager.currentItem)
        }
        setResult(RESULT_OK, intent)
        super.finishAfterTransition()
    }

    private fun setupViewPager2() {
        // 设置预加载的图片数量，最多不超过3
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                //title = "${position + 1}/${mAdapter.getItemCount()}"
                // 切换页面时更新共享元素动画的key值
                mySharedElementCallback.currentPosition = position
            }
        })
        binding.viewPager.adapter = mAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun displayImagesFromIntent(intent: Intent?) {
        val images = intent?.getStringArrayListExtra("IMAGES") ?: return
        val currentIndex = intent.getIntExtra("INDEX", 0)

        // 设置共享元素动画的key值
        mySharedElementCallback.currentPosition = currentIndex

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
            // 避免转场动画没有启动过（如果启动过则no-op）
            supportPostponeEnterTransition()
            // 关闭Activity时的转场动画
            supportFinishAfterTransition()
        }
        return holder
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = BASE_IMAGE_URL + imageUrls[position]

        holder.photoView.transitionName = "image$position"
        holder.photoView.setOnLongClickListener {
            vibrate(context)
            glideSaveImage(context, imageUrl)
            true
        }

        GlideApp.with(context)
            .load(imageUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                    // 当图片加载失败时，也开始转场动画
                    holder.photoView.post(::supportStartPostponedEnterTransition)
                    //supportStartPostponedEnterTransition()
                    return false
                }

                override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    // 当图片加载完成时，开始转场动画
                    holder.photoView.post(::supportStartPostponedEnterTransition)
                    //supportStartPostponedEnterTransition()
                    return false
                }
            })
            .into(holder.photoView)
    }

    override fun getItemCount(): Int = imageUrls.size
}
}

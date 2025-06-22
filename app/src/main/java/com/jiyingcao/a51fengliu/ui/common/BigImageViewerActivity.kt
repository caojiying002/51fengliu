package com.jiyingcao.a51fengliu.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.jiyingcao.a51fengliu.config.AppConfig.Network.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.databinding.ActivityBigImageViewerBinding
import com.jiyingcao.a51fengliu.glide.glideSaveImage
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.setContentViewWithSystemBarPaddings
import com.jiyingcao.a51fengliu.util.vibrate
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.util.ImageLoader
import io.getstream.photoview.PhotoView

class BigImageViewerActivity : BaseActivity() {
    private lateinit var binding: ActivityBigImageViewerBinding
    private val mAdapter = ImagePagerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBigImageViewerBinding.inflate(layoutInflater)
        setContentViewWithSystemBarPaddings(binding.root)
        setupViewPager2()
        displayImagesFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        displayImagesFromIntent(intent)
    }

    private fun setupViewPager2() {
        // 设置预加载的图片数量，最多不超过3
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                //title = "${position + 1}/${mAdapter.getItemCount()}"
            }
        })
        binding.viewPager.adapter = mAdapter
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
            finish()
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

        // 使用ImageLoader
        ImageLoader.loadOriginal(
            imageView = holder.photoView,
            url = imageUrls[position], // Use the relative URL directly
        )
    }

    override fun getItemCount(): Int = imageUrls.size
}
}

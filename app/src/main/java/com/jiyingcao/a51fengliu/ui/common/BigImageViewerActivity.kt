package com.jiyingcao.a51fengliu.ui.common

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.CustomViewTarget
import com.jiyingcao.a51fengliu.databinding.ActivityBigImageViewerBinding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.glide.glideSaveImage
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.setContentViewWithSystemBarPaddings
import com.jiyingcao.a51fengliu.util.vibrate
import io.getstream.photoview.PhotoView

class BigImageViewerActivity : BaseActivity() {
    private lateinit var binding: ActivityBigImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBigImageViewerBinding.inflate(layoutInflater)
        setContentViewWithSystemBarPaddings(binding.root)

        // 确保启用了共享元素转场
        supportPostponeEnterTransition()

        displayImagesFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        displayImagesFromIntent(intent)
    }

    private fun displayImagesFromIntent(intent: Intent?) {
        val images = intent?.getStringArrayListExtra("IMAGES") ?: return
        val currentIndex = intent.getIntExtra("INDEX", 0)
        binding.viewPager.adapter = ImagePagerAdapter(images, this)
        binding.viewPager.setCurrentItem(currentIndex, false)
    }
}

private class ImagePagerAdapter(
    private val imageUrls: List<String>,
    private val context: Context
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView as PhotoView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val photoView = PhotoView(context)
        photoView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (context is BigImageViewerActivity) {
            photoView.setOnViewTapListener { _, _, _ ->
                context.finishAfterTransition()
            }
        }
        return ImageViewHolder(photoView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.photoView.transitionName = "image$position"

        // TODO 无网络时转场动画会导致界面卡死
        val customViewTarget = object : CustomViewTarget<PhotoView, Drawable>(holder.photoView) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
                //holder.photoView.setImageDrawable(errorDrawable)
            }

            override fun onResourceCleared(placeholder: Drawable?) {
                //holder.photoView.setImageDrawable(placeholder)
            }

            override fun onResourceReady(resource: Drawable, transition: com.bumptech.glide.request.transition.Transition<in Drawable>?) {
                holder.photoView.setImageDrawable(resource)
                if (context is BigImageViewerActivity) {
                    context.supportStartPostponedEnterTransition() // 当图片加载完成时，开始转场动画
                }
            }
        }

        val imageUrl = BASE_IMAGE_URL + imageUrls[position]
        GlideApp
            .with(context)
            .load(imageUrl)
            //.diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(customViewTarget)
        holder.photoView.setOnLongClickListener {
            vibrate(context)
            glideSaveImage(context, imageUrl)
            true
        }
    }

    override fun getItemCount(): Int = imageUrls.size
}

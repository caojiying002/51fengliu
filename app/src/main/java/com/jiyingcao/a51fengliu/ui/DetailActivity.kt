package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.app.ActivityOptionsCompat
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.ItemData
import com.jiyingcao.a51fengliu.api.response.Record
import com.jiyingcao.a51fengliu.api.toFullUrl
import com.jiyingcao.a51fengliu.databinding.ActivityDetailV2Binding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.dp

class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityDetailV2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityDetailV2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        //setEdgeToEdgePaddings(binding.root)

        val record = intent.getRecord()
        record?.let { updateUi(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val record = intent.getRecord()
        record?.let { updateUi(it) }
    }
    
    private fun updateUi(itemData: Record) {
        //displayImagesIfAny(itemData.file)
        displayImagesIfAnyV2(itemData.pictureList)

        binding.title.copyOnLongClick()
        binding.dz.copyOnLongClick()
        binding.qq.copyOnLongClick()
        binding.wechat.copyOnLongClick()
        binding.phone.copyOnLongClick()
        binding.address.copyOnLongClick()

        binding.title.text = itemData.title
        binding.age.text = itemData.girlAge
        binding.price.text = itemData.consumeLv
        binding.process.text = itemData.desc
        binding.project.text = itemData.serveList
        binding.dz.text = itemData.cityCode // TODO 城市代码转换为城市名称
        binding.createTime.text = itemData.publishedAt // TODO 时间格式化
        binding.browse.text = itemData.viewCount
        binding.qq.text = itemData.qq
        binding.wechat.text = itemData.wechat
        binding.phone.text = itemData.phone
        binding.address.text = itemData.address
    }

    private fun displayImagesIfAnyV2(imgs: List<String>) {
        if (imgs.isEmpty()) {
            binding.imageContainer.visibility = GONE
            return
        }

        binding.imageContainer.visibility = VISIBLE
        // 从0到3循环
        for (index in 0..3) {
            val imageView = when (index) {
                0 -> binding.image0
                1 -> binding.image1
                2 -> binding.image2
                3 -> binding.image3
                else -> return
            }
            val subUrl = imgs.getOrNull(index)
            if (subUrl == null) {
                imageView.visibility = INVISIBLE
                continue
            }

            imageView.visibility = VISIBLE
            GlideApp.with(this)
                .load(BASE_IMAGE_URL + subUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.image_broken)
                .transform(CenterCrop(), RoundedCorners(4.dp))
                //.transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView)
            imageView.setOnClickListener {
                val intent = Intent(this, BigImageViewerActivity::class.java).apply {
                    putStringArrayListExtra("IMAGES", ArrayList(imgs))
                    putExtra("INDEX", index)
                }
                // 创建包含共享元素的ActivityOptions
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    it, // 这是当前活动中的共享ImageView
                    "image$index" // 与BigImageViewerActivity中的ImageView相同的transitionName
                )
                startActivity(intent, options.toBundle())
            }
        }
    }

    @Deprecated("Use displayImagesIfAnyV2 instead")
    private fun displayImagesIfAny(img: String) {
        val imageContainer = binding.imageContainer
        imageContainer.removeAllViews()
        val subUrls: List<String> = img.split(',')

        if (img.isEmpty() || subUrls.isEmpty()) {
            imageContainer.visibility = View.GONE
            return
        }

        imageContainer.visibility = View.VISIBLE
        for ((index, subUrl) in subUrls.withIndex()) {
            val imageView = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(0, 0, 0, 0)
                GlideApp.with(context)
                    .load(subUrl.toFullUrl())
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.image_broken)
                    .transform(CenterCrop(), RoundedCorners(4.dp))
                    //.transition(DrawableTransitionOptions.withCrossFade())
                    .into(this)

                transitionName = "image$index"
                setOnClickListener {
                    val intent = Intent(this@DetailActivity, BigImageViewerActivity::class.java).apply {
                        putStringArrayListExtra("IMAGES", ArrayList(subUrls))
                        putExtra("INDEX", index)
                    }
                    // 创建包含共享元素的ActivityOptions
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@DetailActivity,
                        it, // 这是当前活动中的共享ImageView
                        "image$index" // 与BigImageViewerActivity中的ImageView相同的transitionName
                    )
                    startActivity(intent, options.toBundle())
                }
            }
            val layoutParams = LinearLayout.LayoutParams(72.dp, 96.dp).apply {
                marginEnd = 48
            }
            imageContainer.addView(imageView, layoutParams)
        }
    }

    companion object {
        private const val TAG = "DetailActivity"
        private const val KEY_EXTRA_RECORD = "RECORD"

        @JvmStatic
        fun start(context: Context, record: Record) {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra(KEY_EXTRA_RECORD, record)
            }
            context.startActivity(intent)
        }

        @Suppress("DEPRECATION")
        private fun Intent.getRecord(): Record? = getParcelableExtra(KEY_EXTRA_RECORD)
    }
}
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
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.Record
import com.jiyingcao.a51fengliu.api.response.getPictures
import com.jiyingcao.a51fengliu.databinding.ActivityDetailV2Binding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.dp
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.DetailViewModel
import com.jiyingcao.a51fengliu.viewmodel.UiState

class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityDetailV2Binding

    private lateinit var viewModel: DetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityDetailV2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        //setEdgeToEdgePaddings(binding.root)

        val record = intent.getRecord()
        record?.let { updateUi(it) }

        val recordId = intent.getRecordId()
        if (recordId == null) {
            showToast("缺少参数: recordId")
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[DetailViewModel::class.java]
        viewModel.data.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // 显示加载动画
                    // if (!hasDataLoaded) statefulLayout.currentState = LOADING
                }
                is UiState.Success -> {
                    // hasDataLoaded = true
                    // refreshLayout.finishRefresh()
                    // statefulLayout.currentState = CONTENT
                    // 显示数据
                    updateUi(state.data)
                }
                is UiState.Empty -> {
                    // 不再使用
                    //refreshLayout.finishRefresh()
                }
                is UiState.Error -> {
                    /*
                    refreshLayout.finishRefresh()
                    // 显示错误信息
                    if (!hasDataLoaded)
                        statefulLayout.currentState = ERROR
                    else
                        Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    */
                }
            }
        }

        viewModel.fetchRecordById(id = recordId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val record = intent.getRecord()
        record?.let { updateUi(it) }

        val recordId = intent.getRecordId()
        recordId?.let { viewModel.fetchRecordById(id = it) }
    }
    
    private fun updateUi(itemData: Record) {
        //displayImagesIfAny(itemData.file)
        displayImagesIfAnyV2(itemData.getPictures())

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
        binding.dz.text = itemData.cityCode.to2LevelName()
        binding.createTime.text = timestampToDay(itemData.publishedAt)
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

    companion object {
        private const val TAG = "DetailActivity"
        private const val KEY_EXTRA_RECORD = "RECORD"
        private const val KEY_EXTRA_RECORD_ID = "RECORD_ID"

        @JvmStatic
        @Deprecated("Use start(context: Context, id: String) instead")
        fun start(context: Context, record: Record) {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra(KEY_EXTRA_RECORD, record)
            }
            context.startActivity(intent)
        }

        @JvmStatic
        fun start(context: Context, id: String) {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra(KEY_EXTRA_RECORD_ID, id)
            }
            context.startActivity(intent)
        }

        @Suppress("DEPRECATION")
        private fun Intent.getRecord(): Record? = getParcelableExtra(KEY_EXTRA_RECORD)
        private fun Intent.getRecordId(): String? = getStringExtra(KEY_EXTRA_RECORD_ID)
    }
}
package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.databinding.ActivityStatefulDetailBinding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.*
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.dp
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.DetailViewModel
import com.jiyingcao.a51fengliu.viewmodel.UiState
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityStatefulDetailBinding
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var realContentView: View

    private lateinit var viewModel: DetailViewModel

    /** 是否有数据已经加载 */
    private var hasDataLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityStatefulDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setEdgeToEdgePaddings(binding.root)

        val recordId = intent.getRecordId()
        if (recordId == null) {
            showToast("缺少参数: recordId")
            finish()
            return
        }

        statefulLayout = binding.statefulLayout // 简化代码调用
        realContentView = binding.statefulLayout.getContentView()
        refreshLayout = realContentView.findViewById(R.id.refreshLayout)
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setOnRefreshListener { viewModel.fetchRecordById(id = recordId) }
        }

        viewModel = ViewModelProvider(this)[DetailViewModel::class.java]
        viewModel.data.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // 显示加载动画
                    if (!hasDataLoaded) statefulLayout.currentState = LOADING
                }
                is UiState.Success -> {
                    hasDataLoaded = true
                    refreshLayout.finishRefresh()
                    statefulLayout.currentState = CONTENT
                    // 显示数据
                    updateUi(state.data)
                }
                is UiState.Empty -> {
                    // 不再使用
                    //refreshLayout.finishRefresh()
                }
                is UiState.Error -> {
                    refreshLayout.finishRefresh()
                    // 显示错误信息
                    if (!hasDataLoaded)
                        statefulLayout.currentState = ERROR
                    else
                        Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fetchRecordById(id = recordId)

        findViewById<View>(R.id.title_bar_back).setOnClickListener { finish() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val recordId = intent.getRecordId()
        recordId?.let { viewModel.fetchRecordById(id = it) }
    }
    
    private fun updateUi(record: RecordInfo) {
        //displayImagesIfAny(itemData.file)
        displayImagesIfAnyV2(record.getPictures())

        val title = realContentView.findViewById<TextView>(R.id.title)
        val dz = realContentView.findViewById<TextView>(R.id.dz)
        val qq = realContentView.findViewById<TextView>(R.id.qq)
        val wechat = realContentView.findViewById<TextView>(R.id.wechat)
        val telegram = realContentView.findViewById<TextView>(R.id.telegram)
        val yuni = realContentView.findViewById<TextView>(R.id.yuni)
        val phone = realContentView.findViewById<TextView>(R.id.phone)
        val address = realContentView.findViewById<TextView>(R.id.address)

        title.copyOnLongClick()
        dz.copyOnLongClick()
        qq.copyOnLongClick()
        wechat.copyOnLongClick()
        telegram.copyOnLongClick()
        yuni.copyOnLongClick()
        phone.copyOnLongClick()
        address.copyOnLongClick()

        val age = realContentView.findViewById<TextView>(R.id.age)
        val faceValue = realContentView.findViewById<TextView>(R.id.faceValue)
        val price = realContentView.findViewById<TextView>(R.id.price)
        val process = realContentView.findViewById<TextView>(R.id.process)
        val project = realContentView.findViewById<TextView>(R.id.project)
        val createTime = realContentView.findViewById<TextView>(R.id.createTime)
        val browse = realContentView.findViewById<TextView>(R.id.browse)
        val publisher = realContentView.findViewById<TextView>(R.id.publisher)

        title.text = record.title
        age.text = record.girlAge
        faceValue.text = record.girlBeauty
        displayPrices(price, record)
        process.text = record.desc
        project.text = record.serveList
        dz.text = record.cityCode.to2LevelName()
        createTime.text = timestampToDay(record.publishedAt)
        browse.text = record.viewCount

        qq.isVisible = !record.qq.isNullOrBlank()
        qq.text = getString(R.string.qq_format, record.qq)
        wechat.isVisible = !record.wechat.isNullOrBlank()
        wechat.text = getString(R.string.wechat_format, record.wechat)
        telegram.isVisible = !record.telegram.isNullOrBlank()
        telegram.text = getString(R.string.telegram_format, record.telegram)
        yuni.isVisible = !record.yuni.isNullOrBlank()
        yuni.text = getString(R.string.yuni_format, record.yuni)
        phone.isVisible = !record.phone.isNullOrBlank()
        phone.text = getString(R.string.phone_format, record.phone)
        address.isVisible = !record.address.isNullOrBlank()
        address.text = getString(R.string.address_format, record.address)

        publisher.text = when {
            record.anonymous == true -> "匿名"
            record.publisher != null -> record.publisher.name
            else -> "匿名"
        }
    }

    /**
     * 显示价格信息，如果有包夜价格则显示
     *
     * @param textView 显示价格的TextView，不包含“价格：”前缀
     */
    private fun displayPrices(
        textView: TextView,
        record: RecordInfo
    ) {
        textView.text = if (record.consumeAllNight.isNullOrBlank()) {
            record.consumeLv
        } else {
            getString(R.string.price_all_night_format, record.consumeLv, record.consumeAllNight)
        }
    }

    private val imageLoadedMap: MutableMap<String, Boolean> = mutableMapOf()

    private fun displayImagesIfAnyV2(imgs: List<String>) {
        val imageContainer = realContentView.findViewById<ViewGroup>(R.id.image_container)

        if (imgs.isEmpty()) {
            imageContainer.visibility = GONE
            return
        }

        imageContainer.visibility = VISIBLE
        // 从0到3循环
        for (index in 0..3) {
            val imageView: ImageView = when (index) {
                0 -> imageContainer.findViewById(R.id.image_0)
                1 -> imageContainer.findViewById(R.id.image_1)
                2 -> imageContainer.findViewById(R.id.image_2)
                3 -> imageContainer.findViewById(R.id.image_3)
                else -> return
            }
            val subUrl = imgs.getOrNull(index)
            if (subUrl.isNullOrBlank()) {
                imageView.visibility = INVISIBLE
                continue
            }

            val fullUrl = BASE_IMAGE_URL + subUrl
            imageView.visibility = VISIBLE
            imageView.tag = fullUrl

            GlideApp.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.image_broken)
                .transform(CenterCrop(), RoundedCorners(4.dp))
                //.transition(DrawableTransitionOptions.withCrossFade())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (model != null && model is String) { imageLoadedMap[model] = false }
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (model is String) { imageLoadedMap[model] = true }
                        return false
                    }

                })
                .into(imageView)
            imageView.setOnClickListener { view ->
                // 如果图片加载成功，才能点击查看大图
                if (imageLoadedMap[view.tag as String] == true) {
                    val intent = Intent(this, BigImageViewerActivity::class.java).apply {
                        putStringArrayListExtra("IMAGES", ArrayList(imgs))
                        putExtra("INDEX", index)
                    }
                    // 创建包含共享元素的ActivityOptions
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        view, // 这是当前活动中的共享ImageView
                        "image$index" // 与BigImageViewerActivity中的ImageView相同的transitionName
                    )
                    startActivity(intent, options.toBundle())
                } else {
                    // Debug only
                    // showToast("图片加载中，请稍候")
                }
            }
        }
    }

    companion object {
        private const val TAG = "DetailActivity"
        private const val KEY_EXTRA_RECORD_ID = "RECORD_ID"

        @JvmStatic
        fun start(context: Context, id: String) {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra(KEY_EXTRA_RECORD_ID, id)
            }
            context.startActivity(intent)
        }

        private fun Intent.getRecordId(): String? = getStringExtra(KEY_EXTRA_RECORD_ID)
    }
}
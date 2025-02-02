package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.databinding.ActivityDetailBinding
import com.jiyingcao.a51fengliu.databinding.ContentDetailBinding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import com.jiyingcao.a51fengliu.ui.dialog.LoadingDialog
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.dp
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.DetailEffect
import com.jiyingcao.a51fengliu.viewmodel.DetailIntent
import com.jiyingcao.a51fengliu.viewmodel.DetailState
import com.jiyingcao.a51fengliu.viewmodel.DetailViewModel
import com.jiyingcao.a51fengliu.viewmodel.DetailViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityDetailBinding
    private val contentBinding: ContentDetailBinding get() = binding.contentLayout

    private lateinit var viewModel: DetailViewModel

    private var loadingDialog: LoadingDialog? = null

    private val mySharedElementCallback = object : SharedElementCallback() {
        var returnIndex = 0

        override fun onMapSharedElements(
            names: List<String>,
            sharedElements: MutableMap<String, View>
        ) {
            val imageContainer = contentBinding.imageContainer
            // 获取对应位置的缩略图ImageView
            val imageView: ImageView = when (returnIndex) {
                0 -> imageContainer.findViewById(R.id.image_0)
                1 -> imageContainer.findViewById(R.id.image_1)
                2 -> imageContainer.findViewById(R.id.image_2)
                3 -> imageContainer.findViewById(R.id.image_3)
                else -> return
            }
            sharedElements.clear()
            sharedElements["image$returnIndex"] = imageView
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setEdgeToEdgePaddings(binding.root)

        val recordId = intent.getRecordId()
        if (recordId == null) {
            showToast("缺少参数: recordId")
            finish()
            return
        }

        setupClickListeners()

        setExitSharedElementCallback(mySharedElementCallback)

        viewModel = ViewModelProvider(
            this,
            DetailViewModelFactory(
                recordId,
                RecordRepository.getInstance(RetrofitClient.apiService),
                TokenManager.getInstance(App.INSTANCE.dataStore)
            )
        )[DetailViewModel::class.java]

        setupFlowCollectors()

        viewModel.processIntent(DetailIntent.LoadDetail)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val recordId = intent.getRecordId()
        if (recordId != null) viewModel.processIntent(DetailIntent.LoadDetail)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)

        if (resultCode == RESULT_OK) {
            val returnIndex = data?.getIntExtra("RESULT_INDEX", 0) ?: 0

            mySharedElementCallback.returnIndex = returnIndex

            // 延迟执行以确保视图已更新
            supportPostponeEnterTransition()

            // 开始延迟的过渡
            supportStartPostponedEnterTransition()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setUIVisibility(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setUIVisibility(false)
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is DetailState.Init -> {
                            showContentView()
                        }
                        is DetailState.Loading -> {
                            if (state.isFloatLoading) {
                                binding.showLoadingOverContent()
                            } else {
                                showLoadingView()
                            }
                        }
                        is DetailState.Success -> {
                            showContentView()
                            updateUI(state.record)
                        }
                        is DetailState.Error -> {
                            showErrorView(state.message)
                        }
                    }
                }
//            }
        }

        // 更新收藏按钮状态
        lifecycleScope.launch {
            viewModel.isFavorited.collect { isFavorited ->
                contentBinding.clickFavorite.isSelected = isFavorited == true
            }
        }

        // Collect side effects
        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collectLatest { effect ->
                    when (effect) {
                        is DetailEffect.ShowLoadingDialog -> showLoadingDialog()
                        is DetailEffect.DismissLoadingDialog -> dismissLoadingDialog()
                        is DetailEffect.ShowToast -> showToast(effect.message)
                    }
                }
//            }
        }
    }

    private fun showLoadingView() { binding.showLoading() }
    private fun showContentView() { binding.showContent() }
    private fun showErrorView(message: String) {
        binding.showError(message) { viewModel.processIntent(DetailIntent.Retry) }
    }

    private fun setupClickListeners() {
        binding.titleBar.titleBarBack.setOnClickListener { finish() }

        with(contentBinding) {
            clickReport.setOnClickListener {}
            clickFavorite.setOnClickListener {
                val detailIntent =
                    if (viewModel.isFavorited.value == true) DetailIntent.Unfavorite else DetailIntent.Favorite
                viewModel.processIntent(detailIntent)
            }

            contactInfoNotLogin.clickLogin.setOnClickListener {
                LoginActivity.start(this@DetailActivity)
            }

            // （不是正式功能，方便截图用的）长按隐藏警告信息
            contactWarning.setOnLongClickListener { v ->
                v.isVisible = false
                true
            }
        }
    }
    
    private fun updateUI(record: RecordInfo) {
        //displayImagesIfAny(itemData.file)
        displayImagesIfAnyV2(record.getPictures())

        with(contentBinding) {
            title.copyOnLongClick()
            dz.copyOnLongClick()

            title.text = record.title
            age.text = record.girlAge
            faceValue.text = record.girlBeauty
            displayPrices(price, record)
            process.text = record.desc
            project.text = record.serveList
            dz.text = record.cityCode.to2LevelName()
            createTime.text = timestampToDay(record.publishedAt)
            browse.text = record.viewCount

            publisher.text = when {
                record.anonymous == true -> "匿名"
                record.publisher != null -> record.publisher.name
                else -> "匿名"
            }
        }

        displayContactInfoByMemberState(record)
    }

    /**
     * 根据用户的会员状态显示联系方式
     */
    private fun displayContactInfoByMemberState(record: RecordInfo) {
        // 情况1：当前用户是VIP会员，显示联系方式
        if (!record.vipView.isNullOrBlank()
            /*&& record.vipProfileStatus!!.toInt() >= 4*/) {
            with(contentBinding) {
                showVip()
                // 显示警告信息，避免诈骗
                contactWarning.isVisible = true

                contactInfoVip.apply {
                    qq.copyOnLongClick()
                    wechat.copyOnLongClick()
                    telegram.copyOnLongClick()
                    yuni.copyOnLongClick()
                    phone.copyOnLongClick()
                    address.copyOnLongClick()

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
                }
            }
            return
        }

        // 情况2：当前用户是注册用户，显示“发布信息”“升级VIP”按钮
        if (record.vipProfileStatus?.toInt() == 3) {
            with(contentBinding) {
                showOrdinaryMember()
                // 没有联系方式时不需要显示警告信息
                contactWarning.isVisible = false
            }
            return
        }

        // 其实还有一种情况2.5：当前用户积分大于20，可以扣除积分查看联系方式。我没有这种账号，不知道UI应该如何呈现。

        // 情况3：当前用户未登录，显示“立即登录”按钮
        if (/*TODO token为空 ||*/
            record.vipProfileStatus?.toInt() == 1) {
            with(contentBinding) {
                showNotLogin()
                // 同上，未登录时不需要显示警告信息
                contactWarning.isVisible = false
            }
            return
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
        val imageContainer = contentBinding.imageContainer

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
                    ActivityCompat.startActivityForResult(this, intent, 42, options.toBundle())
                } else {
                    // Debug only
                    // showToast("图片加载中，请稍候")
                }
            }
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog()
        }
        if (loadingDialog?.isVisible != true) {
            loadingDialog?.showNow(supportFragmentManager, LoadingDialog.TAG)
        }
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismissAllowingStateLoss()
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

// 扩展函数
fun ActivityDetailBinding.showContent() {
    contentLayout.root.isVisible = true
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = false
}

fun ActivityDetailBinding.showError() {
    contentLayout.root.isVisible = false
    errorLayout.root.isVisible = true
    loadingLayout.root.isVisible = false
}

fun ActivityDetailBinding.showError(
    message: String = "出错了，请稍后重试",
    retry: (() -> Unit)? = null
) {
    loadingLayout.root.isVisible = false
    contentLayout.root.isVisible = false
    errorLayout.apply {
        root.isVisible = true
        // 假设错误布局中有这些视图
        tvError.text = message
        clickRetry.isVisible = retry != null
        clickRetry.setOnClickListener { retry?.invoke() }
    }
}

fun ActivityDetailBinding.showLoading() {
    contentLayout.root.isVisible = false
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = true
}

fun ActivityDetailBinding.showLoadingOverContent() {
    contentLayout.root.isVisible = true
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = true
}

fun ContentDetailBinding.showNotLogin() {
    contactInfoNotLogin.root.isVisible = true
    contactInfoOrdinaryMember.root.isVisible = false
    contactInfoVip.root.isVisible = false
}

fun ContentDetailBinding.showOrdinaryMember() {
    contactInfoNotLogin.root.isVisible = false
    contactInfoOrdinaryMember.root.isVisible = true
    contactInfoVip.root.isVisible = false
}

fun ContentDetailBinding.showVip() {
    contactInfoNotLogin.root.isVisible = false
    contactInfoOrdinaryMember.root.isVisible = false
    contactInfoVip.root.isVisible = true
}
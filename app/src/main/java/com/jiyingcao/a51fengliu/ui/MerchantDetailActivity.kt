package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.config.AppConfig.Network.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.databinding.ActivityMerchantDetailBinding
import com.jiyingcao.a51fengliu.databinding.MerchantContentDetailBinding
import com.jiyingcao.a51fengliu.glide.HostInvariantGlideUrl
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.auth.AuthActivity
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import com.jiyingcao.a51fengliu.util.ImageLoader
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.MerchantDetailIntent
import com.jiyingcao.a51fengliu.viewmodel.MerchantDetailState
import com.jiyingcao.a51fengliu.viewmodel.MerchantDetailViewModel
import com.jiyingcao.a51fengliu.viewmodel.MerchantDetailViewModelFactory
import kotlinx.coroutines.launch

class MerchantDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityMerchantDetailBinding
    private val contentBinding: MerchantContentDetailBinding get() = binding.contentDetail
    private lateinit var viewModel: MerchantDetailViewModel

    private val imageLoadedMap: MutableMap<String, Boolean> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMerchantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val merchantId = intent.getMerchantId()
        if (merchantId == null) {
            showToast("缺少参数: merchantId")
            finish()
            return
        }

        setupClickListeners()
        setupSmartRefreshLayout()

        viewModel = ViewModelProvider(
            this,
            MerchantDetailViewModelFactory(
                merchantId,
                RecordRepository.getInstance(RetrofitClient.apiService)
            )
        )[MerchantDetailViewModel::class.java]

        setupFlowCollectors()

        // 初次加载数据
        viewModel.processIntent(MerchantDetailIntent.LoadDetail)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val merchantId = intent.getMerchantId()
        if (merchantId != null) {
            viewModel.processIntent(MerchantDetailIntent.LoadDetail)
        }
    }

    private fun setupClickListeners() {
        binding.titleBar.titleBarBack.setOnClickListener { finish() }

        with(contentBinding) {
            contactInfoNotLogin.clickLogin.setOnClickListener {
                AuthActivity.start(this@MerchantDetailActivity)
            }
        }
    }

    private fun setupSmartRefreshLayout() {
        binding.refreshLayout.setOnRefreshListener {
            viewModel.processIntent(MerchantDetailIntent.PullToRefresh)
        }
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is MerchantDetailState.Idle -> {
                            showContentView()
                        }
                        is MerchantDetailState.Loading.FullScreen -> {
                            showLoadingView()
                        }
                        is MerchantDetailState.Loading.Float -> {
                            binding.showLoadingOverContent()
                        }
                        is MerchantDetailState.Loading.PullToRefresh -> {}
                        is MerchantDetailState.Success -> {
                            showContentView()
                            binding.refreshLayout.finishRefresh(true)
                            updateUI(state.merchant)
                        }
                        is MerchantDetailState.Error.FullScreen -> {
                            showErrorView(state.message)
                        }
                        is MerchantDetailState.Error.Float -> {
                            showContentView()
                            showToast(state.message)
                        }
                        is MerchantDetailState.Error.PullToRefresh -> {
                            binding.refreshLayout.finishRefresh(false)
                            showToast(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showLoadingView() { binding.showLoading() }
    private fun showContentView() { binding.showContent() }
    private fun showErrorView(message: String) {
        binding.showError(message) { viewModel.processIntent(MerchantDetailIntent.Retry) }
    }

    private fun updateUI(merchant: Merchant) {
        displayImagesIfAny(merchant)

        with(contentBinding) {
            name.copyOnLongClick()
            province.copyOnLongClick()
            desc.copyOnLongClick()

            name.text = merchant.name
            province.text = merchant.cityCode.to2LevelName()
            desc.text = merchant.desc
        }

        displayContactInfoByMemberState(merchant)
    }

    /**
     * 显示图片，参考 DetailActivity 的实现风格
     */
    private fun displayImagesIfAny(merchant: Merchant) {
        val imageContainer = contentBinding.imageContainer

        // 获取图片列表，优先使用 picture，如果为空则使用 coverPicture
        val imgs = when {
            !merchant.picture.isNullOrBlank() -> merchant.picture.split(",").filter { it.isNotBlank() }
            !merchant.coverPicture.isNullOrBlank() -> listOf(merchant.coverPicture)
            else -> emptyList()
        }

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

            imageView.visibility = VISIBLE
            imageView.tag = BASE_IMAGE_URL + subUrl  // 保存完整URL作为tag

            ImageLoader.load(
                imageView = imageView,
                url = subUrl, // Use the relative URL directly, ImageLoader will handle the complete URL
                cornerRadius = 4,
                listener = object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (model != null && model is HostInvariantGlideUrl) { 
                            imageLoadedMap[model.originalUrl] = false 
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (model is HostInvariantGlideUrl) { 
                            imageLoadedMap[model.originalUrl] = true 
                        }
                        return false
                    }
                }
            )
            imageView.setOnClickListener { view ->
                // 如果图片加载成功，才能点击查看大图
                if (imageLoadedMap[view.tag as String] == true) {
                    val intent = Intent(this, BigImageViewerActivity::class.java).apply {
                        putStringArrayListExtra("IMAGES", ArrayList(imgs))
                        putExtra("INDEX", index)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    /**
     * 根据用户的会员状态显示联系方式
     * 目前简化处理，只显示未登录状态的UI
     */
    private fun displayContactInfoByMemberState(merchant: Merchant) {
        with(contentBinding) {
            // 根据 merchant.contact 字段判断是否显示联系方式
            // 简化处理：如果有联系方式且用户有权限查看，显示联系方式；否则显示未登录提示
            if (!merchant.contact.isNullOrBlank()) {
                // TODO: 这里可以根据实际的权限逻辑来判断是否显示联系方式
                // 目前简化为显示未登录状态，提示用户登录
                showNotLogin()
            } else {
                showNotLogin()
            }
        }
    }

    companion object {
        private const val TAG = "MerchantDetailActivity"
        private const val KEY_EXTRA_MERCHANT_ID = "extra_merchant_id"

        @JvmStatic
        fun createIntent(context: Context, id: String): Intent =
            Intent(context, MerchantDetailActivity::class.java)
                .putExtra(KEY_EXTRA_MERCHANT_ID, id)

        @JvmStatic
        fun start(context: Context, id: String) {
            context.startActivity(createIntent(context, id))
        }

        private fun Intent.getMerchantId(): String? = getStringExtra(KEY_EXTRA_MERCHANT_ID)
    }
}

// 扩展函数，参考 DetailActivity 的风格
fun ActivityMerchantDetailBinding.showContent() {
    contentLayout.isVisible = true
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = false
}

fun ActivityMerchantDetailBinding.showError() {
    contentLayout.isVisible = false
    errorLayout.root.isVisible = true
    loadingLayout.root.isVisible = false
}

fun ActivityMerchantDetailBinding.showError(
    message: String = "出错了，请稍后重试",
    retry: (() -> Unit)? = null
) {
    loadingLayout.root.isVisible = false
    contentLayout.isVisible = false
    errorLayout.apply {
        root.isVisible = true
        // 假设错误布局中有这些视图
        tvError.text = message
        clickRetry.isVisible = retry != null
        clickRetry.setOnClickListener { retry?.invoke() }
    }
}

fun ActivityMerchantDetailBinding.showLoading() {
    contentLayout.isVisible = false
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = true
}

fun ActivityMerchantDetailBinding.showLoadingOverContent() {
    contentLayout.isVisible = true
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = true
}

fun MerchantContentDetailBinding.showNotLogin() {
    contactInfoNotLogin.root.isVisible = true
}
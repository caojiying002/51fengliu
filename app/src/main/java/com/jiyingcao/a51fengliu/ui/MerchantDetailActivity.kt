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
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.config.AppConfig.Network.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.databinding.ActivityMerchantDetailBinding
import com.jiyingcao.a51fengliu.databinding.MerchantContentDetailBinding
import com.jiyingcao.a51fengliu.glide.HostInvariantGlideUrl
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.auth.AuthActivity
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import com.jiyingcao.a51fengliu.util.ImageLoader
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.*
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

        observeUiState()

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
    }

    private fun setupSmartRefreshLayout() {
        binding.refreshLayout.setOnRefreshListener {
            viewModel.processIntent(MerchantDetailIntent.PullToRefresh)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    handleUiState(uiState)
                }
            }
        }
    }

    /**
     * 处理UI状态变化 - 单一状态处理逻辑
     * 重构后：Activity只负责UI展示，不再包含业务逻辑判断
     */
    private fun handleUiState(uiState: MerchantDetailUiState) {
        // 处理数据展示
        uiState.merchant?.let { merchant ->
            updateMerchantInfo(merchant)
            displayImagesIfAny(merchant)
        }

        // 处理联系信息显示状态 - 响应式更新
        uiState.contactDisplayState?.let { contactState ->
            updateContactDisplay(contactState)
        }

        // 处理各种UI状态
        when {
            uiState.showFullScreenLoading -> {
                showLoadingView()
            }
            uiState.showFullScreenError -> {
                showErrorView(uiState.errorMessage)
            }
            uiState.showContent -> {
                showContentView()
            }
        }

        // 处理覆盖层加载状态
        if (uiState.showOverlayLoading) {
            binding.showLoadingOverContent()
        }

        // 处理刷新状态
        if (!uiState.isRefreshing) {
            binding.refreshLayout.finishRefresh(!uiState.isError)
        }

        // 处理错误提示 - 只对非全屏错误显示Toast
        if (uiState.isError && !uiState.showFullScreenError) {
            showToast(uiState.errorMessage)
        }
    }

    /**
     * 更新商家基本信息显示
     */
    private fun updateMerchantInfo(merchant: Merchant) {
        with(contentBinding) {
            name.copyOnLongClick()
            province.copyOnLongClick()
            desc.copyOnLongClick()

            name.text = merchant.name
            province.text = merchant.cityCode.to2LevelName()
            desc.text = merchant.desc
        }
    }

    /**
     * 根据ViewModel计算的状态更新联系信息显示
     * 重构后：只负责UI更新，不包含业务逻辑判断
     */
    private fun updateContactDisplay(contactState: ContactDisplayState) {
        with(contentBinding) {
            if (contactState.showContact && !contactState.contactText.isNullOrBlank()) {
                // 显示联系方式
                contactNotVipContainer.isVisible = false
                contactVip.isVisible = true
                contactVip.text = contactState.contactText
            } else {
                // 显示提示信息和操作按钮
                contactVip.isVisible = false
                contactNotVipContainer.isVisible = true
                contactNotVip.text = contactState.promptMessage
                clickNotVip.text = contactState.actionButtonText
                
                // 设置点击事件
                clickNotVip.setOnClickListener {
                    when (contactState.actionType) {
                        ContactActionType.LOGIN -> {
                            AuthActivity.start(this@MerchantDetailActivity)
                        }
                        ContactActionType.UPGRADE_VIP -> {
                            // TODO: Handle upgrade VIP action
                        }
                        ContactActionType.NONE -> {
                            // 无操作
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
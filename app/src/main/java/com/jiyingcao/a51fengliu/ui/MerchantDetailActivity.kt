package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.databinding.ActivityMerchantDetailBinding
import com.jiyingcao.a51fengliu.databinding.MerchantContentDetailBinding
import com.jiyingcao.a51fengliu.ui.auth.AuthActivity
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.transition.SharedElementTransitionHelper
import com.jiyingcao.a51fengliu.ui.common.transition.createImageTransitionHelper
import com.jiyingcao.a51fengliu.ui.common.transition.loadMerchantImages
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.launch

/**
 * 重构后的MerchantDetailActivity - 复用相同的转场Helper
 * 
 * 🎯 展示了组件的强大复用性：
 * - 与DetailActivity使用完全相同的转场Helper
 * - 代码逻辑简洁明了，专注于业务逻辑
 * - 图片加载和转场动画逻辑完全透明化
 */
@AndroidEntryPoint
class MerchantDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityMerchantDetailBinding
    private val contentBinding: MerchantContentDetailBinding get() = binding.contentDetail

    private lateinit var merchantId: String

    private val viewModel by viewModels<MerchantDetailViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<MerchantDetailViewModel.Factory> { factory ->
                factory.create(merchantId)
            }
        }
    )

    // 🚀 复用相同的转场Helper - 零额外配置！
    private val transitionHelper: SharedElementTransitionHelper by lazy { 
        createImageTransitionHelper() 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMerchantDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intentMerchantId = intent.getMerchantId()
        if (intentMerchantId == null) {
            showToast("缺少参数: merchantId")
            finish()
            return
        } else {
            merchantId = intentMerchantId
        }

        setupClickListeners()
        setupSmartRefreshLayout()
        observeUiState()

        viewModel.processIntent(MerchantDetailIntent.InitialLoad)
    }

    override fun onStart() {
        super.onStart()
        viewModel.setUIVisibility(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setUIVisibility(false)
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
     * Activity只负责UI展示，不再包含业务逻辑判断
     */
    private fun handleUiState(uiState: MerchantDetailUiState) {
        // 处理数据展示
        uiState.merchant?.let { merchant ->
            updateMerchantInfo(merchant)
            // 🎯 使用DetailActivity相同的Helper处理图片
            displayImages(merchant)
        }

        // 处理联系信息显示状态
        updateContactDisplay(uiState)

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
     * 🚀 商家图片显示 - 使用相同Helper，API保持一致
     * 对比原来的显示逻辑：50+行复杂代码 → 现在：简洁的3行业务逻辑
     */
    private fun displayImages(merchant: Merchant) {
        // 使用Helper的专门方法处理商家图片
        transitionHelper.loadMerchantImages(
            imageContainer = contentBinding.imageContainer,
            merchant = merchant,
            onImageClick = { clickedIndex ->
                // 商家图片通常没有用户权限限制，直接显示
                transitionHelper.startImageViewer(
                    imageUrls = merchant.getPictures(),
                    clickedIndex = clickedIndex,
                    imageContainer = contentBinding.imageContainer
                )
            }
        )
    }

    /**
     * 根据ViewModel计算的状态更新联系信息显示
     * 只负责UI更新，不包含业务逻辑判断
     */
    private fun updateContactDisplay(uiState: MerchantDetailUiState) {
        with(contentBinding) {
            if (uiState.showContact && !uiState.contactText.isNullOrBlank()) {
                // 显示联系方式
                contactNotVipContainer.isVisible = false
                contactVip.isVisible = true
                contactVip.text = uiState.contactText
            } else {
                // 显示提示信息和操作按钮
                contactVip.isVisible = false
                contactNotVipContainer.isVisible = true
                contactNotVip.text = uiState.contactPromptMessage
                clickNotVip.text = uiState.contactActionButtonText
                
                // 设置点击事件
                clickNotVip.setOnClickListener {
                    when (uiState.contactActionType) {
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
package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.config.AppConfig.Network.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.databinding.ActivityDetailBinding
import com.jiyingcao.a51fengliu.databinding.ContentDetail0Binding
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.auth.AuthActivity
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import com.jiyingcao.a51fengliu.ui.dialog.LoadingDialog
import com.jiyingcao.a51fengliu.ui.dialog.ReportDialog
import com.jiyingcao.a51fengliu.ui.dialog.VipPromptDialog
import coil3.load
import coil3.request.placeholder
import coil3.request.error
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.dp
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.util.vibrate
import com.jiyingcao.a51fengliu.viewmodel.DetailEffect
import com.jiyingcao.a51fengliu.viewmodel.DetailIntent
import com.jiyingcao.a51fengliu.viewmodel.DetailUiState
import com.jiyingcao.a51fengliu.viewmodel.DetailViewModel
import com.jiyingcao.a51fengliu.viewmodel.DetailViewModelFactory
import com.jiyingcao.a51fengliu.viewmodel.FavoriteButtonState
import kotlinx.coroutines.launch

class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityDetailBinding
    private val contentBinding: ContentDetail0Binding get() = binding.contentLayout.contentDetail0
    private lateinit var viewModel: DetailViewModel

    private var loadingDialog: LoadingDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recordId = intent.getRecordId()
        if (recordId == null) {
            showToast("缺少参数: recordId")
            finish()
            return
        }

        setupClickListeners()
        setupSmartRefreshLayout()
        setupViewModel(recordId)
        setupStateObservers()

        if (!viewModel.hasLoadedData) {  // 横竖屏等配置更改时，不需要重新加载数据
            viewModel.processIntent(DetailIntent.LoadDetail())
        }
    }

    private fun setupSmartRefreshLayout() {
        binding.contentLayout.refreshLayout.setOnRefreshListener {
            viewModel.processIntent(DetailIntent.PullToRefresh)
        }
    }

    private fun setupViewModel(recordId: String) {
        viewModel = ViewModelProvider(
            this,
            DetailViewModelFactory(
                recordId,
                RecordRepository.getInstance(RetrofitClient.apiService),
                TokenManager.getInstance(App.INSTANCE.dataStore)
            )
        )[DetailViewModel::class.java]
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val recordId = intent.getRecordId()
        if (recordId != null) viewModel.processIntent(DetailIntent.LoadDetail())
    }


    override fun onStart() {
        super.onStart()
        viewModel.setUIVisibility(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setUIVisibility(false)
    }

    /**
     * 设置状态观察器 - 使用单一UiState模式
     */
    private fun setupStateObservers() {
        // 观察主要UI状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    handleUiState(uiState)
                }
            }
        }

        // 观察收藏按钮状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteButtonState.collect { state ->
                    handleFavoriteButtonState(state)
                }
            }
        }

        // 观察副作用
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    handleEffect(effect)
                }
            }
        }
    }

    /**
     * 处理UI状态变化 - 单一状态处理逻辑
     */
    private fun handleUiState(uiState: DetailUiState) {
        // 处理数据展示
        uiState.record?.let { record ->
            updateUI(record)
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
            binding.contentLayout.refreshLayout.finishRefresh(!uiState.isError)
        }

        // 处理错误提示 - 只对非全屏错误显示Toast
        if (uiState.isError && !uiState.showFullScreenError) {
            showToast(uiState.errorMessage)
        }
    }

    /**
     * 处理收藏按钮状态
     */
    private fun handleFavoriteButtonState(state: FavoriteButtonState) {
        when (state) {
            is FavoriteButtonState.Idle -> {
                contentBinding.clickFavorite.isEnabled = true
                contentBinding.clickFavorite.alpha = 1.0f
                contentBinding.clickFavorite.isSelected = state.isFavorited
            }
            is FavoriteButtonState.InProgress -> {
                contentBinding.clickFavorite.isEnabled = false
                contentBinding.clickFavorite.alpha = 0.7f
                contentBinding.clickFavorite.isSelected = state.targetState
            }
        }
    }

    /**
     * 处理副作用
     */
    private fun handleEffect(effect: DetailEffect) {
        when (effect) {
            is DetailEffect.ShowLoadingDialog -> showLoadingDialog()
            is DetailEffect.DismissLoadingDialog -> dismissLoadingDialog()
            is DetailEffect.ShowToast -> showToast(effect.message)
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
            clickReport.setOnClickListener {
                val record = viewModel.uiState.value.record
                record?.let {
                    val reportDialog = ReportDialog.newInstance(it.title, it.id)
                    reportDialog.show(supportFragmentManager, ReportDialog.TAG)
                }
            }
            
            clickFavorite.setOnClickListener {
                val currentButtonState = viewModel.favoriteButtonState.value
                if (currentButtonState !is FavoriteButtonState.Idle) return@setOnClickListener

                if (!currentButtonState.isFavorited) {
                    // 从未收藏变为收藏要震动，反之则不用
                    vibrate(this@DetailActivity)
                }
                viewModel.processIntent(DetailIntent.ToggleFavorite)
            }

            contactInfoNotLogin.clickLogin.setOnClickListener {
                AuthActivity.start(this@DetailActivity)
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
        displayImagesIfAnyV2(record)

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
                contactWarning.isVisible = true // 显示警告信息，避免诈骗

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
                contactWarning.isVisible = false    // 没有联系方式时不需要显示警告信息
            }
            return
        }

        // 其实还有一种情况2.5：当前用户积分大于20，可以扣除积分查看联系方式。我没有这种账号，不知道UI应该如何呈现。

        // 情况3：当前用户未登录，显示“立即登录”按钮
        if (/*TODO token为空 ||*/
            record.vipProfileStatus?.toInt() == 1) {
            with(contentBinding) {
                showNotLogin()
                contactWarning.isVisible = false    // 同上，未登录时不需要显示警告信息
            }
            return
        }
    }

    /**
     * 显示价格信息，如果有包夜价格则显示
     *
     * @param textView 显示价格的TextView，不包含“价格：”前缀
     */
    private fun displayPrices(textView: TextView, record: RecordInfo) {
        textView.text = if (record.consumeAllNight.isNullOrBlank()) {
            record.consumeLv
        } else {
            getString(R.string.price_all_night_format, record.consumeLv, record.consumeAllNight)
        }
    }

    private fun displayImagesIfAnyV2(record: RecordInfo) {
        val imageContainer = contentBinding.imageContainer
        val imgs = record.getPictures()
        
        if (imgs.isEmpty()) {
            imageContainer.visibility = GONE
            return
        }

        imageContainer.visibility = VISIBLE
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

            val fullUrl = BASE_IMAGE_URL + subUrl
            imageView.load(fullUrl) {
                placeholder(R.drawable.placeholder)
                error(R.drawable.image_broken)
                transformations(RoundedCornersTransformation(4.dp.toFloat()))
            }
            
            imageView.setOnClickListener { view ->
                // 显示VIP提示对话框
                if (!canViewLargeImage(record)) {
                    VipPromptDialog.newInstance(cancelable = false).showNow(supportFragmentManager, VipPromptDialog.TAG)
                    return@setOnClickListener
                }

                val intent = Intent(this, BigImageViewerActivity::class.java).apply {
                    putStringArrayListExtra("IMAGES", ArrayList(imgs))
                    putExtra("INDEX", index)
                }
                startActivity(intent)
            }
        }
    }

    /**
     * 判断当前用户是否可以使用图片放大功能
     *
     * @param record 当前记录的信息，包含用户权限相关数据
     * @return 如果用户可以查看大图返回true，否则返回false
     */
    private fun canViewLargeImage(record: RecordInfo): Boolean {
        // 调试模式下，可以跳过功能限制检查
        if (AppConfig.Debug.bypassLargeImageCheck()) {
            return true
        }

        // 根据会员状态判断权限
        // 1. 判断是否是VIP会员
        // 2. 判断是否已使用积分购买联系方式
        // 判断逻辑（目前只使用RecordInfo.vipView字段判断）:
        val hasPermission = !record.vipView.isNullOrBlank()

        return hasPermission
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
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    companion object {
        private const val TAG = "DetailActivity"
        private const val KEY_EXTRA_RECORD_ID = "extra_record_id"

        @JvmStatic
        fun createIntent(context: Context, id: String): Intent =
            Intent(context, DetailActivity::class.java)
                .putExtra(KEY_EXTRA_RECORD_ID, id)

        @JvmStatic
        fun start(context: Context, id: String) {
            context.startActivity(createIntent(context, id))
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
    //retryText: String = "重试",
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

fun ContentDetail0Binding.showNotLogin() {
    contactInfoNotLogin.root.isVisible = true
    contactInfoOrdinaryMember.root.isVisible = false
    contactInfoVip.root.isVisible = false
}

fun ContentDetail0Binding.showOrdinaryMember() {
    contactInfoNotLogin.root.isVisible = false
    contactInfoOrdinaryMember.root.isVisible = true
    contactInfoVip.root.isVisible = false
}

fun ContentDetail0Binding.showVip() {
    contactInfoNotLogin.root.isVisible = false
    contactInfoOrdinaryMember.root.isVisible = false
    contactInfoVip.root.isVisible = true
}
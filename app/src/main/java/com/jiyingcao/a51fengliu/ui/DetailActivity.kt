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
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.databinding.ActivityStatefulDetailBinding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.common.BigImageViewerActivity
import com.jiyingcao.a51fengliu.ui.dialog.LoadingDialog
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.*
import com.jiyingcao.a51fengliu.util.copyOnLongClick
import com.jiyingcao.a51fengliu.util.dp
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.DetailEffect
import com.jiyingcao.a51fengliu.viewmodel.DetailIntent
import com.jiyingcao.a51fengliu.viewmodel.DetailState
import com.jiyingcao.a51fengliu.viewmodel.DetailViewModel
import com.jiyingcao.a51fengliu.viewmodel.DetailViewModelFactory
import com.jiyingcao.a51fengliu.viewmodel.UiState
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class DetailActivity : BaseActivity() {
    private lateinit var binding: ActivityStatefulDetailBinding
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var realContentView: View
    private lateinit var contactInfoContainer: ViewGroup
    private lateinit var contactInfoVIP: View
    private lateinit var contactInfoOrdinaryMember: View
    private lateinit var contactInfoNotLogin: View

    private lateinit var viewModel: DetailViewModel

    /** 是否有数据已经加载 */
    private var hasDataLoaded: Boolean = false

    private var loadingDialog: LoadingDialog? = null

    private val mySharedElementCallback = object : SharedElementCallback() {
        var returnIndex = 0

        override fun onMapSharedElements(
            names: List<String>,
            sharedElements: MutableMap<String, View>
        ) {
            val imageContainer = realContentView.findViewById<ViewGroup>(R.id.image_container)
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
        contactInfoContainer = realContentView.findViewById(R.id.contactInfoContainer)
        contactInfoVIP = contactInfoContainer.findViewById(R.id.contact_info_vip)
        contactInfoOrdinaryMember = contactInfoContainer.findViewById(R.id.contact_info_ordinary_member)
        contactInfoNotLogin = contactInfoContainer.findViewById(R.id.contact_info_not_login)

        refreshLayout = realContentView.findViewById(R.id.refreshLayout)
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setOnRefreshListener { viewModel.processIntent(DetailIntent.Refresh) }
        }

        setupClickListeners()

        setExitSharedElementCallback(mySharedElementCallback)

        viewModel = ViewModelProvider(
            this,
            DetailViewModelFactory(
                recordId,
                RecordRepository.getInstance(RetrofitClient.apiService)
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

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is DetailState.Init -> {
                            showContentView()
                        }
                        is DetailState.Loading -> {
                            // Initial loading state
                            showLoadingView()
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

        // Collect side effects
        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is DetailEffect.ShowLoadingDialog -> showLoadingDialog()
                        is DetailEffect.DismissLoadingDialog -> dismissLoadingDialog()
                        is DetailEffect.FinishRefresh -> refreshLayout.finishRefresh()
                        is DetailEffect.ShowToast -> showToast(effect.message)
                    }
                }
//            }
        }
    }

    private fun showLoadingView() {statefulLayout.currentState = LOADING}
    private fun showContentView() {statefulLayout.currentState = CONTENT}
    private fun showErrorView(message: String = "加载失败") { // TODO 显示[message]到UI，以及点击重试
        statefulLayout.currentState = ERROR
        statefulLayout.getErrorView().findViewById<TextView>(R.id.stateful_default_error_text_view)?.text = message
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.title_bar_back).setOnClickListener { finish() }
        findViewById<View>(R.id.contactWarning).setOnLongClickListener { v ->
            v.isVisible = false
            true
        }
        findViewById<View>(R.id.click_report).setOnClickListener {}
    }
    
    private fun updateUI(record: RecordInfo) {
        //displayImagesIfAny(itemData.file)
        displayImagesIfAnyV2(record.getPictures())

        val title = realContentView.findViewById<TextView>(R.id.title)
        val dz = realContentView.findViewById<TextView>(R.id.dz)

        title.copyOnLongClick()
        dz.copyOnLongClick()

        val age = realContentView.findViewById<TextView>(R.id.age)
        val faceValue = realContentView.findViewById<TextView>(R.id.faceValue)
        val price = realContentView.findViewById<TextView>(R.id.price)
        val process = realContentView.findViewById<TextView>(R.id.process)
        val project = realContentView.findViewById<TextView>(R.id.project)
        val createTime = realContentView.findViewById<TextView>(R.id.createTime)
        val browse = realContentView.findViewById<TextView>(R.id.browse)
        val publisher = realContentView.findViewById<TextView>(R.id.publisher)
        val favorite = realContentView.findViewById<TextView>(R.id.click_favorite)

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

        val isFavorite = record.isFavorite==true
        favorite.isSelected = isFavorite
        favorite.setOnClickListener { v ->
            // TODO viewModel.processIntent(if (isFavorite) DetailIntent.Unfavorite else DetailIntent.Favorite)
        }

        displayContactInfoByMemberState(record)
    }

    /**
     * 根据用户的会员状态显示联系方式
     */
    private fun displayContactInfoByMemberState(record: RecordInfo) {
        val contactWarning = realContentView.findViewById<View>(R.id.contactWarning)

        // 情况1：当前用户是VIP会员，显示联系方式
        if (!record.vipView.isNullOrBlank()
            /*&& record.vipProfileStatus!!.toInt() >= 4*/) {
            contactInfoVIP.isVisible = true
            contactInfoOrdinaryMember.isVisible = false
            contactInfoNotLogin.isVisible = false

            // 显示警告信息，避免诈骗
            contactWarning.isVisible = true

            val qq = realContentView.findViewById<TextView>(R.id.qq)
            val wechat = realContentView.findViewById<TextView>(R.id.wechat)
            val telegram = realContentView.findViewById<TextView>(R.id.telegram)
            val yuni = realContentView.findViewById<TextView>(R.id.yuni)
            val phone = realContentView.findViewById<TextView>(R.id.phone)
            val address = realContentView.findViewById<TextView>(R.id.address)

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
            return
        }

        // 情况2：当前用户是注册用户，显示“发布信息”“升级VIP”按钮
        if (record.vipProfileStatus?.toInt() == 3) {
            contactInfoVIP.isVisible = false
            contactInfoOrdinaryMember.isVisible = true
            contactInfoNotLogin.isVisible = false

            // 没有联系方式时不需要显示警告信息
            contactWarning.isVisible = false
            return
        }

        // 其实还有一种情况2.5：当前用户积分大于20，可以扣除积分查看联系方式。我没有这种账号，不知道UI应该如何呈现。

        // 情况3：当前用户未登录，显示“立即登录”按钮
        if (/*TODO token为空 ||*/
            record.vipProfileStatus?.toInt() == 1) {
            contactInfoVIP.isVisible = false
            contactInfoOrdinaryMember.isVisible = false
            contactInfoNotLogin.isVisible = true
            // 同上，未登录时不需要显示警告信息
            contactWarning.isVisible = false
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
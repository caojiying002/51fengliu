package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.databinding.ActivitySearchBinding
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.ImeUtil
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.RefreshState
import com.jiyingcao.a51fengliu.viewmodel.SearchIntent
import com.jiyingcao.a51fengliu.viewmodel.SearchState
import com.jiyingcao.a51fengliu.viewmodel.SearchState.Loading
import com.jiyingcao.a51fengliu.viewmodel.SearchState.Error
import com.jiyingcao.a51fengliu.viewmodel.SearchViewModel4
import com.jiyingcao.a51fengliu.viewmodel.SearchViewModelFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class SearchActivity: BaseActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private lateinit var viewModel: SearchViewModel4

    private lateinit var recordAdapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupSmartRefreshLayout()
        setupRecyclerView()

        setupViewModel()
        setupFlowCollectors()

        /*viewModel.uiState.observe(this) { state ->
            handleLoadingState(state)

            // Success: 将数据提交或追加到列表
            if (state is Success<*>) {
                val pageData = state.data as PageData
                val page = pageData.current
                val data = pageData.records

                if (page == 1) {
                    recordAdapter.submitList(data)
                    // refreshLayout.setEnableLoadMore(true)   // 第一页有数据了，可以启用LoadMore了
                }
                else
                    recordAdapter.addAll(data)
            }
        }*/

        /*viewModel.data.observe(this) { dataWithLoadingType ->
            val loadingType = dataWithLoadingType.loadingType
            val page = dataWithLoadingType.page
            val data: List<ItemData> = dataWithLoadingType.data
            // 显示数据
            when (loadingType) {
                FULL_SCREEN -> { statefulLayout.currentState = StatefulLayout.State.CONTENT }
                PULL_REFRESH -> { refreshLayout.finishRefresh(true) }
                LOAD_MORE -> { refreshLayout.finishLoadMore(true) }
                NONE -> {}
            }
            if (page == 1) {
                itemDataAdapter.submitList(data)
                // refreshLayout.setEnableLoadMore(true)   // 第一页有数据了，可以启用LoadMore了
            }
            else
                itemDataAdapter.addAll(data)
        }*/

        //viewModel.search(page = 1)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            SearchViewModelFactory(
                RecordRepository.getInstance(RetrofitClient.apiService)
            )
        )[SearchViewModel4::class.java]
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is SearchState.Loading -> handleLoadingState(state)
                    is SearchState.Error -> handleErrorState(state)
                    is SearchState.Success -> {
                        state.apply { updateUI(pagedData, isFirstPage, isLastPage) }
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.noMoreDataState.collect { noMoreData ->
                refreshLayout.setNoMoreData(noMoreData)
            }
        }

        lifecycleScope.launch {
            viewModel.keywords.collect { keywords ->
                binding.introSearchResult.isVisible = keywords != null
                binding.introSearchResult.setText(getString(R.string.intro_search_result_format, keywords))
            }
        }

        /*lifecycleScope.launch {
            viewModel.refreshState.collect { state ->
                when (state) {
                    RefreshState.RefreshSuccess -> refreshLayout.finishRefresh()
                    RefreshState.RefreshError -> refreshLayout.finishRefresh(false)
                    RefreshState.LoadMoreSuccess -> refreshLayout.finishLoadMore()
                    RefreshState.LoadMoreError -> refreshLayout.finishLoadMore(false)
                    else -> {}
                }
            }
        }*/
    }

    private fun showContentView() { binding.showContent() }
    private fun showFullScreenLoading() { binding.showLoading() }
    private fun showPullToRefreshLoading() { /* 下拉刷新加载 */ }
    private fun showLoadMoreLoading() { /* 分页加载 */ }
    private fun showFloatLoading() { binding.showLoadingOverContent() }
    private fun hideAllLoadingIndicators() { /* 隐藏所有加载指示器 */ }
    private fun showError(error: String) { binding.showError(error) { /* TODO 重试按钮 */ } }

    private fun handleLoadingState(loading: Loading) {
        when (loading) {
            Loading.FullScreen -> showFullScreenLoading()
            Loading.PullToRefresh -> showPullToRefreshLoading()
            Loading.LoadMore -> showLoadMoreLoading()
            Loading.Float -> showFloatLoading()
        }
    }

    private fun handleErrorState(error: Error) {
        when (error) {
            is Error.FullScreen -> showError(error.message)
            is Error.PullToRefresh -> {
                refreshLayout.finishRefresh(false)
                showToast(error.message)
            }
            is Error.LoadMore -> {
                refreshLayout.finishLoadMore(false)
                showToast(error.message)
            }
            is Error.Float -> {
                showContentView()
                showToast(error.message)
            }
        }
    }

    private fun updateUI(pagedData: PageData, isFirstPage: Boolean, isLastPage: Boolean) {
        showContentView()
        refreshLayout.finishRefresh()
        refreshLayout.finishLoadMore()

        if (isFirstPage && pagedData.records.isEmpty()) {
            binding.contentLayout.emptyContent.isVisible = true
            binding.contentLayout.realContent.isVisible = false
        } else {
            binding.contentLayout.emptyContent.isVisible = false
            binding.contentLayout.realContent.isVisible = true
        }

        when (isFirstPage) {
            true -> recordAdapter.submitList(pagedData.records)
            false -> recordAdapter.addAll(pagedData.records)
        }
    }

    private val chooseCityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val cityCode = result.data?.getStringExtra("CITY_CODE")
            Log.d("registerForActivityResult", "City code selected: $cityCode")

            displayCity(cityCode)
            cityCode?.let {
                App.INSTANCE.showToast("City code selected: $cityCode")
                viewModel.processIntent(SearchIntent.UpdateCity(cityCode))
            }
        }
    }

    private fun displayCity(cityCode: String?) {
        binding.clickChooseCity.text = cityCode?.to2LevelName() ?: "选择地区"
    }

    private fun setupClickListeners() {
        binding.titleBarBack.setOnClickListener { finish() }
        binding.clickChooseCity.setOnClickListener {
            chooseCityLauncher.launch(ChooseCityActivity.createIntent(this@SearchActivity))
        }
        binding.clickSearch.setOnClickListener { v ->
            ImeUtil.hideIme(v)

            val keywords = binding.searchEditText.text.toString().trim()
            // 关键字允许为空，相当于显示(某城市)所有数据
            Log.d(TAG, "Search keywords=$keywords")
            // 隐藏键盘
            viewModel.processIntent(SearchIntent.UpdateKeywords(keywords))
        }
        binding.searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null &&
                        event.keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.action == KeyEvent.ACTION_DOWN)
            ) {
                // 触发搜索点击
                binding.clickSearch.performClick()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun setupSmartRefreshLayout() {
        refreshLayout = binding.contentLayout.refreshLayout
            .apply {
                setRefreshHeader(ClassicsHeader(context))
                setRefreshFooter(ClassicsFooter(context))
                setOnRefreshListener { viewModel.processIntent(SearchIntent.Refresh) }
                setOnLoadMoreListener { viewModel.processIntent(SearchIntent.NextPage) }
                // setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
            }
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { _, _, position ->
                this.getItem(position)?.let {
                    DetailActivity.start(context, it.id)
                }
            }
        }
        recyclerView = binding.contentLayout.recyclerView
            .apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = recordAdapter
            }
    }

    companion object {
        private const val TAG = "SearchActivity"

        @JvmStatic
        fun createIntent(context: Context) =
            Intent(context, SearchActivity::class.java).apply {
                // putExtra("ITEM_DATA", itemData)
            }

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}

// 扩展函数
fun ActivitySearchBinding.showContent() {
    contentLayout.root.isVisible = true
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = false
}

fun ActivitySearchBinding.showError() {
    contentLayout.root.isVisible = false
    errorLayout.root.isVisible = true
    loadingLayout.root.isVisible = false
}

fun ActivitySearchBinding.showError(
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

fun ActivitySearchBinding.showLoading() {
    contentLayout.root.isVisible = false
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = true
}

fun ActivitySearchBinding.showLoadingOverContent() {
    contentLayout.root.isVisible = true
    errorLayout.root.isVisible = false
    loadingLayout.root.isVisible = true
}
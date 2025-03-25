package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.databinding.ActivitySearchBinding
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.ImeUtil
import com.jiyingcao.a51fengliu.util.scrollToTopIfEmpty
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.SearchIntent
import com.jiyingcao.a51fengliu.viewmodel.SearchState
import com.jiyingcao.a51fengliu.viewmodel.SearchState.Loading
import com.jiyingcao.a51fengliu.viewmodel.SearchState.Error
import com.jiyingcao.a51fengliu.viewmodel.SearchViewModel
import com.jiyingcao.a51fengliu.viewmodel.SearchViewModelFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class SearchActivity: BaseActivity() {
    private lateinit var binding: ActivitySearchBinding

    private val statefulContent: StatefulRefreshRecyclerViewBinding
        get() = binding.statefulContent

    private val refreshLayout: SmartRefreshLayout get() = statefulContent.refreshLayout
    private val recyclerView: RecyclerView get() = statefulContent.recyclerView

    private lateinit var recordAdapter: RecordAdapter

    private val viewModel by viewModels<SearchViewModel> {
        SearchViewModelFactory(
            RecordRepository.getInstance(RetrofitClient.apiService)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupSmartRefreshLayout()
        setupRecyclerView()

        setupFlowCollectors()
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is SearchState.Loading -> handleLoadingState(state)
                    is SearchState.Error -> handleErrorState(state)
                    is SearchState.Success -> {
                        showContentView()
                        refreshLayout.finishRefresh()
                        refreshLayout.finishLoadMore()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            viewModel.records.collect { updateRecords(it) }
        }

        lifecycleScope.launch {
            viewModel.noMoreDataState.collect { noMoreData ->
                refreshLayout.setNoMoreData(noMoreData)
            }
        }

        lifecycleScope.launch {
            viewModel.keywords.collect { keywords ->
                binding.introSearchResult.isVisible = keywords != null
                binding.introSearchResult.text =
                    getString(R.string.intro_search_result_format, keywords)
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

    private fun showContentView() { statefulContent.showContentView() }
    private fun showFullScreenLoading() { statefulContent.showLoadingView() }
    private fun showPullToRefreshLoading() { /* 下拉刷新加载 */ }
    private fun showLoadMoreLoading() { /* 分页加载 */ }
    private fun hideAllLoadingIndicators() { /* 隐藏所有加载指示器 */ }
    private fun showError(error: String) { statefulContent.showErrorView(error) { /* TODO 重试按钮 */ } }

    private fun handleLoadingState(loading: Loading) {
        when (loading) {
            Loading.FullScreen -> showFullScreenLoading()
            Loading.PullToRefresh -> showPullToRefreshLoading()
            Loading.LoadMore -> showLoadMoreLoading()
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
        }
    }

    private fun updateRecords(records: List<RecordInfo>) {
        recordAdapter.submitList(records)
        recyclerView.scrollToTopIfEmpty(records)

        // 如果没有数据，显示空状态
        if (records.isEmpty()) {
            statefulContent.showEmptyContent()
        } else {
            statefulContent.showRealContent()
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

                // 更新城市时，要连用户输入的关键字一起传递
                val keywords = binding.searchEditText.text.toString().trim()
                viewModel.processIntent(SearchIntent.UpdateCityWithKeywords(cityCode, keywords))
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
        refreshLayout.apply {
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
        recyclerView.apply {
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
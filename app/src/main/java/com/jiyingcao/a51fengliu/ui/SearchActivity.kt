package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.databinding.ActivitySearchBinding
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.AppLogger
import com.jiyingcao.a51fengliu.util.ImeUtil
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.LoadingType
import com.jiyingcao.a51fengliu.viewmodel.SearchIntent
import com.jiyingcao.a51fengliu.viewmodel.SearchUiState
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

        observeUiState()
    }

    /**
     * 单一状态流观察 - 企业级MVI最佳实践
     * 所有UI状态变化都在一个地方处理，便于维护和调试
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    // 处理数据展示
                    recordAdapter.submitList(uiState.records) {
                        // 如果数据列表为空且已搜索，将RecyclerView滚动到最顶端
                        if (uiState.records.isEmpty() && uiState.hasSearched) {
                            recyclerView.scrollToPosition(0)
                        }
                    }

                    // 处理各种UI状态 - 使用when表达式确保所有状态都被处理
                    when {
                        uiState.showFullScreenLoading -> {
                            showLoadingView()
                        }

                        uiState.showFullScreenError -> {
                            showErrorView(uiState.errorMessage)
                        }

                        uiState.showEmpty -> {
                            statefulContent.showEmptyContent()
                        }

                        uiState.showContent -> {
                            statefulContent.showRealContent()
                        }

                        uiState.showInitialState -> {
                            // 初始状态，显示内容视图但不显示任何数据
                            statefulContent.showContentView()
                        }
                    }

                    // 处理搜索结果提示
                    binding.introSearchResult.isVisible = uiState.hasSearched
                    if (uiState.hasSearched) {
                        binding.introSearchResult.text =
                            getString(R.string.intro_search_result_format, uiState.keywords)
                    }

                    // 精确处理刷新状态 - 只处理下拉刷新相关的状态变化
                    handleRefreshState(uiState)
                    
                    // 精确处理加载更多状态 - 只处理上拉加载相关的状态变化
                    handleLoadMoreState(uiState)

                    // 处理无更多数据状态
                    refreshLayout.setNoMoreData(uiState.noMoreData)

                    // 处理错误提示 - 只对非全屏错误显示Toast
                    if (uiState.isError && !uiState.showFullScreenError) {
                        showToast(uiState.errorMessage)
                    }
                }
            }
        }
    }

    private fun handleRefreshState(uiState: SearchUiState) {
        when {
            uiState.isRefreshing -> {
                // 下拉刷新进行中，SmartRefreshLayout 自动处理
            }
            !uiState.isRefreshing && uiState.loadingType == LoadingType.PULL_TO_REFRESH -> {
                // 下拉刷新结束（无论成功失败）
                refreshLayout.finishRefresh(!uiState.isError)
            }
            // 其他情况不处理 refreshLayout
        }
    }

    private fun handleLoadMoreState(uiState: SearchUiState) {
        when {
            uiState.isLoadingMore -> {
                // 上拉加载进行中，SmartRefreshLayout 自动处理
            }
            !uiState.isLoadingMore && uiState.loadingType == LoadingType.LOAD_MORE -> {
                // 上拉加载结束（无论成功失败）
                refreshLayout.finishLoadMore(!uiState.isError)
            }
            // 其他情况不处理 refreshLayout
        }
    }

    private fun showLoadingView() { statefulContent.showLoadingView() }
    private fun showContentView() { statefulContent.showContentView() }
    private fun showErrorView(message: String) {
        statefulContent.showErrorView(message) {
            viewModel.processIntent(SearchIntent.Retry)
        }
    }

    private val chooseCityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val cityCode = result.data?.getStringExtra("CITY_CODE")
            AppLogger.d("ActivityResultCallback", "City code selected: $cityCode")

            displayCity(cityCode)
            cityCode?.let {
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
            // 关键字允许为空，相当于显示(某城市)所有数据
            val keywords = binding.searchEditText.text.toString().trim()
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
                // 收起键盘并清除焦点
                ImeUtil.hideIme(v)
                v.clearFocus()
                return@setOnEditorActionListener true
            }
            false
        }
        // 用户触摸EditText以外的区域时，隐藏键盘并清除EditText的焦点
        binding.main.addWatchedEditText(binding.searchEditText)
    }

    private fun setupSmartRefreshLayout() {
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.processIntent(SearchIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(SearchIntent.LoadMore) }
            // setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
        }
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { record, position ->
                DetailActivity.start(this@SearchActivity, record.id)
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
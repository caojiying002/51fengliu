package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.databinding.DefaultLayoutStatefulRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.viewmodel.LoadingType.*
import com.jiyingcao.a51fengliu.viewmodel.SearchViewModel2
import com.jiyingcao.a51fengliu.viewmodel.SearchViewModel2.UiState
import com.jiyingcao.a51fengliu.viewmodel.UiState2
import com.jiyingcao.a51fengliu.viewmodel.UiState2.*
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchActivity: BaseActivity() {
    private lateinit var binding: DefaultLayoutStatefulRecyclerViewBinding
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private val viewModel: SearchViewModel2 by viewModels()

    private lateinit var recordAdapter: RecordAdapter

    /** 是否有数据已经加载 */
    @Deprecated("") private var hasDataLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DefaultLayoutStatefulRecyclerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSearchBar()
        setupStatefulLayout()
        setupSmartRefreshLayout()
        setupRecyclerView()

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

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.searchResults.collect {
                        // it: PagedData { records: List<RecordInfo> }
                        if (it == null) {
                            Log.d(TAG, "No data loaded yet")
                            return@collect
                        }

                        // 根据是否第1页来决定清空列表还是追加数据
                        recordAdapter.let { adapter ->
                            if (it.isFirstPage()) {
                                adapter.submitList(it.records)
                            } else {
                                adapter.addAll(it.records)
                            }
                        }

                        // 是否还有下一页可以加载
                        refreshLayout.setNoMoreData(!it.hasNextPage())
                    }
                }
                launch {
                    viewModel.uiState.collectLatest { state ->
                        handleUiState(state)
                    }
                }
            }
        }
    }

    private fun handleUiState(state: UiState) {
        when (state) {
            is UiState.Idle -> {
                // Hide loading indicators
                statefulLayout.currentState = StatefulLayout.State.CONTENT
                refreshLayout.finishRefresh(true)
                refreshLayout.finishLoadMore(true)
            }
            is UiState.Loading.Initial -> {
                // Show initial loading state (e.g., full-screen progress bar)
                statefulLayout.currentState = StatefulLayout.State.LOADING
                refreshLayout.setNoMoreData(false) // 以防之前设置过“没有更多数据了”
            }
            is UiState.Loading.Refresh -> {
                // Show refresh loading state (e.g., SwipeRefreshLayout)
                // 样式由SmartRefreshLayout控制，这里不需要做什么
            }
            is UiState.Loading.Pagination -> {
                // Show pagination loading state (e.g., progress bar at the bottom of the list)
                // 样式由SmartRefreshLayout控制，这里不需要做什么
            }
            /*is UiState.Error.Initial -> {
                // Show initial load error state
            }
            is UiState.Error.Refresh -> {
                // Show refresh error state
            }
            is UiState.Error.Pagination -> {
                // Show pagination error state
            }*/
        }
    }

    @Suppress("CascadeIf")
    private fun handleLoadingState(state: UiState2) {
        val loadingType = state.loadingType

        if (state is Loading) {
            when (loadingType) {
                FULL_SCREEN -> {
                    statefulLayout.currentState = StatefulLayout.State.LOADING
                    refreshLayout.setNoMoreData(false) // 重新加载时重置NoMoreData状态
                }
                PULL_REFRESH -> { /*ignore*/ }
                LOAD_MORE -> { /*ignore*/ }
                NONE -> {}
            }
        } else if (state is Error) {
            when (loadingType) {
                FULL_SCREEN -> statefulLayout.currentState = StatefulLayout.State.ERROR
                PULL_REFRESH -> refreshLayout.finishRefresh(false)
                LOAD_MORE -> refreshLayout.finishLoadMore(false)
                NONE -> {}
            }
        } else if (state is Success<*>) {
            when (loadingType) {
                FULL_SCREEN -> statefulLayout.currentState = StatefulLayout.State.CONTENT
                PULL_REFRESH -> refreshLayout.finishRefresh(true)
                LOAD_MORE -> {
                    refreshLayout.finishLoadMore(true)

                    if (state.data is PageData && state.data.records.isEmpty())
                        refreshLayout.setNoMoreData(true)
                }
                NONE -> {}
            }
        }
    }

    private fun setupSearchBar() {
        val searchBar =
            layoutInflater.inflate(R.layout.search_fixed_area, binding.topFixedLayoutContainer, true)
        val searchEditText = searchBar.findViewById<EditText>(R.id.search_edit_text)
        val searchIcon = searchBar.findViewById<View>(R.id.search_icon)

        // 初始时隐藏搜索图标
        searchIcon.isGone = true
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // 根据输入框是否为空显示或隐藏搜索图标
                searchIcon.isGone = s.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        searchIcon.setOnClickListener {
            val keywords = searchEditText.text.toString()
            //if (keywords.isNotEmpty()) {
            Log.d(TAG, "Search keywords=$keywords")
            viewModel.setKeywords(keywords)
            //}
        }
    }

    private fun setupStatefulLayout() {
        /* binding.statefulLayout被多次引用，所以用一个变量简化它 */
        statefulLayout = binding.statefulLayout
    }

    private fun setupSmartRefreshLayout() {
        refreshLayout = statefulLayout.getContentView().findViewById<SmartRefreshLayout>(R.id.refreshLayout)
            .apply {
                setRefreshHeader(ClassicsHeader(context))
                setRefreshFooter(ClassicsFooter(context))
                setOnRefreshListener { viewModel.refresh() }
                setOnLoadMoreListener { viewModel.loadNextPage() }
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
        recyclerView = statefulLayout.getContentView().findViewById<RecyclerView>(R.id.recyclerView)
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
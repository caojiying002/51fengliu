package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseSingleItemAdapter
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.ItemData
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.PagedItemData
import com.jiyingcao.a51fengliu.api.response.SearchItemData
import com.jiyingcao.a51fengliu.api.response.isEmpty
import com.jiyingcao.a51fengliu.databinding.DefaultLayoutStatefulRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.CityActivity.Companion
import com.jiyingcao.a51fengliu.ui.adapter.ItemDataAdapter
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.util.setEdgeToEdgePaddings
import com.jiyingcao.a51fengliu.viewmodel.LoadingType.*
import com.jiyingcao.a51fengliu.viewmodel.SearchViewModel
import com.jiyingcao.a51fengliu.viewmodel.UiState2
import com.jiyingcao.a51fengliu.viewmodel.UiState2.*
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class SearchActivity: BaseActivity() {
    private lateinit var binding: DefaultLayoutStatefulRecyclerViewBinding
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private lateinit var viewModel: SearchViewModel

    @Deprecated("User recordAdapter instead")
    private lateinit var itemDataAdapter: ItemDataAdapter
    private lateinit var recordAdapter: RecordAdapter

    /** 是否有数据已经加载 */
    @Deprecated("") private var hasDataLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = DefaultLayoutStatefulRecyclerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setEdgeToEdgePaddings(binding.root)

        statefulLayout = binding.statefulLayout // 简化代码调用
        refreshLayout = findViewById(R.id.refreshLayout)
        recyclerView = findViewById(R.id.recyclerView)

        val fixedAreaAdapter = object : BaseSingleItemAdapter<Any, RecyclerView.ViewHolder>() {
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Any?) {}

            override fun onCreateViewHolder(
                context: Context,
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(context)
                    .inflate(R.layout.search_fixed_area, parent, false)
                val searchEditText = itemView.findViewById<EditText>(R.id.search_edit_text)
                val searchIcon = itemView.findViewById<View>(R.id.search_icon)

                // 初始时隐藏搜索图标
                searchIcon.visibility = View.GONE
                searchEditText.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        // 根据输入框是否为空显示或隐藏搜索图标
                        searchIcon.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })

                searchIcon.setOnClickListener {
                    val keywords = searchEditText.text.toString()
                    if (keywords.isNotEmpty()) {
                        Log.d(TAG, "Search keywords=$keywords")
                        viewModel.startNewSearch(keywords)
                    }
                }
                return object : RecyclerView.ViewHolder(itemView) {}
            }

        }
        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { _, _, position ->
                Log.d(TAG, "Record $position clicked")
                recordAdapter.getItem(position)?.let {
                    DetailActivity.start(context, it.id)
                }
            }
        }
        recyclerView.apply {
            // 使用线性布局管理器
            layoutManager = LinearLayoutManager(context)
            // 指定适配器
            adapter = ConcatAdapter(fixedAreaAdapter, recordAdapter)
        }

        refreshLayout.setRefreshHeader(ClassicsHeader(this))
        refreshLayout.setRefreshFooter(ClassicsFooter(this))
        refreshLayout.setOnRefreshListener { viewModel.pullRefresh() }
        refreshLayout.setOnLoadMoreListener { viewModel.loadMore() }
        // refreshLayout.setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        viewModel.uiState.observe(this) { state ->
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
        }

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

    companion object {
        private const val TAG = "SearchActivity"

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, SearchActivity::class.java).apply {
                // putExtra("ITEM_DATA", itemData)
            }
            context.startActivity(intent)
        }
    }
}
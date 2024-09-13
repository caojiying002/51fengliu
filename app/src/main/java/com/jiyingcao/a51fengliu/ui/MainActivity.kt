package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseSingleItemAdapter
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.databinding.DefaultLayoutStatefulRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.*
import com.jiyingcao.a51fengliu.viewmodel.MainViewModel
import com.jiyingcao.a51fengliu.viewmodel.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class MainActivity : BaseActivity() {
    private lateinit var binding: DefaultLayoutStatefulRecyclerViewBinding
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private lateinit var viewModel: MainViewModel

    private lateinit var recordAdapter: RecordAdapter

    /** 是否有数据已经加载 */
    private var hasDataLoaded: Boolean = false
    /** 当前已经加载成功的页数 */
    private var currentPage: Int = 0

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
                    .inflate(R.layout.main_fixed_area, parent, false)
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
            adapter = recordAdapter
        }
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.fetchByPage(false) }
            setOnLoadMoreListener { viewModel.fetchByPage(false, currentPage+1) }
            // setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.data.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // 显示加载动画
                    if (!hasDataLoaded)
                        statefulLayout.currentState = LOADING
                }
                is UiState.Success -> {
                    hasDataLoaded = true
                    refreshLayout.finishRefresh()
                    refreshLayout.finishLoadMore()
                    statefulLayout.currentState = CONTENT

                    val pageData = state.data
                    val page = pageData.current
                    val data = pageData.records

                    // 记录页数
                    currentPage = page
                    // 显示数据
                    if (page == 1) {
                        recordAdapter.submitList(data)
                        // refreshLayout.setEnableLoadMore(true)   // 第一页有数据了，可以启用LoadMore了
                    }
                    else
                        recordAdapter.addAll(data)
                    // TODO 如果列表为空需要显示空状态
                }
                is UiState.Empty -> {
                    // 不再使用
                    //refreshLayout.finishRefresh()
                }
                is UiState.Error -> {
                    refreshLayout.finishRefresh()
                    refreshLayout.finishLoadMore()
                    // 显示错误信息
                    if (!hasDataLoaded)
                        statefulLayout.currentState = ERROR
                    else
                        Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fetchByPage(true)

        findViewById<View>(R.id.title_bar_menu)?.setOnClickListener { CityActivity.start(this) }
        findViewById<View>(R.id.title_bar_profile)?.setOnClickListener { SearchActivity.start(this) }
    }

    companion object {
        private const val TAG: String = "MainActivity"
    }
}
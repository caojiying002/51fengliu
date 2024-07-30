package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.ItemData
import com.jiyingcao.a51fengliu.databinding.DefaultLayoutStatefulRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.ItemDataAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.*
import com.jiyingcao.a51fengliu.util.setEdgeToEdgePaddings
import com.jiyingcao.a51fengliu.viewmodel.CityViewModel
import com.jiyingcao.a51fengliu.viewmodel.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class CityActivity: BaseActivity() {
    private lateinit var binding: DefaultLayoutStatefulRecyclerViewBinding
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private lateinit var viewModel: CityViewModel

    private lateinit var itemDataAdapter: ItemDataAdapter

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
        itemDataAdapter = ItemDataAdapter().apply {
            setOnItemClickListener { _, _, position ->
                Log.d(TAG, "Item $position clicked")
                itemDataAdapter.getItem(position)?.let {
                    if (it.id == 1) { return@let }  // "快活林APP已推出，欢迎下载" 不处理点击
                    // DetailActivity.start(context, it)
                }
            }
        }
        recyclerView.apply {
            // 使用线性布局管理器
            layoutManager = LinearLayoutManager(context)
            // 指定适配器
            adapter = itemDataAdapter
        }

        refreshLayout.setRefreshHeader(ClassicsHeader(this))
        refreshLayout.setRefreshFooter(ClassicsFooter(this))
        refreshLayout.setOnRefreshListener { viewModel.fetchCityDataByPage(1) }
        refreshLayout.setOnLoadMoreListener { viewModel.fetchCityDataByPage(currentPage+1) }
        // refreshLayout.setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
        viewModel = ViewModelProvider(this)[CityViewModel::class.java]
        viewModel.data.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // 第一次加载时显示全屏加载动画
                    if (!hasDataLoaded) statefulLayout.currentState = LOADING
                }
                is UiState.Success -> {
                    hasDataLoaded = true
                    refreshLayout.finishRefresh()
                    refreshLayout.finishLoadMore()
                    statefulLayout.currentState = CONTENT

                    val page = state.data.page
                    val data: List<ItemData> = state.data.data
                    // 记录页数
                    currentPage = page
                    // 显示数据
                    if (page == 1) {
                        itemDataAdapter.submitList(data)
                        // refreshLayout.setEnableLoadMore(true)   // 第一页有数据了，可以启用LoadMore了
                    }
                    else
                        itemDataAdapter.addAll(data)
                }
                is UiState.Empty -> {
                    // 不再使用
                    //refreshLayout.finishRefresh()
                }
                is UiState.Error -> {
                    refreshLayout.finishRefresh()
                    refreshLayout.finishLoadMore()
                    // 显示错误状态
                    statefulLayout.currentState = ERROR
                }
            }
        }

        viewModel.fetchCityDataByPage(1)
    }

    companion object {
        private const val TAG = "CityActivity"

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, CityActivity::class.java).apply {
                // putExtra("ITEM_DATA", itemData)
            }
            context.startActivity(intent)
        }
    }
}
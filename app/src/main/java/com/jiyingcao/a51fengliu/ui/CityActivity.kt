package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.databinding.DefaultLayoutStatefulRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.*
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

    private lateinit var recordAdapter: RecordAdapter

    /** 是否有数据已经加载 */
    private var hasDataLoaded: Boolean = false
    /** 当前已经加载成功的页数 */
    private var currentPage: Int = 0
    private var cityCode = "330100"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = DefaultLayoutStatefulRecyclerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setEdgeToEdgePaddings(binding.root)

        statefulLayout = binding.statefulLayout // 简化代码调用
        refreshLayout = findViewById(R.id.refreshLayout)
        recyclerView = findViewById(R.id.recyclerView)
        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { _, _, position ->
                Log.d(TAG, "Record $position clicked")
                this@apply.getItem(position)?.let {
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
            setOnRefreshListener { viewModel.fetchCityDataByPage(cityCode, 1) }
            setOnLoadMoreListener { viewModel.fetchCityDataByPage(cityCode, currentPage+1) }
            // setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
        }

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

                    // TODO 如果没有新数据了需要禁用loadMore
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

        viewModel.fetchCityDataByPage(cityCode, 1)

        findViewById<View>(R.id.title_bar_menu)?.setOnClickListener {
            val intent = ChooseCityActivity.createIntent(this)
            startActivityForResult(intent, 42)  // TODO 管理requestCode和bundle key
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            val cityCode = data?.getStringExtra("CITY_CODE")
            Log.d(TAG, "City code selected: $cityCode")
            cityCode?.let {
                if (it == this.cityCode) return

                // 清空数据
                recordAdapter.submitList(emptyList())
                hasDataLoaded = false
                currentPage = 0

                this.cityCode = it
                viewModel.fetchCityDataByPage(it, 1)
            }
        }
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
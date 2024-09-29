package com.jiyingcao.a51fengliu.ui.tab

import android.annotation.SuppressLint
import android.widget.TextView
import androidx.fragment.app.Fragment
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
import com.jiyingcao.a51fengliu.ui.CityActivity
import com.jiyingcao.a51fengliu.ui.DetailActivity
import com.jiyingcao.a51fengliu.ui.SearchActivity
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.*
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.MainViewModel
import com.jiyingcao.a51fengliu.viewmodel.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class HomeFragment : Fragment() {
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private lateinit var viewModel: MainViewModel

    private lateinit var recordAdapter: RecordAdapter

    /** 是否有数据已经加载 */
    private var hasDataLoaded: Boolean = false
    /** 当前已经加载成功的页数 */
    private var currentPage: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.default_layout_stateful_recycler_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statefulLayout = view.findViewById(R.id.stateful_layout)
        refreshLayout = view.findViewById(R.id.refreshLayout)
        recyclerView = view.findViewById(R.id.recyclerView)

        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { _, _, position ->
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
            setOnRefreshListener { viewModel.fetchByPage(false) }
            setOnLoadMoreListener { viewModel.fetchByPage(false, currentPage+1) }
            // setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.data.observe(viewLifecycleOwner) { state ->
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
                        view.context.showToast(state.message)
                }
            }
        }

        viewModel.fetchByPage(true)

        // TODO 使用fragment.startActivity()
        view.findViewById<View>(R.id.title_bar_menu)?.setOnClickListener { CityActivity.start(view.context) }
        view.findViewById<View>(R.id.title_bar_profile)?.setOnClickListener { SearchActivity.start(view.context) }
    }

    override fun onResume() {
        super.onResume()
        // Fragment 变为可见时的逻辑
    }

    override fun onPause() {
        super.onPause()
        // Fragment 变为不可见时的逻辑
    }
}
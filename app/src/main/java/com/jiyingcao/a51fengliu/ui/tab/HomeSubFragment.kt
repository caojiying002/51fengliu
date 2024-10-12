package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.CityActivity
import com.jiyingcao.a51fengliu.ui.DetailActivity
import com.jiyingcao.a51fengliu.ui.SearchActivity
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.CONTENT
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.ERROR
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.LOADING
import com.jiyingcao.a51fengliu.util.FirstResumeLifecycleObserver
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.MainViewModel
import com.jiyingcao.a51fengliu.viewmodel.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class HomeSubFragment : Fragment(),
    FirstResumeLifecycleObserver.FirstResumeListener {

    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private val viewModel: MainViewModel by viewModels()

    private lateinit var recordAdapter: RecordAdapter

    /** daily热门，publish最新 */
    private lateinit var sort: String
    /** 是否有数据已经加载 */
    private var hasDataLoaded: Boolean = false
    /** 当前已经加载成功的页数 */
    private var currentPage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 添加监听器实现第一次 onResume 事件
        lifecycle.addObserver(FirstResumeLifecycleObserver(this))
        sort = arguments?.getString(ARG_SORT) ?: "daily"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.common_stateful_refresh_recycler_view, container, false)
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
            // 设置固定大小
            setHasFixedSize(true)
            // 使用线性布局管理器
            layoutManager = LinearLayoutManager(context)
            // 指定适配器
            adapter = recordAdapter
        }
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.fetchByPage(showFullScreenLoading = false, sort = sort) }
            setOnLoadMoreListener { viewModel.fetchByPage(false, sort, currentPage+1) }
            // setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
        }

        //viewModel = ViewModelProvider(this)[MainViewModel::class.java]
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

        // TODO 使用fragment.startActivity()
        view.findViewById<View>(R.id.title_bar_menu)?.setOnClickListener { CityActivity.start(view.context) }
        view.findViewById<View>(R.id.title_bar_profile)?.setOnClickListener { SearchActivity.start(view.context) }
    }

    override fun onFirstResume(isRecreate: Boolean) {
        // 重新创建时不加载数据
        if (!isRecreate) {
            // 第一次 onResume 事件发生时加载数据
            viewModel.fetchByPage(showFullScreenLoading = true, sort = sort)
        }
    }

    override fun onResume() {
        super.onResume()
        onFragmentVisible()
    }

    override fun onPause() {
        super.onPause()
        onFragmentInvisible()
    }

    private fun onFragmentVisible() {
        // Fragment 变为可见时的逻辑
        Log.d("HomeSubFragment", "${arguments?.getString(ARG_SORT)} is now visible")
    }

    private fun onFragmentInvisible() {
        // Fragment 变为不可见时的逻辑
        Log.d("HomeSubFragment", "${arguments?.getString(ARG_SORT)} is now invisible")
    }

    companion object {
        private const val ARG_SORT = "sort"
        fun newInstance(sort: String): HomeSubFragment {
            return HomeSubFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, sort)
                }
            }
        }
    }
}
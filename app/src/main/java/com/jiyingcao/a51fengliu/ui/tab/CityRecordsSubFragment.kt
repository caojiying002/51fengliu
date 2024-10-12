package com.jiyingcao.a51fengliu.ui.tab

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.databinding.DefaultLayoutStatefulRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.ChooseCityActivity
import com.jiyingcao.a51fengliu.ui.DetailActivity
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.CONTENT
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.ERROR
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.LOADING
import com.jiyingcao.a51fengliu.util.FirstResumeLifecycleObserver
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.CityViewModel
import com.jiyingcao.a51fengliu.viewmodel.UiState
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class CityRecordsSubFragment : Fragment(),
    FirstResumeLifecycleObserver.FirstResumeListener {
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private val viewModel: CityViewModel by viewModels()

    private lateinit var recordAdapter: RecordAdapter

    /** publish最新发布，weekly一周热门，monthly本月热门，lastMonth上月热门 */
    private lateinit var sort: String
    /** 是否有数据已经加载 */
    private var hasDataLoaded: Boolean = false
    /** 当前已经加载成功的页数 */
    private var currentPage: Int = 0
    /** 当前城市区域代码 */
    private var cityCode = "330100"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 添加监听器实现第一次 onResume 事件
        lifecycle.addObserver(FirstResumeLifecycleObserver(this))
        sort = arguments?.getString(ARG_SORT) ?: "publish"
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
            setOnRefreshListener { viewModel.fetchCityDataByPage(cityCode, sort, 1) }
            setOnLoadMoreListener { viewModel.fetchCityDataByPage(cityCode, sort, currentPage+1) }
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

        view.findViewById<View>(R.id.title_bar_menu)?.setOnClickListener { v ->
            val intent = ChooseCityActivity.createIntent(v.context)
            startActivityForResult(intent, 42)  // TODO 管理requestCode和bundle key
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            val cityCode = data?.getStringExtra("CITY_CODE")
            Log.d("CityRecordsSubFragment", "City code selected: $cityCode")
            cityCode?.let {
                if (it == this.cityCode) return

                // 清空数据
                recordAdapter.submitList(emptyList())
                hasDataLoaded = false
                currentPage = 0

                this.cityCode = it
                viewModel.fetchCityDataByPage(it, sort, 1)
            }
        }
    }

    override fun onFirstResume(isRecreate: Boolean) {
        // 重新创建时不加载数据
        if (!isRecreate) {
            // 第一次 onResume 事件发生时加载数据
            viewModel.fetchCityDataByPage(cityCode, sort, 1)
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
        Log.d("CityRecordsSubFragment", "${arguments?.getString(ARG_SORT)} is now visible")
    }

    private fun onFragmentInvisible() {
        // Fragment 变为不可见时的逻辑
        Log.d("CityRecordsSubFragment", "${arguments?.getString(ARG_SORT)} is now invisible")
    }


    companion object {
        private const val ARG_SORT = "sort"
        fun newInstance(sort: String): CityRecordsSubFragment {
            return CityRecordsSubFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, sort)
                }
            }
        }
    }
}
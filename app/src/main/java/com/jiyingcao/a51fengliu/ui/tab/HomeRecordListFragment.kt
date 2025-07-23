package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.databinding.StatefulViewpager2RecyclerViewBinding
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.DetailActivity
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.showContentView
import com.jiyingcao.a51fengliu.ui.showEmptyContent
import com.jiyingcao.a51fengliu.ui.showErrorView
import com.jiyingcao.a51fengliu.ui.showLoadingView
import com.jiyingcao.a51fengliu.ui.showRealContent
import com.jiyingcao.a51fengliu.util.AppLogger
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.HomeRecordListIntent
import com.jiyingcao.a51fengliu.viewmodel.HomeRecordListUiState
import com.jiyingcao.a51fengliu.viewmodel.HomeRecordListViewModel
import com.jiyingcao.a51fengliu.viewmodel.HomeRecordListViewModelFactory
import com.jiyingcao.a51fengliu.viewmodel.LoadingType
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class HomeRecordListFragment : Fragment() {

    private var _binding: StatefulViewpager2RecyclerViewBinding? = null
    private val binding get() = _binding!!

    private val refreshLayout: SmartRefreshLayout get() = binding.refreshLayout
    private val recyclerView: RecyclerView get() = binding.recyclerView

    /** daily热门，publish最新 */
    private lateinit var sort: String

    private val viewModel: HomeRecordListViewModel by viewModels {
        HomeRecordListViewModelFactory(
            sort,
            RecordRepository.getInstance(RetrofitClient.apiService)
        )
    }

    private lateinit var recordAdapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sort = arguments?.getString(ARG_SORT) ?: "daily"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = StatefulViewpager2RecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSmartRefreshLayout()
        observeUiState()
    }
    
    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { record, position ->
                startActivity(DetailActivity.createIntent(requireContext(), record.id))
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
    }
    
    private fun setupSmartRefreshLayout() {
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.processIntent(HomeRecordListIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(HomeRecordListIntent.LoadMore) }
            // setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
        }
    }

    private fun observeUiState() {
        // 监听单一UI状态
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { uiState ->
                    // 更新记录数据
                    recordAdapter.submitList(uiState.records)

                    // 处理加载状态
                    when {
                        uiState.showFullScreenLoading -> binding.showLoadingView()
                        uiState.showFullScreenError -> {
                            binding.showErrorView(uiState.errorMessage) {
                                viewModel.processIntent(HomeRecordListIntent.Retry)
                            }
                        }

                        uiState.showEmpty -> binding.showEmptyContent()
                        uiState.showContent -> binding.showRealContent()
                    }

                    // 精确处理刷新状态 - 只处理下拉刷新相关的状态变化
                    handleRefreshState(uiState)
                    
                    // 精确处理加载更多状态 - 只处理上拉加载相关的状态变化
                    handleLoadMoreState(uiState)

                    // 设置是否还有更多数据
                    refreshLayout.setNoMoreData(uiState.noMoreData)

                    // 处理错误消息（非全屏错误）
                    if (uiState.isError && !uiState.showFullScreenError) {
                        requireContext().showToast(uiState.errorMessage)
                    }
                }
            }
        }
    }

    private fun handleRefreshState(uiState: HomeRecordListUiState) {
        when {
            uiState.isRefreshing -> {
                // 下拉刷新进行中，SmartRefreshLayout 自动处理
            }
            !uiState.isRefreshing && uiState.loadingType == LoadingType.PULL_TO_REFRESH -> {
                // 下拉刷新结束（成功或失败）
                refreshLayout.finishRefresh(!uiState.isError)
            }
            // 其他情况不处理 refreshLayout
        }
    }

    private fun handleLoadMoreState(uiState: HomeRecordListUiState) {
        when {
            uiState.isLoadingMore -> {
                // 上拉加载进行中，SmartRefreshLayout 自动处理
            }
            !uiState.isLoadingMore && uiState.loadingType == LoadingType.LOAD_MORE -> {
                // 上拉加载结束（成功或失败）
                refreshLayout.finishLoadMore(!uiState.isError)
            }
            // 其他情况不处理 refreshLayout
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.processIntent(HomeRecordListIntent.InitialLoad)
        onFragmentVisible()
    }

    override fun onPause() {
        super.onPause()
        onFragmentInvisible()
    }

    private fun onFragmentVisible() {
        // Fragment 变为可见时的逻辑
        AppLogger.d(TAG, "${arguments?.getString(ARG_SORT)} is now visible")
    }

    private fun onFragmentInvisible() {
        // Fragment 变为不可见时的逻辑
        AppLogger.d(TAG, "${arguments?.getString(ARG_SORT)} is now invisible")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeRecordListFragment"
        private const val ARG_SORT = "sort"
        
        fun newInstance(sort: String): HomeRecordListFragment {
            return HomeRecordListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, sort)
                }
            }
        }
    }
}
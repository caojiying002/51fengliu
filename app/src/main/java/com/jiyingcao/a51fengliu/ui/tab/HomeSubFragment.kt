package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
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
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.HomeIntent
import com.jiyingcao.a51fengliu.viewmodel.HomeState
import com.jiyingcao.a51fengliu.viewmodel.HomeViewModel
import com.jiyingcao.a51fengliu.viewmodel.HomeViewModelFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class HomeSubFragment : Fragment() {

    private var _binding: StatefulViewpager2RecyclerViewBinding? = null
    private val binding get() = _binding!!

    private val refreshLayout: SmartRefreshLayout get() = binding.refreshLayout
    private val recyclerView: RecyclerView get() = binding.recyclerView

    /** daily热门，publish最新 */
    private lateinit var sort: String

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            RecordRepository.getInstance(RetrofitClient.apiService),
            sort
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
        setupFlowCollectors()
    }
    
    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { _, _, position ->
                this.getItem(position)?.let {
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
    }
    
    private fun setupSmartRefreshLayout() {
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.processIntent(HomeIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(HomeIntent.LoadMore) }
            // setEnableLoadMore(false)  // 加载第一页成功前暂时禁用LoadMore
        }
    }

    private fun setupFlowCollectors() {
        // 监听ViewModel状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is HomeState.Loading -> handleLoadingState(state)
                    is HomeState.Error -> handleErrorState(state)
                    is HomeState.Success -> {
                        binding.showContentView()
                        refreshLayout.finishRefresh()
                        refreshLayout.finishLoadMore()
                    }
                    else -> {}
                }
            }
        }
        
        // 监听记录数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.records.collect { records ->
                recordAdapter.submitList(records)
                
                // 如果没有数据，显示空状态
                if (records.isEmpty()) {
                    binding.showEmptyContent()
                } else {
                    binding.showRealContent()
                }
            }
        }
        
        // 监听是否还有更多数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noMoreDataState.collect { noMoreData ->
                refreshLayout.setNoMoreData(noMoreData)
            }
        }
    }
    
    private fun handleLoadingState(loading: HomeState.Loading) {
        when (loading) {
            HomeState.Loading.FullScreen -> binding.showLoadingView()
            HomeState.Loading.PullToRefresh -> { /* 下拉刷新加载处理 */ }
            HomeState.Loading.LoadMore -> { /* 加载更多处理 */ }
        }
    }
    
    private fun handleErrorState(error: HomeState.Error) {
        when (error) {
            is HomeState.Error.FullScreen -> {
                binding.showErrorView(error.message) {
                    viewModel.processIntent(HomeIntent.Retry)
                }
            }
            is HomeState.Error.PullToRefresh -> {
                refreshLayout.finishRefresh(false)
                requireContext().showToast(error.message)
            }
            is HomeState.Error.LoadMore -> {
                refreshLayout.finishLoadMore(false)
                requireContext().showToast(error.message)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.setUIVisibility(true)
        onFragmentVisible()
    }

    override fun onPause() {
        super.onPause()
        viewModel.setUIVisibility(false)
        onFragmentInvisible()
    }

    private fun onFragmentVisible() {
        // Fragment 变为可见时的逻辑
        Log.d(TAG, "${arguments?.getString(ARG_SORT)} is now visible")
    }

    private fun onFragmentInvisible() {
        // Fragment 变为不可见时的逻辑
        Log.d(TAG, "${arguments?.getString(ARG_SORT)} is now invisible")
    }

    companion object {
        private const val TAG = "HomeSubFragment"
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
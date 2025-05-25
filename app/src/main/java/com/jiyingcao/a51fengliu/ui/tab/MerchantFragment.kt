package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.databinding.FragmentMerchantBinding
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.adapter.MerchantAdapter
import com.jiyingcao.a51fengliu.ui.showContentView
import com.jiyingcao.a51fengliu.ui.showEmptyContent
import com.jiyingcao.a51fengliu.ui.showErrorView
import com.jiyingcao.a51fengliu.ui.showLoadingView
import com.jiyingcao.a51fengliu.ui.showRealContent
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.MerchantIntent
import com.jiyingcao.a51fengliu.viewmodel.MerchantState
import com.jiyingcao.a51fengliu.viewmodel.MerchantViewModel
import com.jiyingcao.a51fengliu.viewmodel.MerchantViewModelFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class MerchantFragment : Fragment() {
    private var _binding: FragmentMerchantBinding? = null
    private val binding get() = _binding!!

    private val statefulBinding: StatefulRefreshRecyclerViewBinding
        get() = binding.statefulContent

    private val refreshLayout: SmartRefreshLayout get() = statefulBinding.refreshLayout
    private val recyclerView: RecyclerView get() = statefulBinding.recyclerView

    private lateinit var merchantAdapter: MerchantAdapter

    private val viewModel: MerchantViewModel by viewModels {
        MerchantViewModelFactory(
            RecordRepository.getInstance(RetrofitClient.apiService)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMerchantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSmartRefreshLayout()
        setupFlowCollectors()
    }

    private fun setupRecyclerView() {
        merchantAdapter = MerchantAdapter().apply {
            setOnItemClickListener { merchant, position ->
                // TODO 打开商家详情
            }
        }
        
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = merchantAdapter
        }
    }

    private fun setupSmartRefreshLayout() {
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.processIntent(MerchantIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(MerchantIntent.LoadMore) }
        }
    }

    private fun setupFlowCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                handleStateChange(state)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.merchants.collect { merchants ->
                merchantAdapter.submitList(merchants)

                // 如果没有数据，显示空状态
                if (merchants.isEmpty()) {
                    statefulBinding.showEmptyContent()
                } else {
                    statefulBinding.showRealContent()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noMoreDataState.collect { noMoreData ->
                refreshLayout.setNoMoreData(noMoreData)
            }
        }
    }

    private fun handleStateChange(state: MerchantState) {
        when (state) {
            is MerchantState.Loading -> handleLoadingState(state)
            is MerchantState.Error -> handleErrorState(state)
            is MerchantState.Success -> {
                statefulBinding.showContentView()
                refreshLayout.finishRefresh()
                refreshLayout.finishLoadMore()
            }
            else -> {}
        }
    }

    private fun handleLoadingState(loading: MerchantState.Loading) {
        when (loading) {
            MerchantState.Loading.FullScreen -> statefulBinding.showLoadingView()
            MerchantState.Loading.PullToRefresh -> { /* 下拉刷新加载处理 */ }
            MerchantState.Loading.LoadMore -> { /* 加载更多处理 */ }
        }
    }

    private fun handleErrorState(error: MerchantState.Error) {
        when (error) {
            is MerchantState.Error.FullScreen -> {
                statefulBinding.showErrorView(error.message) {
                    viewModel.processIntent(MerchantIntent.Retry)
                }
            }
            is MerchantState.Error.PullToRefresh -> {
                refreshLayout.finishRefresh(false)
                requireContext().showToast(error.message)
            }
            is MerchantState.Error.LoadMore -> {
                refreshLayout.finishLoadMore(false)
                requireContext().showToast(error.message)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.processIntent(MerchantIntent.InitialLoad)
    }

    override fun onPause() {
        super.onPause()
        // Fragment 变为不可见时的逻辑
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
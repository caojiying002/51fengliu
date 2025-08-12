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
import com.jiyingcao.a51fengliu.databinding.FragmentMerchantBinding
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.MerchantDetailActivity
import com.jiyingcao.a51fengliu.ui.adapter.MerchantAdapter
import com.jiyingcao.a51fengliu.ui.compose.ComposeContainerActivity
import com.jiyingcao.a51fengliu.ui.showContentView
import com.jiyingcao.a51fengliu.ui.showEmptyContent
import com.jiyingcao.a51fengliu.ui.showErrorView
import com.jiyingcao.a51fengliu.ui.showLoadingView
import com.jiyingcao.a51fengliu.ui.showRealContent
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.LoadingType
import com.jiyingcao.a51fengliu.viewmodel.MerchantListIntent
import com.jiyingcao.a51fengliu.viewmodel.MerchantListUiState
import com.jiyingcao.a51fengliu.viewmodel.MerchantListViewModel
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MerchantListFragment : Fragment() {
    private var _binding: FragmentMerchantBinding? = null
    private val binding get() = _binding!!

    private val statefulBinding: StatefulRefreshRecyclerViewBinding
        get() = binding.statefulContent

    private val refreshLayout: SmartRefreshLayout get() = statefulBinding.refreshLayout
    private val recyclerView: RecyclerView get() = statefulBinding.recyclerView

    private lateinit var merchantAdapter: MerchantAdapter

    private val viewModel: MerchantListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMerchantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupRecyclerView()
        setupSmartRefreshLayout()
        setupFlowCollectors()
    }

    private fun setupClickListeners() {
        binding.titleBarChooseCity.setOnClickListener {
            // TODO 选择城市
        }
    }

    private fun setupRecyclerView() {
        merchantAdapter = MerchantAdapter().apply {
            setOnItemClickListener { merchant, position ->
                //startActivity(ComposeContainerActivity.createMerchantDetailIntent(requireContext(), merchant.id))
                startActivity(MerchantDetailActivity.createIntent(requireContext(), merchant.id))
            }
        }
        
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = merchantAdapter
            // 禁用动画
            itemAnimator = null
        }
    }

    private fun setupSmartRefreshLayout() {
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.processIntent(MerchantListIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(MerchantListIntent.LoadMore) }
        }
    }

    private fun setupFlowCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { uiState ->
                    // 更新商户数据
                    merchantAdapter.submitList(uiState.merchants)

                    // 处理加载状态
                    when {
                        uiState.showFullScreenLoading -> statefulBinding.showLoadingView()
                        uiState.showFullScreenError -> {
                            statefulBinding.showErrorView(uiState.errorMessage) {
                                viewModel.processIntent(MerchantListIntent.Retry)
                            }
                        }
                        uiState.showEmpty -> statefulBinding.showEmptyContent()
                        uiState.showContent -> statefulBinding.showRealContent()
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

    private fun handleRefreshState(uiState: MerchantListUiState) {
        when {
            uiState.isRefreshing -> {
                // 下拉刷新进行中，SmartRefreshLayout 自动处理
            }
            !uiState.isRefreshing && uiState.loadingType == LoadingType.PULL_TO_REFRESH -> {
                // 下拉刷新结束（无论成功失败）
                refreshLayout.finishRefresh(!uiState.isError)
            }
            // 其他情况不处理 refreshLayout
        }
    }

    private fun handleLoadMoreState(uiState: MerchantListUiState) {
        when {
            uiState.isLoadingMore -> {
                // 上拉加载进行中，SmartRefreshLayout 自动处理
            }
            !uiState.isLoadingMore && uiState.loadingType == LoadingType.LOAD_MORE -> {
                // 上拉加载结束（无论成功失败）
                refreshLayout.finishLoadMore(!uiState.isError)
            }
            // 其他情况不处理 refreshLayout
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.processIntent(MerchantListIntent.InitialLoad)
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
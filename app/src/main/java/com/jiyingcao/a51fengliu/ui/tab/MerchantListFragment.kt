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
import com.jiyingcao.a51fengliu.databinding.FragmentMerchantBinding
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.repository.MerchantRepository
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
import com.jiyingcao.a51fengliu.viewmodel.MerchantListViewModel
import com.jiyingcao.a51fengliu.viewmodel.MerchantListViewModelFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class MerchantListFragment : Fragment() {
    private var _binding: FragmentMerchantBinding? = null
    private val binding get() = _binding!!

    private val statefulBinding: StatefulRefreshRecyclerViewBinding
        get() = binding.statefulContent

    private val refreshLayout: SmartRefreshLayout get() = statefulBinding.refreshLayout
    private val recyclerView: RecyclerView get() = statefulBinding.recyclerView

    private lateinit var merchantAdapter: MerchantAdapter

    private val viewModel: MerchantListViewModel by viewModels {
        MerchantListViewModelFactory(
            MerchantRepository.getInstance(RetrofitClient.apiService)
        )
    }

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
                startActivity(ComposeContainerActivity.createMerchantDetailIntent(requireContext(), merchant.id))
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
                        uiState.showContent -> {
                            statefulBinding.showContentView()
                            statefulBinding.showRealContent()
                        }
                    }

                    // 处理刷新状态
                    if (uiState.isRefreshing) {
                        // 下拉刷新中 - SmartRefreshLayout 自动处理
                    } else {
                        refreshLayout.finishRefresh(!uiState.isError)
                    }

                    // 处理加载更多状态
                    if (uiState.isLoadingMore) {
                        // 加载更多中 - SmartRefreshLayout 自动处理
                    } else {
                        refreshLayout.finishLoadMore(!uiState.isError)
                    }

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
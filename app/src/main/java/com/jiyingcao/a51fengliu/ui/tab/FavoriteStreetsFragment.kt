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
import com.jiyingcao.a51fengliu.databinding.StatefulViewpager2RecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.StreetAdapter
import com.jiyingcao.a51fengliu.ui.showEmptyContent
import com.jiyingcao.a51fengliu.ui.showErrorView
import com.jiyingcao.a51fengliu.ui.showLoadingView
import com.jiyingcao.a51fengliu.ui.showRealContent
import com.jiyingcao.a51fengliu.util.AppLogger
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.FavoriteStreetsIntent
import com.jiyingcao.a51fengliu.viewmodel.FavoriteStreetsUiState
import com.jiyingcao.a51fengliu.viewmodel.FavoriteStreetsViewModel
import com.jiyingcao.a51fengliu.viewmodel.LoadingType
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 收藏的暗巷列表Fragment
 * 
 * 命名说明：
 * 虽然项目中有 XXXListFragment 模式，但在 Favorite 这个特定上下文中：
 * - Favorite 前缀已经限定了数据范围（收藏的）
 * - Streets 复数形式已经暗示列表性质
 * - 避免过度冗长的命名 (FavoriteStreetListFragment)
 */
@AndroidEntryPoint
class FavoriteStreetsFragment : Fragment() {

    private var _binding: StatefulViewpager2RecyclerViewBinding? = null
    private val binding get() = _binding!!

    private val refreshLayout: SmartRefreshLayout get() = binding.refreshLayout
    private val recyclerView: RecyclerView get() = binding.recyclerView

    private lateinit var streetAdapter: StreetAdapter

    private val viewModel by viewModels<FavoriteStreetsViewModel>()

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

    override fun onResume() {
        super.onResume()
        // Fragment可见时懒加载数据
        viewModel.processIntent(FavoriteStreetsIntent.InitialLoad)
    }

    private fun setupRecyclerView() {
        streetAdapter = StreetAdapter().apply {
            setOnItemClickListener { street, position ->
                // TODO: 实现跳转到暗巷详情页面
                requireContext().showToast("点击了暗巷: ${street.title}")
            }
        }
        
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = streetAdapter
        }
    }

    private fun setupSmartRefreshLayout() {
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.processIntent(FavoriteStreetsIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(FavoriteStreetsIntent.LoadMore) }
        }
    }

    /**
     * 单一状态流观察 - 所有UI状态变化都在一个地方处理，便于维护和调试
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { uiState ->
                    // 处理数据展示
                    streetAdapter.submitList(uiState.streets)

                    // 处理各种UI状态 - 使用when表达式确保所有状态都被处理
                    when {
                        uiState.showFullScreenLoading -> {
                            binding.showLoadingView()
                        }

                        uiState.showFullScreenError -> {
                            binding.showErrorView(uiState.errorMessage) {
                                viewModel.processIntent(FavoriteStreetsIntent.Retry)
                            }
                        }

                        uiState.showFullScreenEmpty -> {
                            binding.showEmptyContent()
                        }

                        uiState.showContent -> {
                            binding.showRealContent()
                        }
                    }

                    // 精确处理刷新状态 - 只处理下拉刷新相关的状态变化
                    handleRefreshState(uiState)
                    
                    // 精确处理加载更多状态 - 只处理上拉加载相关的状态变化
                    handleLoadMoreState(uiState)

                    // 处理无更多数据状态
                    refreshLayout.setNoMoreData(uiState.noMoreData)

                    // 处理错误提示 - 只对非全屏错误显示Toast
                    if (uiState.isError && !uiState.showFullScreenError) {
                        requireContext().showToast(uiState.errorMessage)
                    }
                }
            }
        }
    }

    private fun handleRefreshState(uiState: FavoriteStreetsUiState) {
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

    private fun handleLoadMoreState(uiState: FavoriteStreetsUiState) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "FavoriteStreetsFragment"
    }
}
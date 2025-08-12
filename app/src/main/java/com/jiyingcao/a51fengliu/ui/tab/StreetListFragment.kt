package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.jiyingcao.a51fengliu.viewmodel.CitySelectionViewModel
import com.jiyingcao.a51fengliu.viewmodel.LoadingType
import com.jiyingcao.a51fengliu.viewmodel.StreetListIntent
import com.jiyingcao.a51fengliu.viewmodel.StreetListUiState
import com.jiyingcao.a51fengliu.viewmodel.StreetListViewModel
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StreetListFragment : Fragment() {

    private var _binding: StatefulViewpager2RecyclerViewBinding? = null
    private val binding get() = _binding!!

    private val refreshLayout: SmartRefreshLayout get() = binding.refreshLayout
    private val recyclerView: RecyclerView get() = binding.recyclerView

    /** publish最新发布，weekly一周热门，monthly本月热门，lastMonth上月热门 */
    private lateinit var sort: String

    /**
     * The ViewModel for this fragment.
     *
     * 用于保存当前城市的暗巷列表数据。
     */
    private val viewModel: StreetListViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<StreetListViewModel.Factory> { factory ->
                factory.create(sort = sort)
            }
        }
    )

    /**
     * Saving selected city, shared with other [StreetListFragment] instances.
     *
     * 保存选中的城市，与其他 [StreetListFragment] 实例共享。
     */
    private val citySelectionViewModel: CitySelectionViewModel by activityViewModels {
        CitySelectionViewModel.Factory()
    }

    private lateinit var streetAdapter: StreetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sort = arguments?.getString(ARG_SORT) ?: "publish"
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
        streetAdapter = StreetAdapter().apply {
            setOnItemClickListener { street, position ->
                // TODO: 跳转到暗巷详情页
                AppLogger.d(TAG, "Street item clicked: ${street.title}, id: ${street.id}")
            }
        }
        
        recyclerView.apply {
            // 设置固定大小
            setHasFixedSize(true)
            // 使用线性布局管理器
            layoutManager = LinearLayoutManager(context)
            // 指定适配器
            adapter = streetAdapter
        }
    }
    
    private fun setupSmartRefreshLayout() {
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.processIntent(StreetListIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(StreetListIntent.LoadMore) }
        }
    }

    private fun observeUiState() {
        // 监听共享的城市选择状态
        // 注：这是对严格MVI的实用性妥协，因为城市选择需要跨Fragment共享
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                citySelectionViewModel.selectedCitySharedFlow
                    .distinctUntilChanged()
                    .collect { cityCode ->
                        AppLogger.d(
                            TAG,
                            "$TAG@${this@StreetListFragment.hashCode()}: city code selected: $cityCode"
                        )
                        // 特殊逻辑：null表示用户未选择城市，转换为空字符串(CITY_CODE_ALL_CITIES)传递给ViewModel
                        // ViewModel将解释空字符串(CITY_CODE_ALL_CITIES)为"加载所有城市的数据"
                        val cityCodeForViewModel =
                            cityCode ?: StreetListViewModel.CITY_CODE_ALL_CITIES // 空字符串("")
                        viewModel.processIntent(StreetListIntent.UpdateCity(cityCodeForViewModel))
                    }
            }
        }
        
        // 监听单一UI状态 - 符合MVI单一数据源
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { uiState ->
                    // 更新暗巷数据
                    streetAdapter.submitList(uiState.streets)

                    // 处理加载状态
                    when {
                        uiState.showFullScreenLoading -> binding.showLoadingView()
                        uiState.showFullScreenError -> {
                            binding.showErrorView(uiState.errorMessage) {
                                viewModel.processIntent(StreetListIntent.Retry)
                            }
                        }
                        uiState.showEmpty -> binding.showEmptyContent()
                        uiState.showContent -> binding.showRealContent()
                    }

                    // 处理下拉刷新状态
                    handleRefreshState(uiState)
                    
                    // 处理上拉加载状态
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
    
    private fun handleRefreshState(uiState: StreetListUiState) {
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

    private fun handleLoadMoreState(uiState: StreetListUiState) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        AppLogger.d(TAG, "${arguments?.getString(ARG_SORT)} is now visible")
    }

    private fun onFragmentInvisible() {
        // Fragment 变为不可见时的逻辑
        AppLogger.d(TAG, "${arguments?.getString(ARG_SORT)} is now invisible")
    }

    companion object {
        private const val ARG_SORT = "sort"
        private const val TAG = "StreetListFragment"
        
        fun newInstance(sort: String, index: Int): StreetListFragment {
            return StreetListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, sort)
                }
            }
        }
    }
}
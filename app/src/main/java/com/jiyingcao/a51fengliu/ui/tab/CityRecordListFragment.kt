package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.App
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
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.CityRecordListIntent
import com.jiyingcao.a51fengliu.viewmodel.CityRecordListViewModel
import com.jiyingcao.a51fengliu.viewmodel.CityRecordListViewModelFactory
import com.jiyingcao.a51fengliu.viewmodel.CitySelectionViewModel
import com.jiyingcao.a51fengliu.viewmodel.LoadingType
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class CityRecordListFragment : Fragment() {

    private var _binding: StatefulViewpager2RecyclerViewBinding? = null
    private val binding get() = _binding!!

    private val refreshLayout: SmartRefreshLayout get() = binding.refreshLayout
    private val recyclerView: RecyclerView get() = binding.recyclerView

    /** publish最新发布，weekly一周热门，monthly本月热门，lastMonth上月热门 */
    private lateinit var sort: String

    /**
     * The ViewModel for this fragment.
     *
     * 用于保存当前城市的Records列表数据。
     */
    private val viewModel: CityRecordListViewModel by viewModels {
        CityRecordListViewModelFactory(
            RecordRepository.getInstance(RetrofitClient.apiService),
            sort
        )
    }

    /**
     * Saving selected city, shared with other [CityRecordListFragment] instances.
     *
     * 保存选中的城市，与其他 [CityRecordListFragment] 实例共享。
     */
    private val citySelectionViewModel: CitySelectionViewModel by activityViewModels {
        CitySelectionViewModel.Factory(App.INSTANCE.dataStore)
    }

    private lateinit var recordAdapter: RecordAdapter

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
        setupFlowCollectors()
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
            setOnRefreshListener { viewModel.processIntent(CityRecordListIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(CityRecordListIntent.LoadMore) }
        }
    }

    private fun setupFlowCollectors() {
        // 监听选择的城市
        viewLifecycleOwner.lifecycleScope.launch {
            citySelectionViewModel.selectedCity.collect { cityCode ->
                AppLogger.d(TAG, "$TAG@${this@CityRecordListFragment.hashCode()}: city code selected: $cityCode")
                cityCode?.let {
                    viewModel.processIntent(CityRecordListIntent.UpdateCity(it))
                }
            }
        }
        
        // 监听单一UI状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                // 更新记录数据和滚动重置
                recordAdapter.submitList(uiState.records) {
                    // 如果需要重置滚动位置（切换过城市），则滚动到顶部
                    if (uiState.shouldResetScroll) {
                        recyclerView.scrollToPosition(0)
                        viewModel.processIntent(CityRecordListIntent.ScrollResetHandled)
                    }
                }
                
                // 处理加载状态
                when {
                    uiState.showFullScreenLoading -> binding.showLoadingView()
                    uiState.showFullScreenError -> {
                        binding.showErrorView(uiState.errorMessage) {
                            viewModel.processIntent(CityRecordListIntent.Retry)
                        }
                    }
                    uiState.showEmpty -> binding.showEmptyContent()
                    uiState.showContent -> {
                        binding.showContentView()
                        binding.showRealContent()
                    }
                }
                
                // 处理刷新状态
                if (uiState.isRefreshing) {
                    // 下拉刷新中 - SmartRefreshLayout 自动处理
                } else {
                    refreshLayout.finishRefresh(!uiState.isError || uiState.errorType != LoadingType.PULL_TO_REFRESH)
                }
                
                // 处理加载更多状态
                if (uiState.isLoadingMore) {
                    // 加载更多中 - SmartRefreshLayout 自动处理
                } else {
                    refreshLayout.finishLoadMore(!uiState.isError || uiState.errorType != LoadingType.LOAD_MORE)
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
        private const val TAG = "CityRecordListFragment"
        
        fun newInstance(sort: String): CityRecordListFragment {
            return CityRecordListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, sort)
                }
            }
        }
    }
}
package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.util.Log
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
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.CityIntent
import com.jiyingcao.a51fengliu.viewmodel.CityRecordsViewModel
import com.jiyingcao.a51fengliu.viewmodel.CityState
import com.jiyingcao.a51fengliu.viewmodel.CityViewModel
import com.jiyingcao.a51fengliu.viewmodel.CityViewModelFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class CityRecordsSubFragment : Fragment() {

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
    private val viewModel: CityViewModel by viewModels {
        CityViewModelFactory(
            RecordRepository.getInstance(RetrofitClient.apiService),
            sort
        )
    }

    /**
     * Saving selected city, shared with other [CityRecordsSubFragment] instances.
     *
     * 保存选中的城市，与其他 [CityRecordsSubFragment] 实例共享。
     */
    private val selectedCityViewModel: CityRecordsViewModel by activityViewModels {
        CityRecordsViewModel.Factory(App.INSTANCE.dataStore)
    }

    private lateinit var recordAdapter: RecordAdapter
    
    /** 标记是否需要重置列表滚动位置 */
    private var shouldResetScroll = false

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
            setOnRefreshListener { viewModel.processIntent(CityIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(CityIntent.LoadMore) }
        }
    }

    private fun setupFlowCollectors() {
        // 监听选择的城市
        viewLifecycleOwner.lifecycleScope.launch {
            selectedCityViewModel.selectedCity.collect { cityCode ->
                Log.d(TAG, "City code selected: $cityCode")
                cityCode?.let {
                    // StateFlow保证值不会重复发射，所以每次收到新的城市代码时都需要重置滚动
                    shouldResetScroll = true
                    viewModel.processIntent(CityIntent.UpdateCity(it))
                }
            }
        }
        
        // 监听ViewModel状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is CityState.Loading -> handleLoadingState(state)
                    is CityState.Error -> handleErrorState(state)
                    is CityState.Success -> {
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
                // 使用BRVAH的普通方法设置列表数据，无法使用回调
                recordAdapter.submitList(records)
                
                // 如果需要重置滚动位置（切换过城市），则滚动到顶部
                if (shouldResetScroll) {
                    recyclerView.scrollToPosition(0)
                    shouldResetScroll = false
                }
                
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
    
    private fun handleLoadingState(loading: CityState.Loading) {
        when (loading) {
            CityState.Loading.FullScreen -> binding.showLoadingView()
            CityState.Loading.PullToRefresh -> { /* 下拉刷新加载处理 */ }
            CityState.Loading.LoadMore -> { /* 加载更多处理 */ }
        }
    }
    
    private fun handleErrorState(error: CityState.Error) {
        when (error) {
            is CityState.Error.FullScreen -> {
                binding.showErrorView(error.message) {
                    viewModel.processIntent(CityIntent.Retry)
                }
            }
            is CityState.Error.PullToRefresh -> {
                refreshLayout.finishRefresh(false)
                requireContext().showToast(error.message)
            }
            is CityState.Error.LoadMore -> {
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
        private const val ARG_SORT = "sort"
        private const val TAG = "CityRecordsSubFragment"
        
        fun newInstance(sort: String): CityRecordsSubFragment {
            return CityRecordsSubFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, sort)
                }
            }
        }
    }
}
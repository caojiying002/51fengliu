package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.databinding.ActivityFavoriteBinding
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.FavoriteIntent
import com.jiyingcao.a51fengliu.viewmodel.FavoriteState
import com.jiyingcao.a51fengliu.viewmodel.FavoriteViewModel
import com.jiyingcao.a51fengliu.viewmodel.FavoriteViewModelFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

class FavoriteActivity : BaseActivity() {
    private lateinit var binding: ActivityFavoriteBinding

    private val statefulContent: StatefulRefreshRecyclerViewBinding
        get() = binding.statefulContent

    private val refreshLayout: SmartRefreshLayout get() = statefulContent.refreshLayout
    private val recyclerView: RecyclerView get() = statefulContent.recyclerView

    private lateinit var recordAdapter: RecordAdapter

    private val viewModel by viewModels<FavoriteViewModel> {
        FavoriteViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupTitleBar()
        setupRecyclerView()
        setupSmartRefreshLayout()
        setupFlowCollectors()

        viewModel.processIntent(FavoriteIntent.InitialLoad)
    }

    private fun setupTitleBar() {
        binding.titleBar.titleBarBack.text = getString(R.string.my_favorite)
        binding.titleBar.titleBarBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { _, _, position ->
                getItem(position)?.let {
                    DetailActivity.start(context, it.id)
                }
            }
        }
        
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = recordAdapter
        }
    }

    private fun setupSmartRefreshLayout() {
        refreshLayout.apply {
            setRefreshHeader(ClassicsHeader(context))
            setRefreshFooter(ClassicsFooter(context))
            setOnRefreshListener { viewModel.processIntent(FavoriteIntent.Refresh) }
            setOnLoadMoreListener { viewModel.processIntent(FavoriteIntent.LoadMore) }
        }
    }

    private fun setupFlowCollectors() {
        // 监听加载中、失败、成功状态
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is FavoriteState.Loading -> handleLoadingState(state)
                    is FavoriteState.Error -> handleErrorState(state)
                    is FavoriteState.Success -> {
                        statefulContent.showContentView()
                        refreshLayout.finishRefresh()
                        refreshLayout.finishLoadMore()
                    }
                    else -> {}
                }
            }
        }

        // 监听数据列表
        lifecycleScope.launch {
            viewModel.records.collect { records ->
                recordAdapter.submitList(records)
                
                // 如果没有数据，显示空状态
                if (records.isEmpty()) {
                    statefulContent.showEmptyContent()
                } else {
                    statefulContent.showRealContent()
                }
            }
        }

        // 监听是否已经是最后一页
        lifecycleScope.launch {
            viewModel.noMoreDataState.collect { noMoreData ->
                refreshLayout.setNoMoreData(noMoreData)
            }
        }
    }

    private fun handleLoadingState(loading: FavoriteState.Loading) {
        when (loading) {
            FavoriteState.Loading.FullScreen -> statefulContent.showLoadingView()
            FavoriteState.Loading.PullToRefresh -> { /* 下拉刷新加载处理 */ }
            FavoriteState.Loading.LoadMore -> { /* 加载更多处理 */ }
        }
    }

    private fun handleErrorState(error: FavoriteState.Error) {
        when (error) {
            is FavoriteState.Error.FullScreen -> {
                statefulContent.showErrorView(error.message) {
                    viewModel.processIntent(FavoriteIntent.Retry)
                }
            }
            is FavoriteState.Error.PullToRefresh -> {
                refreshLayout.finishRefresh(false)
                showToast(error.message)
            }
            is FavoriteState.Error.LoadMore -> {
                refreshLayout.finishLoadMore(false)
                showToast(error.message)
            }
        }
    }

    companion object {
        private const val TAG = "FavoriteActivity"

        @JvmStatic
        fun createIntent(context: Context) = Intent(context, FavoriteActivity::class.java)

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}
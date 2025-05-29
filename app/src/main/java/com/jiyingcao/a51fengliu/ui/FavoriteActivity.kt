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
import com.jiyingcao.a51fengliu.viewmodel.LoadingType
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
            setOnItemClickListener { record, position ->
                DetailActivity.start(this@FavoriteActivity, record.id)
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
                    is FavoriteState.Loading -> handleLoadingState(state.loadingType)
                    is FavoriteState.Error -> handleErrorState(state.message, state.errorType)
                    is FavoriteState.Success -> {
                        showContentView()
                        refreshLayout.finishRefresh()
                        refreshLayout.finishLoadMore()
                    }
                    else -> { showContentView() }
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

    private fun handleLoadingState(loadingType: LoadingType) {
        when (loadingType) {
            LoadingType.FULL_SCREEN -> showLoadingView()
            LoadingType.PULL_TO_REFRESH -> { /* 下拉刷新 */ }
            LoadingType.LOAD_MORE -> { /* 加载更多 */ }
            else -> showLoadingView() // 其他类型默认显示全屏加载
        }
    }

    private fun handleErrorState(message: String, errorType: LoadingType) {
        when (errorType) {
            LoadingType.FULL_SCREEN -> showErrorView(message)
            LoadingType.PULL_TO_REFRESH -> {
                refreshLayout.finishRefresh(false)
                showToast(message)
            }
            LoadingType.LOAD_MORE -> {
                refreshLayout.finishLoadMore(false)
                showToast(message)
            }
            else -> showErrorView(message)  // 其他类型默认显示全屏错误
        }
    }

    private fun showLoadingView() { statefulContent.showLoadingView() }
    private fun showContentView() { statefulContent.showContentView() }
    private fun showErrorView(message: String) {
        statefulContent.showErrorView(message) {
            viewModel.processIntent(FavoriteIntent.Retry)
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
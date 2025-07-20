package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.databinding.ActivityFavoriteBinding
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.FavoriteIntent
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
        observeUiState()

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

    /**
     * 单一状态流观察 - 企业级MVI最佳实践
     * 所有UI状态变化都在一个地方处理，便于维护和调试
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    // 处理数据展示, 最好优化一下避免每次都提交列表数据
                    recordAdapter.submitList(uiState.records)

                    // 处理各种UI状态 - 使用when表达式确保所有状态都被处理
                    when {
                        uiState.showFullScreenLoading -> {
                            showLoadingView()
                        }

                        uiState.showFullScreenError -> {
                            showErrorView(uiState.errorMessage)
                        }

                        uiState.showEmpty -> {
                            statefulContent.showEmptyContent()
                        }

                        uiState.showContent -> {
                            statefulContent.showRealContent()
                        }
                    }

                    // 处理刷新状态
                    if (!uiState.isRefreshing) {
                        refreshLayout.finishRefresh(!uiState.isError)
                    }

                    // 处理加载更多状态
                    if (!uiState.isLoadingMore) {
                        refreshLayout.finishLoadMore(!uiState.isError)
                    }

                    // 处理无更多数据状态
                    refreshLayout.setNoMoreData(uiState.noMoreData)

                    // 处理错误提示 - 只对非全屏错误显示Toast
                    if (uiState.isError && !uiState.showFullScreenError) {
                        showToast(uiState.errorMessage)
                    }
                }
            }
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
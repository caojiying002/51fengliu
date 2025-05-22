package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.databinding.FragmentMerchantBinding
import com.jiyingcao.a51fengliu.databinding.StatefulRefreshRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.MerchantAdapter
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class MerchantFragment : Fragment() {
    private var _binding: FragmentMerchantBinding? = null
    private val binding get() = _binding!!

    private val statefulContent: StatefulRefreshRecyclerViewBinding
        get() = binding.statefulContent

    private val refreshLayout: SmartRefreshLayout get() = statefulContent.refreshLayout
    private val recyclerView: RecyclerView get() = statefulContent.recyclerView

    private lateinit var merchantAdapter: MerchantAdapter

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
            //setOnRefreshListener { viewModel.processIntent(Refresh) }
            //setOnLoadMoreListener { viewModel.processIntent(LoadMore) }
        }
    }

    private fun setupFlowCollectors() {

    }

    override fun onResume() {
        super.onResume()
        // Fragment 变为可见时的逻辑
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
package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter4.BaseSingleItemAdapter
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.ItemData
import com.jiyingcao.a51fengliu.api.toFullUrl
import com.jiyingcao.a51fengliu.databinding.ActivityMainBinding
import com.jiyingcao.a51fengliu.databinding.DefaultLayoutStatefulRecyclerViewBinding
import com.jiyingcao.a51fengliu.databinding.ItemViewBinding
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.ui.adapter.ItemDataAdapter
import com.jiyingcao.a51fengliu.ui.adapter.RecordAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout
import com.jiyingcao.a51fengliu.ui.widget.StatefulLayout.State.*
import com.jiyingcao.a51fengliu.util.dp
import com.jiyingcao.a51fengliu.util.setEdgeToEdgePaddings
import com.jiyingcao.a51fengliu.viewmodel.MainViewModel2
import com.jiyingcao.a51fengliu.viewmodel.UiState
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class MainActivity : BaseActivity() {
    private lateinit var binding: DefaultLayoutStatefulRecyclerViewBinding
    private lateinit var statefulLayout: StatefulLayout
    private lateinit var refreshLayout: SmartRefreshLayout
    private lateinit var recyclerView: RecyclerView

    private lateinit var viewModel: MainViewModel2

    @Deprecated("Use recordAdapter instead")
    private lateinit var itemDataAdapter: ItemDataAdapter
    private lateinit var recordAdapter: RecordAdapter

    /** 是否有数据已经加载 */
    private var hasDataLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = DefaultLayoutStatefulRecyclerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setEdgeToEdgePaddings(binding.root)

        statefulLayout = binding.statefulLayout // 简化代码调用
        refreshLayout = findViewById(R.id.refreshLayout)
        recyclerView = findViewById(R.id.recyclerView)

        val fixedAreaAdapter = object : BaseSingleItemAdapter<Any, RecyclerView.ViewHolder>() {
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Any?) {}

            override fun onCreateViewHolder(
                context: Context,
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(context)
                    .inflate(R.layout.main_fixed_area, parent, false)
                return object : RecyclerView.ViewHolder(itemView) {}
            }

        }
        recordAdapter = RecordAdapter().apply {
            setOnItemClickListener { _, _, position ->
                Log.d(TAG, "Record $position clicked")
                recordAdapter.getItem(position)?.let {
                    DetailActivity.start(context, it)
                }
            }
        }

        recyclerView.apply {
            // 使用线性布局管理器
            layoutManager = LinearLayoutManager(context)
            // 指定适配器
            adapter = ConcatAdapter(fixedAreaAdapter, recordAdapter)
        }

        refreshLayout.setOnRefreshListener { viewModel.fetchByPage(false) }
        viewModel = ViewModelProvider(this)[MainViewModel2::class.java]
        viewModel.data.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // 显示加载动画
                    if (!hasDataLoaded)
                        statefulLayout.currentState = LOADING
                }
                is UiState.Success -> {
                    hasDataLoaded = true
                    refreshLayout.finishRefresh()
                    // 显示数据
                    statefulLayout.currentState = CONTENT
                    recordAdapter.submitList(state.data.records)
                    // TODO 如果列表为空需要显示空状态
                }
                is UiState.Empty -> {
                    // 不再使用
                    //refreshLayout.finishRefresh()
                }
                is UiState.Error -> {
                    refreshLayout.finishRefresh()
                    // 显示错误信息
                    if (!hasDataLoaded)
                        statefulLayout.currentState = ERROR
                    else
                        Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.fetchByPage(true)

        findViewById<View>(R.id.title_bar_menu)?.setOnClickListener { CityActivity.start(this) }
        findViewById<View>(R.id.title_bar_profile)?.setOnClickListener { SearchActivity.start(this) }
    }

    companion object {
        private const val TAG: String = "MainActivity"
    }
}

@Deprecated("Use ItemDataAdapter instead")
class MyAdapter(private val dataset: List<ItemData>) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    class MyViewHolder(val binding: ItemViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding: ItemViewBinding = ItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val context = holder.itemView.context
        val itemData = dataset[position]
        holder.itemView.setOnClickListener {
            Log.d("MyAdapter", "Item $position clicked")
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("ITEM_DATA", itemData)
            }
            context.startActivity(intent)
        }
        val binding = holder.binding
        binding.itemTitle.text = itemData.title
        binding.itemProcess.text = itemData.process
        binding.itemDz.text = itemData.dz
        binding.itemCreateTime.text = itemData.create_time
        binding.itemBrowse.text = itemData.browse
        // 加载图片，这里只是示例，实际可能需要用到像Glide这样的库来加载网络图片
        GlideApp.with(binding.itemImage.context)
            .load(itemData.img.toFullUrl())
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.image_broken)
            .transform(CenterCrop(), RoundedCorners(4.dp))
            //.transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.itemImage)
    }

    override fun getItemCount() = dataset.size
}


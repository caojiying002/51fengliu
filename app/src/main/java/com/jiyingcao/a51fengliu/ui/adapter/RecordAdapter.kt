package com.jiyingcao.a51fengliu.ui.adapter

import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.placeholder
import coil3.request.error
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.databinding.ItemRecordBinding
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.util.dp

class RecordAdapter : ListAdapter<RecordInfo, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    private var onItemClickListener: ((record: RecordInfo, position: Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (record: RecordInfo, position: Int) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding, this)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class RecordViewHolder(
        val binding: ItemRecordBinding, 
        private val adapter: RecordAdapter
    ) : RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = adapter.getItem(position)
                    adapter.onItemClickListener?.invoke(item, position)
                }
            }
        }
        
        fun bind(item: RecordInfo) {
            binding.apply {
                itemTitle.text = item.title
                itemProcess.text = item.desc
                itemDz.text = item.cityCode.to2LevelName() // 城市代码转换为城市名称
                itemCreateTime.text = timestampToDay(item.publishedAt)
                itemBrowse.text = item.viewCount

                itemImage.let { imageView ->
                    if (item.coverPicture.isNullOrBlank()) {
                        imageView.visibility = GONE
                    } else {
                        imageView.visibility = VISIBLE
                        
                        // 使用Coil3替代Glide加载图片，自动使用全局设置的ImageLoader
                        val fullUrl = AppConfig.Network.BASE_IMAGE_URL + item.coverPicture
                        imageView.load(fullUrl) {
                            placeholder(R.drawable.placeholder)
                            error(R.drawable.image_broken)
                            transformations(RoundedCornersTransformation(4.dp.toFloat()))
                        }
                    }
                }
            }
        }
    }

    class RecordDiffCallback : DiffUtil.ItemCallback<RecordInfo>() {
        override fun areItemsTheSame(oldItem: RecordInfo, newItem: RecordInfo): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: RecordInfo, newItem: RecordInfo): Boolean {
            return oldItem == newItem
        }
    }
}
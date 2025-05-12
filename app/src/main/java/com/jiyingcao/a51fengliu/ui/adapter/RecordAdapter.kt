package com.jiyingcao.a51fengliu.ui.adapter

import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.databinding.ItemRecordBinding
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.util.ImageLoader

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

                itemImage.let {
                    if (item.coverPicture.isNullOrBlank()) {
                        it.visibility = GONE
                    } else {
                        it.visibility = VISIBLE
                        ImageLoader.load(
                            imageView = it,
                            url = item.coverPicture,
                            cornerRadius = 4
                        )
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
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
import com.jiyingcao.a51fengliu.api.response.Street
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.databinding.ItemStreetBinding
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.util.dp

class StreetAdapter : ListAdapter<Street, StreetAdapter.StreetViewHolder>(StreetDiffCallback()) {

    private var onItemClickListener: ((street: Street, position: Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (street: Street, position: Int) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreetViewHolder {
        val binding = ItemStreetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StreetViewHolder(binding, this)
    }

    override fun onBindViewHolder(holder: StreetViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class StreetViewHolder(
        val binding: ItemStreetBinding, 
        private val adapter: StreetAdapter
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
        
        fun bind(item: Street) {
            binding.apply {
                itemTitle.text = item.title
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
                            placeholder(R.drawable.layer_placeholder)
                            error(R.drawable.picture_loading_failed)
                            transformations(RoundedCornersTransformation(4.dp.toFloat()))
                        }
                    }
                }
            }
        }
    }

    class StreetDiffCallback : DiffUtil.ItemCallback<Street>() {
        override fun areItemsTheSame(oldItem: Street, newItem: Street): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Street, newItem: Street): Boolean {
            return oldItem == newItem
        }
    }
}
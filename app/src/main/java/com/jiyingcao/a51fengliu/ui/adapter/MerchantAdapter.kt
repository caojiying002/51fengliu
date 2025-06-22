package com.jiyingcao.a51fengliu.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.placeholder
import coil3.request.error
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.databinding.ItemMerchantBinding
import com.jiyingcao.a51fengliu.util.dp
import com.jiyingcao.a51fengliu.util.to2LevelName

class MerchantAdapter : ListAdapter<Merchant, MerchantAdapter.MerchantViewHolder>(MerchantDiffCallback()) {

    private var onItemClickListener: ((merchant: Merchant, position: Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (merchant: Merchant, position: Int) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MerchantViewHolder {
        val binding = ItemMerchantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MerchantViewHolder(binding, this)
    }

    override fun onBindViewHolder(holder: MerchantViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class MerchantViewHolder(
        val binding: ItemMerchantBinding,
        private val adapter: MerchantAdapter
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

        fun bind(item: Merchant) {
            binding.apply {
                name.text = item.name
                intro.text = item.intro
                province.text = item.cityCode.to2LevelName() // TODO 只显示省份就可以

                coverPicture.let { imageView ->
                    if (item.coverPicture.isNullOrBlank()) {
                        imageView.isVisible = false
                    } else {
                        imageView.isVisible = true

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

    class MerchantDiffCallback : DiffUtil.ItemCallback<Merchant>() {
        override fun areItemsTheSame(oldItem: Merchant, newItem: Merchant): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Merchant, newItem: Merchant): Boolean {
            return oldItem == newItem
        }
    }
}
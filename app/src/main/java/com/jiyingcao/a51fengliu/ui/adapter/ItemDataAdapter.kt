package com.jiyingcao.a51fengliu.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter4.BaseQuickAdapter
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.ItemData
import com.jiyingcao.a51fengliu.api.toFullUrl
import com.jiyingcao.a51fengliu.databinding.ItemViewBinding
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.util.dp

class ItemDataAdapter : BaseQuickAdapter<ItemData, ItemDataAdapter.ItemDataViewHolder>() {
    class ItemDataViewHolder(val binding: ItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    override fun onBindViewHolder(holder: ItemDataViewHolder, position: Int, item: ItemData?) {
        requireNotNull(item) { "ItemData is null" }

        holder.binding.apply {
            itemTitle.text = item.title
            itemProcess.text = item.process
            itemDz.text = item.dz
            itemCreateTime.text = item.create_time
            itemBrowse.text = item.browse

            itemImage.let {
                if (item.img.isBlank()) {
                    it.visibility = GONE
                } else {
                    it.visibility = VISIBLE
                    GlideApp.with(it.context)
                        .load(item.img.toFullUrl())
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.image_broken)
                        .transform(CenterCrop(), RoundedCorners(4.dp))
                        //.transition(DrawableTransitionOptions.withCrossFade())
                        .into(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): ItemDataViewHolder {
        val binding: ItemViewBinding =
            ItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemDataViewHolder(binding)
    }
}
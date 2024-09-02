package com.jiyingcao.a51fengliu.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter4.BaseQuickAdapter
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.Record
import com.jiyingcao.a51fengliu.databinding.ItemViewBinding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.util.dp
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName

class RecordAdapter : BaseQuickAdapter<Record, RecordAdapter.RecordViewHolder>() {
    class RecordViewHolder(val binding: ItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int, item: Record?) {
        requireNotNull(item) { "ItemData is null" }

        holder.binding.apply {
            itemTitle.text = item.title
            itemProcess.text = item.desc
            itemDz.text = item.cityCode.to2LevelName() // TODO 城市代码转换为城市名称
            itemCreateTime.text = timestampToDay(item.publishedAt)
            itemBrowse.text = item.viewCount

            itemImage.let {
                if (item.coverPicture.isNullOrBlank()) {
                    it.visibility = GONE
                } else {
                    it.visibility = VISIBLE
                    GlideApp.with(it.context)
                        .load(BASE_IMAGE_URL + item.coverPicture)   // TODO 简化拼接URL过程
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
    ): RecordViewHolder {
        val binding: ItemViewBinding =
            ItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }
}
package com.jiyingcao.a51fengliu.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.jiyingcao.a51fengliu.util.City

class CityAdapter : BaseQuickAdapter<City, CityAdapter.CityViewHolder>() {
    class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text1: TextView = itemView.findViewById(android.R.id.text1)
        //val text2: TextView = itemView.findViewById(android.R.id.text2)
    }

    override fun onBindViewHolder(
        holder: CityViewHolder,
        position: Int,
        item: City?
    ) {
        requireNotNull(item) { "item: City is null" }

        holder.text1.text = item.name
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): CityViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return CityViewHolder(itemView)
    }
}
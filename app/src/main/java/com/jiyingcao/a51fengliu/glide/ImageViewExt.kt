package com.jiyingcao.a51fengliu.glide

import android.widget.ImageView
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.jiyingcao.a51fengliu.util.dp

fun ImageView.load(url: String) {
    GlideApp.with(this)
        .load(url)
        .transform(CenterCrop(), RoundedCorners(4.dp))
        .into(this)
}
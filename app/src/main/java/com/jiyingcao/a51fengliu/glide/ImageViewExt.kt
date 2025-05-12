package com.jiyingcao.a51fengliu.glide

import android.widget.ImageView
import com.jiyingcao.a51fengliu.util.ImageLoader
import com.jiyingcao.a51fengliu.util.dp

fun ImageView.load(url: String) {
    ImageLoader.load(this, url, cornerRadius = 4)
}
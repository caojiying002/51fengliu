package com.jiyingcao.a51fengliu.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun timestampToDate(timestamp: Long, format: String = "yyyy-MM-dd HH:mm:ss"): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(date)
}

fun timestampToDate(timestamp: String?, format: String = "yyyy-MM-dd HH:mm:ss"): String {
    return timestamp?.toLongOrNull()?.let {
        timestampToDate(it, format)
    } ?: timestamp ?: ""
}

fun timestampToDay(timestamp: Long, format: String = "yyyy-MM-dd"): String {
    return timestampToDate(timestamp, format)
}

fun timestampToDay(timestamp: String?, format: String = "yyyy-MM-dd"): String {
    return timestampToDate(timestamp, format)
}
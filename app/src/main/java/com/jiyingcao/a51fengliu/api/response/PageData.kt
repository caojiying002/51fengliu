package com.jiyingcao.a51fengliu.api.response

data class PageData<T>(
    val records: List<T>,
    val total: Int,
    val size: Int,
    val current: Int,
    val pages: Int,
) {
    fun isFirstPage() = (current == 1)

    fun noMoreData() = (current >= pages)
}

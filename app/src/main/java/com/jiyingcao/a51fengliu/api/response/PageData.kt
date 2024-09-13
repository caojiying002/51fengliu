package com.jiyingcao.a51fengliu.api.response

data class PageData(
    val records: List<RecordInfo>,
    val total: Int,
    val size: Int,
    val current: Int,
    val orders: List<Any?>,
    val optimizeCountSql: Boolean,
    val searchCount: Boolean,
    val countId: Any?,
    val maxLimit: Any?,
    val pages: Int,
) {
    val hasNextPage: Boolean
        get() = (current < pages)
}

package com.jiyingcao.a51fengliu.viewmodel

import com.jiyingcao.a51fengliu.api.response.ItemData

enum class LoadingType {
    FULL_SCREEN,
    PULL_REFRESH,
    LOAD_MORE,
    NONE,   // 无加载效果，不应使用
}

/**
 * A wrapper class for wrapping paged data.
 * 一个包装类，用于包装分页数据
 *
 * @param data 数据列表
 * @param page 当前页码
 */
@Deprecated("") class ItemDataWithLoadingType (
    val data: List<ItemData> = emptyList(),
    val page: Int = 1,
    val loadingType: LoadingType = LoadingType.NONE,
)
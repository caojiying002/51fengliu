package com.jiyingcao.a51fengliu.viewmodel

/**
 * 加载状态类型枚举
 * 用于标识不同的加载场景，便于统一管理UI状态
 */
enum class LoadingType {
    FULL_SCREEN,     // 全屏加载（初次进入页面）
    PULL_TO_REFRESH, // 下拉刷新
    PAGINATION,      // 底部分页加载更多
    OVERLAY,         // 浮层加载
    NONE             // 无加载状态（默认值）
}
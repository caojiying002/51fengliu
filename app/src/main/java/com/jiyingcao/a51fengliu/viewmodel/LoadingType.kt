package com.jiyingcao.a51fengliu.viewmodel

/**
 * 通用加载类型枚举
 * 统一管理所有ViewModel的加载场景
 */
enum class LoadingType {
    FULL_SCREEN,     // 全屏加载
    PULL_TO_REFRESH, // 下拉刷新
    LOAD_MORE,       // 加载更多
    //FLOAT,           // 浮层加载（用于登录后刷新等场景）
    OVERLAY          // 覆盖层加载
}
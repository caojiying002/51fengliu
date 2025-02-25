package com.jiyingcao.a51fengliu.viewmodel

// TODO 改用object
fun UiState0.Loading.fullScreen() = UiState0.Loading(UiState0.LoadingType.FULL_SCREEN)
fun UiState0.Loading.pullRefresh() = UiState0.Loading(UiState0.LoadingType.PULL_REFRESH)

sealed class UiState0<out T> {
    data class Loading(val type: LoadingType) : UiState0<Nothing>() {
        companion object {
            @JvmStatic fun fullScreen() = Loading(LoadingType.FULL_SCREEN)
            @JvmStatic fun pullRefresh() = Loading(LoadingType.PULL_REFRESH)
        }
    }
    data class Success<T>(val data: T) : UiState0<T>()
    @Deprecated("不再专门使用Empty状态，例如列表页下拉加载更多，UI层不需要显示整页的空状态")
    data object Empty : UiState0<Nothing>()
    data class Error(val message: String) : UiState0<Nothing>()

    /**
     * 加载动画类型
     * [FULL_SCREEN] 全屏加载动画
     * [PULL_REFRESH] 下拉刷新
     */
    @Deprecated("") enum class LoadingType {
        FULL_SCREEN,
        PULL_REFRESH
    }
}


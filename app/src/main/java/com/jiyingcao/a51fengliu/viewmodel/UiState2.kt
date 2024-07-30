package com.jiyingcao.a51fengliu.viewmodel

import com.jiyingcao.a51fengliu.viewmodel.LoadingType.*

sealed class UiState2(
    open val loadingType: LoadingType
) {
    class Loading(loadingType: LoadingType = NONE) : UiState2(loadingType)
    class Success<T>(val data: T, loadingType: LoadingType = NONE) : UiState2(loadingType)
    class Error(/*val message: String = "加载失败",*/ loadingType: LoadingType = NONE) : UiState2(loadingType)
}


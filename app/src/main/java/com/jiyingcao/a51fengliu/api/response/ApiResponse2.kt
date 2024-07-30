package com.jiyingcao.a51fengliu.api.response

data class ApiResponse2<T>(
    val code: Int,
    val msg: String?,
    val data: T?
) {
    val isSuccess: Boolean
        get() = (code == 0)
}
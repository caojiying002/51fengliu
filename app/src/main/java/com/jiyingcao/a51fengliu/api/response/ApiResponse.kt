package com.jiyingcao.a51fengliu.api.response

data class ApiResponse<T>(
    val code: Int,
    val msg: String?,
    val data: T?
) {
    fun isSuccessful() = (code == 0)
}
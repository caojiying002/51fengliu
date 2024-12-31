package com.jiyingcao.a51fengliu.api.response

import com.jiyingcao.a51fengliu.domain.exception.BusinessException

data class ApiResponse<T>(
    val code: Int,
    val msg: String?,
    val data: T?
) {
    fun isSuccessful() = (code == 0)
}

/** API响应的扩展函数，用于快速检查响应状态并抛出异常 */
fun <T> ApiResponse<T>.throwIfNotZero() {
    if (code != 0) {
        throw BusinessException.createFromResponse(this)
    }
}
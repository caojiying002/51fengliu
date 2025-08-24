package com.jiyingcao.a51fengliu.api.response

import com.jiyingcao.a51fengliu.domain.exception.ApiException

/**
 * 通用API响应基类
 * 用于大多数正常的API接口
 */
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
        throw ApiException.createFromResponse(this)
    }
}

/** 如果某些接口不需要错误数据类型，可以使用这个对象来代替 */
object NoErrorData
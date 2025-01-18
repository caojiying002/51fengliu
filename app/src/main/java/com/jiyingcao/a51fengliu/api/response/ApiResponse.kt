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

/** 如果某些接口不需要错误数据类型，可以使用这个对象来代替 */
object NoErrorData

/**
 * 定义一个通用的密封类来表示所有API响应的结果，用[Success]或者[Error]来包装ApiResponse.data
 */
sealed class ApiResult<out T, out E> {
    data class Success<T>(val data: T) : ApiResult<T, Nothing>()
    data class Error<E>(
        val code: Int,
        val msg: String?,
        val errorData: E? = null
    ) : ApiResult<Nothing, E>()
}
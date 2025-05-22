package com.jiyingcao.a51fengliu.api.response

import com.jiyingcao.a51fengliu.domain.exception.ApiException

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

/**
 * 只用于[ApiResponse.data]在成功/失败时类型不同的场景，用[Success]或者[Error]来包装ApiResponse.data。
 * 在大部分场景下，[ApiResponse.data]的类型是固定的，所以直接使用ApiResponse<T>就可以了。
 *
 * 例如：登录接口成功时返回用户token，失败时返回错误信息。
 * 这种场景下，[ApiResponse.data]的类型是String和[LoginErrorData]，
 * 应当使用ApiResponse<ApiResult<String, LoginErrorData>>。
 */
sealed class ApiResult<out T, out E> {
    data class Success<T>(val data: T) : ApiResult<T, Nothing>()
    data class Error<E>(
        val code: Int,
        val msg: String?,
        val errorData: E? = null
    ) : ApiResult<Nothing, E>()
}
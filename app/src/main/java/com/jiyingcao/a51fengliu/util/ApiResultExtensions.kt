package com.jiyingcao.a51fengliu.util

import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.domain.model.ApiResult

/**
 * ApiResult扩展函数集合
 */

/**
 * 将ApiResult映射为另一种类型
 *
 * @param transform 成功时的转换函数
 * @return 转换后的ApiResult
 */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> {
    return when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.ApiError -> this
        is ApiResult.NetworkError -> this
        is ApiResult.UnknownError -> this
    }
}

/**
 * 成功时执行操作
 *
 * @param action 成功时执行的操作
 * @return 原始ApiResult（支持链式调用）
 */
inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) {
        action(data)
    }
    return this
}

/**
 * API业务错误时执行操作
 *
 * @param action 错误时执行的操作，参数为(code, message, data)
 * @return 原始ApiResult（支持链式调用）
 */
inline fun <T> ApiResult<T>.onApiError(action: (code: Int, message: String, data: Any?) -> Unit): ApiResult<T> {
    if (this is ApiResult.ApiError) {
        action(code, message, data)
    }
    return this
}

/**
 * 网络错误时执行操作
 *
 * @param action 错误时执行的操作，参数为异常对象
 * @return 原始ApiResult（支持链式调用）
 */
inline fun <T> ApiResult<T>.onNetworkError(action: (Throwable) -> Unit): ApiResult<T> {
    if (this is ApiResult.NetworkError) {
        action(exception)
    }
    return this
}

/**
 * 未知错误时执行操作
 *
 * @param action 错误时执行的操作，参数为异常对象
 * @return 原始ApiResult（支持链式调用）
 */
inline fun <T> ApiResult<T>.onUnknownError(action: (Throwable) -> Unit): ApiResult<T> {
    if (this is ApiResult.UnknownError) {
        action(exception)
    }
    return this
}

/**
 * 任意失败时执行操作（包括ApiError、NetworkError、UnknownError）
 *
 * @param action 失败时执行的操作
 * @return 原始ApiResult（支持链式调用）
 */
inline fun <T> ApiResult<T>.onFailure(action: () -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) {
        // 不执行
    } else {
        action()
    }
    return this
}

/**
 * 获取用户友好的错误消息
 *
 * @return 错误消息字符串，成功时返回null
 */
fun <T> ApiResult<T>.getErrorMessageOrNull(): String? {
    return when (this) {
        is ApiResult.Success -> null
        is ApiResult.ApiError -> message
        is ApiResult.NetworkError -> exception.toUserFriendlyMessage()
        is ApiResult.UnknownError -> exception.toUserFriendlyMessage()
    }
}

/**
 * 获取用户友好的错误消息
 *
 * @param defaultMessage 成功时的默认消息
 * @return 错误消息字符串
 */
fun <T> ApiResult<T>.getErrorMessage(defaultMessage: String = ""): String {
    return getErrorMessageOrNull() ?: defaultMessage
}

/**
 * 将ApiResult转换为Kotlin标准库的Result
 *
 * @return Result<T>
 */
fun <T> ApiResult<T>.toResult(): Result<T> {
    return when (this) {
        is ApiResult.Success -> Result.success(data)
        is ApiResult.ApiError -> Result.failure(com.jiyingcao.a51fengliu.domain.exception.ApiException(code, message))
        is ApiResult.NetworkError -> Result.failure(exception)
        is ApiResult.UnknownError -> Result.failure(exception)
    }
}

/**
 * 将Kotlin标准库的Result转换为ApiResult
 * 注意：Result.failure无法区分ApiError和NetworkError，统一转为UnknownError
 *
 * @return ApiResult<T>
 */
fun <T> Result<T>.toApiResult(): ApiResult<T> {
    return when {
        isSuccess -> ApiResult.Success(getOrThrow())
        else -> {
            val exception = exceptionOrNull()
            when (exception) {
                is com.jiyingcao.a51fengliu.domain.exception.ApiException ->
                    ApiResult.ApiError(exception.code, exception.message ?: "Unknown error")
                is java.io.IOException ->
                    ApiResult.NetworkError(exception)
                else ->
                    ApiResult.UnknownError(exception ?: Exception("Unknown error"))
            }
        }
    }
}

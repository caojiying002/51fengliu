package com.jiyingcao.a51fengliu.domain.model

/**
 * 统一API调用结果封装
 *
 * 用于Repository层向ViewModel层传递API调用结果，包含所有可能的状态：
 * - [Success]: API调用成功且业务成功
 * - [ApiError]: API调用成功但业务失败（服务端返回错误码）
 * - [NetworkError]: 网络层面失败（IO异常、JSON解析异常等）
 * - [UnknownError]: 其他未预期的异常
 *
 * ## 使用示例
 * ```kotlin
 * // Repository层
 * suspend fun getProfile(): Flow<ApiResult<Profile>> = flow {
 *     emit(ApiResult.Success(profile))
 * }
 *
 * // ViewModel层
 * repository.getProfile().collect { result ->
 *     when (result) {
 *         is ApiResult.Success -> updateUiState(result.data)
 *         is ApiResult.ApiError -> showError(result.message)
 *         is ApiResult.NetworkError -> showNetworkError()
 *         is ApiResult.UnknownError -> showUnknownError(result.throwable)
 *     }
 * }
 * ```
 */
sealed class ApiResult<out T> {
    /**
     * API调用成功且业务成功
     * @param data 业务数据
     */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * API调用成功但业务失败
     * @param code 业务错误码（如 1003、2001 等）
     * @param message 错误消息
     * @param data 部分失败时可能返回的数据（如登录失败时的验证码图片）
     */
    data class ApiError(
        val code: Int,
        val message: String,
        val data: Any? = null
    ) : ApiResult<Nothing>()

    /**
     * 网络层面失败
     * 包括但不限于：
     * - IOException: 网络连接失败、超时等
     * - JsonDataException: JSON解析失败
     * - JsonEncodingException: JSON编码错误
     * - HttpException: HTTP错误（非2xx状态码）
     *
     * @param exception 原始异常对象
     */
    data class NetworkError(val exception: Throwable) : ApiResult<Nothing>()

    /**
     * 其他未预期的异常
     * @param exception 原始异常对象
     */
    data class UnknownError(val exception: Throwable) : ApiResult<Nothing>()

    /**
     * 判断是否为成功状态
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * 判断是否为失败状态
     */
    fun isFailure(): Boolean = !isSuccess()

    /**
     * 如果是成功状态，返回数据；否则返回null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * 如果是成功状态，返回数据；否则抛出异常
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is ApiError -> throw com.jiyingcao.a51fengliu.domain.exception.ApiException(code, message)
        is NetworkError -> throw exception
        is UnknownError -> throw exception
    }
}

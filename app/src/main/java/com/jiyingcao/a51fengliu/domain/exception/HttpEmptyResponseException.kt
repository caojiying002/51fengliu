package com.jiyingcao.a51fengliu.domain.exception

/**
 * TODO
 * 可以考虑将异常名称改为更准确的描述，如：
 *
 * UnexpectedEmptyResponseException
 * MissingApiResponseException
 * 这样更清楚地表达了"期望有 ApiResponse 结构但实际为空"的语义，而不是简单的"HTTP 空响应体异常"。
 */

/**
 * 响应体为空异常
 * 用于处理HTTP请求成功但响应体为null的情况
 */
class HttpEmptyResponseException(
    message: String = "Response body is null",
    cause: Throwable? = null
) : IllegalStateException(message, cause) { // TODO 父类用IllegalStateException是否合适？

    override fun toString(): String {
        return "HttpEmptyResponseException(message='$message')"
    }
}
package com.jiyingcao.a51fengliu.domain.exception

/**
 * 响应体为空异常
 * 用于处理HTTP请求成功但响应体为null的情况
 */
class HttpEmptyResponseException(
    message: String = "Response body is null",
    cause: Throwable? = null
) : IllegalStateException(message, cause) {

    override fun toString(): String {
        return "HttpEmptyResponseException(message='$message')"
    }
}
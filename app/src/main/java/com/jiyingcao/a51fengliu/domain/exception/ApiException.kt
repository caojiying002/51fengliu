package com.jiyingcao.a51fengliu.domain.exception

/**
 * 基础API异常类，包含基本的错误码和消息
 */
open class ApiException(
    val code: Int,
    message: String?,
    cause: Throwable? = null
) : Exception(message, cause)
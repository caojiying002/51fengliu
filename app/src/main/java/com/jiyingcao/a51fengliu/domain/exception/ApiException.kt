package com.jiyingcao.a51fengliu.domain.exception

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.IOException

/**
 * 基础API异常类，包含基本的错误码和消息
 */
open class ApiException(
    val code: Int,
    message: String?,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * 将异常转换为用户友好的消息
 */
fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is ApiException -> "API错误码${code}：${messageTaggedByType()}"
        is HttpException -> "${messageTaggedByType()}\r\n" + this.response()?.errorBody()?.string()
        is MalformedJsonException, is JsonParseException -> "数据解析错误：${messageTaggedByType()}"
        is IOException -> "网络错误：${messageTaggedByType()}"
        else -> "未知错误：${messageTaggedByType()}"
    }
}

/**
 * 将异常的简单类名与消息拼接为字符串
 */
fun Throwable.messageTaggedByType(): String {
    return "[${this::class.simpleName}] $message"
}
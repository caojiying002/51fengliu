package com.jiyingcao.a51fengliu.domain.exception

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.jiyingcao.a51fengliu.api.response.ApiResponse
import retrofit2.HttpException
import java.io.IOException

/**
 * 基础API异常类，包含基本的错误码和消息
 */
open class ApiException(
    val code: Int,
    message: String?,
    cause: Throwable? = null
) : Exception(message, cause) {

    override fun toString(): String {
        return "ApiException(code=$code, message=$message)"
    }

    companion object {
        const val CODE_REMOTE_LOGIN = 1003

        /** 2001: 无效的记录Record ID，可能已被删除 */
        const val CODE_INVALID_RECORD_ID = 2001

        /** 创建异常实例的便捷方法 */
        @JvmStatic
        fun createFromResponse(response: ApiResponse<*>): ApiException {
            if (response.code == CODE_REMOTE_LOGIN)
                return RemoteLoginException(response.code, response.msg)

            return ApiException(
                code = response.code,
                message = response.msg ?: "Unknown API error"
            )
        }
    }
}

/**
 * 将异常转换为用户友好的消息
 */
fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is ApiException -> toUserFriendlyMessage()
        is HttpException -> toUserFriendlyMessage()
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

fun HttpException.toUserFriendlyMessage(): String {
    return when (code()) {
        429 -> "请求过于频繁，请稍后再试"
        else -> "${messageTaggedByType()}\r\n" + this.response()?.errorBody()?.string()
    }
}

/**
 * 将[ApiException]转换为用户友好的消息
 *
 * 示例：“[-2] 已经举报过此信息”
 */
fun ApiException.toUserFriendlyMessage(): String {
    return when (code) {
        ApiException.CODE_REMOTE_LOGIN -> "您的账号已在其他设备登录"
        ApiException.CODE_INVALID_RECORD_ID -> "您查找的信息不存在或已删除"
        else -> if (!message.isNullOrBlank()) "[$code] $message" else "API错误码$code：${messageTaggedByType()}"
    }
}
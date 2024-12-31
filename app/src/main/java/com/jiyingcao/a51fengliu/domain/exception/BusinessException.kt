package com.jiyingcao.a51fengliu.domain.exception

import com.jiyingcao.a51fengliu.api.response.ApiResponse

/**
 * 继承自[ApiException]的具体业务异常类，包含了常见错误码的定义
 */
class BusinessException(
    code: Int,
    message: String?,
    cause: Throwable? = null
) : ApiException(code, message, cause) {

    companion object {
        const val CODE_UNKNOWN = -1
        const val CODE_NETWORK = -2
        const val CODE_DATA = -3
        const val CODE_SERVER = -4
        const val CODE_AUTH = -5
        const val CODE_PERMISSION = -6
        const val CODE_NOT_FOUND = -7
        const val CODE_TIMEOUT = -8
        const val CODE_INVALID = -9
        const val CODE_DUPLICATE = -10
        const val CODE_FORBIDDEN = -11
        const val CODE_UNAVAILABLE = -12
        const val CODE_EXPIRED = -13
        const val CODE_LIMIT = -14
        const val CODE_EMPTY = -15
        const val CODE_INVALID_PARAM = -16

        /** 创建异常实例的便捷方法 */
        fun createFromResponse(response: ApiResponse<*>): BusinessException {
            return BusinessException(
                code = response.code,
                message = response.msg ?: "Unknown business error"
            )
        }
    }
}
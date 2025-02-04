package com.jiyingcao.a51fengliu.domain.exception

/**
 * 远程登录异常，用于标识用户在其他设备上登录
 */
class RemoteLoginException(
    code: Int = CODE_REMOTE_LOGIN,
    message: String? = "Remote login detected",
    cause: Throwable? = null
) : ApiException(code, message, cause) {

    override fun toString(): String {
        return "RemoteLoginException(code=$code, message=$message)"
    }
}
package com.jiyingcao.a51fengliu.domain.exception

import com.jiyingcao.a51fengliu.api.response.LoginErrorData

class LoginException(
    code: Int,
    message: String?,
    cause: Throwable? = null,
    val errorData: LoginErrorData?
) : ApiException(code, message, cause) {

    override fun toString(): String {
        return "LoginException(code=$code, message=$message, errorData=$errorData)"
    }
}
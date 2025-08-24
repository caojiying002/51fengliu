package com.jiyingcao.a51fengliu.domain.exception

class LoginException(
    code: Int,
    message: String?,
    cause: Throwable? = null,
    val errors: Map<String, String>
) : ApiException(code, message, cause) {

    override fun toString(): String {
        return "LoginException(code=$code, message=$message, errors=$errors)"
    }
}
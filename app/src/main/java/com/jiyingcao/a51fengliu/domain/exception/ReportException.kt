package com.jiyingcao.a51fengliu.domain.exception

class ReportException(
    code: Int,
    message: String?,
    cause: Throwable? = null,
    val errors: Map<String, String>
) : ApiException(code, message, cause) {

    override fun toString(): String {
        return "ReportException(code=$code, message=$message, errors=$errors)"
    }
}
package com.jiyingcao.a51fengliu.api.response

/**
 * {
 *     "code": -1,
 *     "msg": "Validation Error",
 *     "data": {
 *         "password": "请输入正确的密码",
 *         "name": "请输入正确的用户名或者邮箱"
 *     }
 * }
 */
data class LoginErrorData(
    val name: String?,
    val password: String?
)

sealed class LoginResult {
    data class Success(val token: String) : LoginResult()
    data class Error(
        // code和msg就是ApiResponse中的code和msg
        val code: Int,
        val msg: String?,
        val data: LoginErrorData?
    ) : LoginResult()
}
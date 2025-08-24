package com.jiyingcao.a51fengliu.api.response

/**
 * {
 *     "code": 0,
 *     "msg": "Ok",
 *     "data": "some.jwt.token"
 * }
 */

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

/**
 * 登录响应的特殊处理
 * 使用sealed class来表示不同的响应状态
 */
data class LoginResponse(
    val code: Int,
    val msg: String?,
    val data: LoginData
)

/**
 * 登录数据的多态表示
 */
sealed class LoginData {
    data class Success(val token: String) : LoginData()
    data class Error(val errors: Map<String, String>) : LoginData()
}
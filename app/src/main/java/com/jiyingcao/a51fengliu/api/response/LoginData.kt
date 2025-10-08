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
 * 登录接口的data字段多态表示
 *
 * ## 使用场景
 * `ApiResponse<LoginData>` - 登录接口的 `data` 字段在成功/失败时类型不同：
 * - 成功: `data` 是字符串 token
 * - 失败: `data` 是字段错误 Map
 *
 * ## 特殊处理
 * 由于多态性，登录接口**无法使用** [com.jiyingcao.a51fengliu.repository.apiCall]，
 * 需要在 Repository 层手动处理，参见 [com.jiyingcao.a51fengliu.repository.UserRepository.login]
 *
 * @see com.jiyingcao.a51fengliu.api.parse.LoginDataTypeAdapter 自定义反序列化器
 */
sealed class LoginData {
    /** 登录成功，包含JWT token */
    data class Success(val token: String) : LoginData()

    /** 登录失败，包含字段级错误信息 */
    data class Error(val errors: Map<String, String>) : LoginData()
}
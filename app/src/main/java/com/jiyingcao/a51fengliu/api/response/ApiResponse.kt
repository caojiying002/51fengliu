package com.jiyingcao.a51fengliu.api.response

import com.jiyingcao.a51fengliu.domain.exception.ApiException

/**
 * 通用API响应基类
 *
 * ## 标准用法
 * 大多数接口直接使用此类型，例如：
 * ```kotlin
 * suspend fun getProfile(): Response<ApiResponse<Profile>>
 * ```
 *
 * ## 特殊情况：data 字段多态
 * 某些接口的 `data` 字段在成功/失败时类型不同，需要自定义 TypeAdapter：
 *
 * 1. **登录接口** - [LoginData] + [com.jiyingcao.a51fengliu.api.parse.LoginDataTypeAdapter]
 *    - 成功: `data` 是字符串 token
 *    - 失败: `data` 是字段错误 Map
 *
 * 2. **举报接口** - [ReportData] + [com.jiyingcao.a51fengliu.api.parse.ReportDataTypeAdapter]
 *    - 成功: `data` 是空字符串 ""
 *    - 失败: `data` 是字段错误 Map 或 null
 *
 * @see com.jiyingcao.a51fengliu.repository.UserRepository.login 登录接口特殊处理示例
 * @see com.jiyingcao.a51fengliu.repository.RecordRepository.report 举报接口特殊处理示例
 */
data class ApiResponse<T>(
    val code: Int,
    val msg: String?,
    val data: T?
) {
    fun isSuccessful() = (code == 0)
}

/** API响应的扩展函数，用于快速检查响应状态并抛出异常 */
fun <T> ApiResponse<T>.throwIfUnsuccessful() {
    if (code != 0) {
        throw ApiException.createFromResponse(this)
    }
}
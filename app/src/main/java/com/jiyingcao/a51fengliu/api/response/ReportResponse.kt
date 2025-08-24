package com.jiyingcao.a51fengliu.api.response

/**
 * {
 *     "code": 0,
 *     "msg": "Ok",
 *     "data": ""
 * }
 */

/**
 * {
 *     "code": -2,
 *     "msg": "已经举报过此信息",
 *     "data": null
 * }
 */

/**
 * {
 *     "code": -1,
 *     "msg": "Validation Error",
 *     "data": {
 *         "content": "举报原因不得低于15个字"
 *     }
 * }
 */

/**
 * {
 *     "code": -1,
 *     "msg": "Validation Error",
 *     "data": {
 *         "picture": "请使用正确的图片"
 *     }
 * }
 */

/**
 * 举报响应的特殊处理
 * 使用sealed class来表示不同的响应状态
 */
data class ReportResponse(
    val code: Int,
    val msg: String?,
    val data: ReportData
)

/**
 * 举报数据的多态表示
 */
sealed class ReportData {
    data object Success : ReportData()
    data class Error(val errors: Map<String, String>) : ReportData()
}
package com.jiyingcao.a51fengliu.api.response

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
data class ReportErrorData (
    val content: String?,
)
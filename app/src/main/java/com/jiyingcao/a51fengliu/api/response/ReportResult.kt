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

/**
 * {
 *     "code": -1,
 *     "msg": "Validation Error",
 *     "data": {
 *         "picture": "请使用正确的图片"
 *     }
 * }
 */
data class ReportErrorData (
    val content: String?,
    val picture: String?,
)
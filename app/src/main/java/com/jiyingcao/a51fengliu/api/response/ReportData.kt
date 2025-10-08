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
 * 举报接口的data字段多态表示
 *
 * ## 使用场景
 * `ApiResponse<ReportData>` - 举报接口的 `data` 字段在成功/失败时类型不同：
 * - 成功: `data` 是空字符串 ""
 * - 失败: `data` 是字段错误 Map 或 null
 *
 * ## 特殊处理
 * 由于多态性，举报接口**无法使用** [com.jiyingcao.a51fengliu.repository.apiCall]，
 * 需要在 Repository 层手动处理，参见 [com.jiyingcao.a51fengliu.repository.RecordRepository.report]
 *
 * @see com.jiyingcao.a51fengliu.api.parse.ReportDataTypeAdapter 自定义反序列化器
 */
sealed class ReportData {
    /** 举报成功 */
    data object Success : ReportData()

    /** 举报失败，包含字段级错误信息 */
    data class Error(val errors: Map<String, String>) : ReportData()
}
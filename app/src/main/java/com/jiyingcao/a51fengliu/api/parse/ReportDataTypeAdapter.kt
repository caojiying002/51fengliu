package com.jiyingcao.a51fengliu.api.parse

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jiyingcao.a51fengliu.api.response.ReportData
import com.jiyingcao.a51fengliu.util.AppLogger
import java.lang.reflect.Type

/**
 * [ReportData] 反序列化适配器
 *
 * 只根据 data 字段的内容结构判断类型，不依赖外层 code：
 * - data 是 JsonObject（字段错误详情） → ReportData.Error(map)
 * - data 是空字符串 "" → ReportData.Success
 * - data 是 null/其他 → ReportData.Error(emptyMap())
 *
 * 注意：调用方需要结合 ApiResponse.code 判断业务成功/失败
 */
class ReportDataTypeAdapter : JsonDeserializer<ReportData> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ReportData {
        return when {
            // 空字符串 → Success（成功时服务端返回 ""）
            json?.isJsonPrimitive == true && json.asJsonPrimitive.isString
                && json.asString.isEmpty() -> ReportData.Success

            // JsonObject → 解析为字段错误详情
            json?.isJsonObject == true -> {
                try {
                    val errors = context?.deserialize<Map<String, String>>(
                        json,
                        object : TypeToken<Map<String, String>>() {}.type
                    ) ?: emptyMap()
                    ReportData.Error(errors)
                } catch (e: Exception) {
                    AppLogger.w("ReportDataTypeAdapter failed to parse error map: ${e.message}")
                    ReportData.Error(emptyMap())
                }
            }

            // null 或其他情况 → Error(emptyMap())
            // 包括：{"code": -2, "data": null} 这种失败但无字段错误的情况
            else -> {
                if (json != null && !json.isJsonNull) {
                    AppLogger.w("ReportDataTypeAdapter unexpected data type: ${json::class.simpleName}")
                }
                ReportData.Error(emptyMap())
            }
        }
    }
}

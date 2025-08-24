package com.jiyingcao.a51fengliu.api.parse

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jiyingcao.a51fengliu.api.response.ReportData
import com.jiyingcao.a51fengliu.api.response.ReportResponse
import java.lang.reflect.Type

/**
 * ReportResponse的自定义Gson适配器
 * 处理成功时data为空字符串，失败时data为Map<String, String>或null的情况
 */
class ReportResponseAdapter : JsonDeserializer<ReportResponse> {
    
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ReportResponse {
        val jsonObject = json.asJsonObject
        val code = jsonObject.get("code").asInt
        val msg = jsonObject.get("msg")?.asString
        val dataElement = jsonObject.get("data")
        
        val reportData = when {
            code == 0 -> {
                // 成功情况：不管data是什么，都表示成功
                ReportData.Success
            }
            code != 0 && dataElement != null && dataElement.isJsonObject -> {
                // 失败情况：data是错误信息对象
                val errors = context.deserialize<Map<String, String>>(
                    dataElement,
                    object : TypeToken<Map<String, String>>() {}.type
                )
                ReportData.Error(errors)
            }
            else -> {
                // 其他失败情况（如code != 0但data为null），创建默认错误数据
                ReportData.Error(mapOf("error" to (msg ?: "Unknown error")))
            }
        }
        
        return ReportResponse(code, msg, reportData)
    }
}
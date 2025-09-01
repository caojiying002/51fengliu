package com.jiyingcao.a51fengliu.api.parse

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.jiyingcao.a51fengliu.api.response.NoData
import com.jiyingcao.a51fengliu.util.AppLogger
import java.lang.reflect.Type

/**
 * [NoData] 反序列化适配器。
 * 宽容策略：任何能解析到的非常规内容都打印警告日志并返回 [NoData]，避免接口形态小变动导致崩溃。
 */
class NoDataTypeAdapter : JsonDeserializer<NoData> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): NoData {
        return when {
            json == null || json is JsonNull -> NoData
            json is JsonPrimitive && json.isString && json.asString.isEmpty() -> NoData
            json is JsonObject && json.entrySet().isEmpty() -> NoData
            else -> {
                // 非期望内容，记录一次警告日志
                AppLogger.w("NoDataTypeAdapter unexpected data for NoData: ${json.toString()?.take(200)}")
                NoData
            }
        }
    }
}

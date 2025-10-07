package com.jiyingcao.a51fengliu.api.parse

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jiyingcao.a51fengliu.api.response.LoginData
import com.jiyingcao.a51fengliu.util.AppLogger
import java.lang.reflect.Type

/**
 * [LoginData] 反序列化适配器
 *
 * 处理登录接口的特殊data字段：
 * - 成功时: data 是字符串 token → LoginData.Success(token)
 * - 失败时: data 是 Map<String, String> → LoginData.Error(errors)
 */
class LoginDataTypeAdapter : JsonDeserializer<LoginData> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LoginData {
        return when {
            // 字符串 → Success(token)
            json?.isJsonPrimitive == true && json.asJsonPrimitive.isString -> {
                LoginData.Success(json.asString)
            }

            // JsonObject → 解析为错误详情
            json?.isJsonObject == true -> {
                try {
                    val errors = context?.deserialize<Map<String, String>>(
                        json,
                        object : TypeToken<Map<String, String>>() {}.type
                    ) ?: emptyMap()
                    LoginData.Error(errors)
                } catch (e: Exception) {
                    AppLogger.w("LoginDataTypeAdapter failed to parse error map: ${e.message}")
                    LoginData.Error(emptyMap())
                }
            }

            // null 或其他异常情况
            else -> {
                AppLogger.w("LoginDataTypeAdapter unexpected data type: ${json?.toString()?.take(100)}")
                LoginData.Error(emptyMap())
            }
        }
    }
}

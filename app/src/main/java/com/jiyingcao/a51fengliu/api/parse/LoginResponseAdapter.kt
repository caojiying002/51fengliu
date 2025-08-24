package com.jiyingcao.a51fengliu.api.parse

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jiyingcao.a51fengliu.api.response.LoginData
import com.jiyingcao.a51fengliu.api.response.LoginResponse
import java.lang.reflect.Type

/**
 * LoginResponse的自定义Gson适配器
 * 处理成功时data为字符串，失败时data为Map<String, String>的情况
 */
class LoginResponseAdapter : JsonDeserializer<LoginResponse> {
    
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LoginResponse {
        val jsonObject = json.asJsonObject
        val code = jsonObject.get("code").asInt
        val msg = jsonObject.get("msg")?.asString
        val dataElement = jsonObject.get("data")
        
        val loginData = when {
            code == 0 && dataElement.isJsonPrimitive -> {
                // 成功情况：data是字符串token
                LoginData.Success(dataElement.asString)
            }
            code != 0 && dataElement.isJsonObject -> {
                // 失败情况：data是错误信息对象
                val errors = context.deserialize<Map<String, String>>(
                    dataElement,
                    object : TypeToken<Map<String, String>>() {}.type
                )
                LoginData.Error(errors)
            }
            else -> {
                // 其他异常情况，创建一个默认的错误数据
                LoginData.Error(mapOf("error" to (msg ?: "Unknown error")))
            }
        }
        
        return LoginResponse(code, msg, loginData)
    }
}
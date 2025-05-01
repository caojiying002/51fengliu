package com.jiyingcao.a51fengliu.api

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.*
import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.api.response.ApiResult
import com.jiyingcao.a51fengliu.api.response.LoginErrorData
import com.jiyingcao.a51fengliu.api.response.ReportErrorData
import com.jiyingcao.a51fengliu.util.AppLogger
import java.lang.reflect.Type

private const val TAG = "GsonInstance"

object GsonInstance {
    val gson: Gson by lazy {
        GsonBuilder()
            .registerApiResponseType<String, LoginErrorData>()
            .registerApiResponseType<String, ReportErrorData>()
            //.registerTypeAdapterFactory(NullStringToEmptyAdapterFactory())
            //.registerTypeAdapter(String::class.java, NullStringDeserializer())
            //.registerTypeAdapter(ItemData::class.java, NullObjectDeserializer(ItemData::class.java))
            .create()
    }
}

class ApiResponseDeserializer<T, E>(
    private val successType: Type,
    private val errorType: Type
) : JsonDeserializer<ApiResponse<ApiResult<T, E>>> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ApiResponse<ApiResult<T, E>> {
        val jsonObject = json.asJsonObject
        val code = jsonObject.get("code").asInt
        val msg = jsonObject.get("msg")?.asString
        val data = jsonObject.get("data")

        val result = when {
            code == 0 && !data.isJsonNull -> {
                val successData = context.deserialize<T>(data, successType)
                ApiResult.Success(successData)
            }
            data.isJsonNull -> {
                ApiResult.Error(code, msg, null)
            }
            else -> {
                val errorData = context.deserialize<E>(data, errorType)
                ApiResult.Error(code, msg, errorData)
            }
        }

        return ApiResponse(code, msg, result)
    }
}

inline fun <reified S, reified E> GsonBuilder.registerApiResponseType(): GsonBuilder = apply {
    registerTypeAdapter(
        TypeToken.getParameterized(
            ApiResponse::class.java,
            TypeToken.getParameterized(
                ApiResult::class.java,
                S::class.java,
                E::class.java
            ).type
        ).type,
        ApiResponseDeserializer<S, E>(S::class.java, E::class.java)
    )
}

/**
 * 防御性编程：如果字段的值为null，将其替换为空字符串。
 */
class NullStringToEmptyAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        val delegate: TypeAdapter<T> = gson.getDelegateAdapter(this, type)
        val elementAdapter: TypeAdapter<JsonElement> = gson.getAdapter(JsonElement::class.java)

        return object : TypeAdapter<T>() {
            override fun read(`in`: JsonReader): T {
                val jsonElement = elementAdapter.read(`in`)
                if (jsonElement.isJsonObject) {
                    val jsonObject = jsonElement.asJsonObject
                    /* 用于打印信息 */ val jsonObjectToString: String = jsonObject.toString()
                    val clazz = type.rawType
                    val fields = clazz.declaredFields
                    for (field in fields) {
                        if (field.type == String::class.java) {
                            val key = field.name
                            if (!jsonObject.has(key)) {
                                AppLogger.w(TAG, "Non-exist key: [$key], in JsonObject: $jsonObjectToString")
                                jsonObject.add(key, JsonPrimitive(""))
                            } else if(jsonObject.get(key).isJsonNull) {
                                AppLogger.w(TAG, "Null value for key: [$key], in JsonObject: $jsonObjectToString")
                                jsonObject.add(key, JsonPrimitive(""))
                            }
                        }
                    }
                }
                return delegate.fromJsonTree(jsonElement)
            }

            override fun write(out: JsonWriter, value: T) {
                delegate.write(out, value)
            }
        }/*.nullSafe()*/
    }
}

/**
 * 防御性编程：用于处理服务器返回的空字符串
 */
class NullStringDeserializer : JsonDeserializer<String> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): String {
        if (json == null || json.isJsonNull/* || json.asString == "null" || json.asString == "NULL" || json.asString == "Null"*/)
            return ""
        return json.asString
    }
}

/**
 * 防御性编程：用于处理服务器返回的空对象
 */
class NullObjectDeserializer<T>(private val clazz: Class<T>) : JsonDeserializer<T> where T : Any {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): T {
        return if (json == null || json.isJsonNull) {
            clazz.getDeclaredConstructor().newInstance() // 返回一个新的空对象实例
        } else {
            Gson().fromJson(json, clazz) // 正常反序列化，创建一个新Gson实例避免无限递归
        }
    }
}

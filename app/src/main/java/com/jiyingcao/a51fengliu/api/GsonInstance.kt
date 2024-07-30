package com.jiyingcao.a51fengliu.api

import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.*
import java.lang.reflect.Type

private const val TAG = "GsonInstance"

object GsonInstance {
    val gson: Gson by lazy {
        GsonBuilder()
            //.registerTypeAdapterFactory(NullStringToEmptyAdapterFactory())
            //.registerTypeAdapter(String::class.java, NullStringDeserializer())
            //.registerTypeAdapter(ItemData::class.java, NullObjectDeserializer(ItemData::class.java))
            .create()
    }
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
                                Log.w(TAG, "Non-exist key: [$key], in JsonObject: $jsonObjectToString")
                                jsonObject.add(key, JsonPrimitive(""))
                            } else if(jsonObject.get(key).isJsonNull) {
                                Log.w(TAG, "Null value for key: [$key], in JsonObject: $jsonObjectToString")
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

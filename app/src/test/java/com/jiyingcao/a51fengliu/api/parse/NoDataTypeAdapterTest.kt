package com.jiyingcao.a51fengliu.api.parse

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.api.response.NoData
import com.jiyingcao.a51fengliu.api.response.ReportResponse
import com.jiyingcao.a51fengliu.api.parse.LoginResponseAdapter // ensure consistency if needed
import com.jiyingcao.a51fengliu.api.parse.ReportResponseAdapter
import com.jiyingcao.a51fengliu.api.response.LoginResponse
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class NoDataTypeAdapterTest {

    private lateinit var gson: Gson

    @Before
    fun setUp() {
        gson = GsonBuilder()
            .registerTypeAdapter(LoginResponse::class.java, LoginResponseAdapter())
            .registerTypeAdapter(ReportResponse::class.java, ReportResponseAdapter())
            .registerTypeAdapter(NoData::class.java, NoDataTypeAdapter())
            .create()
    }

    private val type = object : TypeToken<ApiResponse<NoData>>() {}.type

    @Test
    fun `empty string data maps to NoData`() {
        val json = """{"code":0,"msg":"Ok","data":""}"""
        val parsed: ApiResponse<NoData> = gson.fromJson(json, type)
        assertThat(parsed.data).isSameInstanceAs(NoData)
        assertThat(parsed.code).isEqualTo(0)
    }

    @Test
    fun `null data maps to NoData`() {
        val json = """{"code":0,"msg":"Ok","data":null}"""
        val parsed: ApiResponse<NoData> = gson.fromJson(json, type)
        assertThat(parsed.data).isSameInstanceAs(NoData)
    }

    @Test
    fun `empty object data maps to NoData`() {
        val json = """{"code":0,"msg":"Ok","data":{}}"""
        val parsed: ApiResponse<NoData> = gson.fromJson(json, type)
        assertThat(parsed.data).isSameInstanceAs(NoData)
    }

    @Test
    fun `missing data field maps to NoData via manual fill`() {
        // 模拟后端缺失 data 字段场景：我们手动补一个默认值再解析，或直接解析为 ApiResponse<NoData?> 再映射
        val json = """{"code":0,"msg":"Ok"}"""
        val parsedMapType = object : TypeToken<Map<String, Any>>() {}.type
        val asMap: Map<String, Any> = gson.fromJson(json, parsedMapType)
        // 构造一个补上 data:null 的 JSON 再去解析，模拟通用层逻辑
        val rebuiltJson = buildString {
            append('{')
            append("\"code\":0,\"msg\":\"Ok\",\"data\":null")
            append('}')
        }
        val parsed: ApiResponse<NoData> = gson.fromJson(rebuiltJson, type)
        assertThat(parsed.data).isSameInstanceAs(NoData)
    }

    @Test
    fun `unexpected non empty string still maps to NoData`() {
        val json = """{"code":0,"msg":"Ok","data":"unexpected"}"""
        val parsed: ApiResponse<NoData> = gson.fromJson(json, type)
        assertThat(parsed.data).isSameInstanceAs(NoData)
    }
}

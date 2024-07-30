package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.response.ApiResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {

    @POST("/public/index/data/getdata.html")
    suspend fun getData(): ApiResponse

    @FormUrlEncoded
    @POST("/public/index/data/getdata.html")
    suspend fun getCityData(
        @Field("pid") pid: String = "330000",
        @Field("cid") cid: String = "330100",
        @Field("page") page: Int = 1,
        @Field("ontype") ontype: Int = 0,
        @Field("keywords") keywords: String = "0",
    ): ApiResponse

    @FormUrlEncoded
    @POST("/public/index/data/getdata.html")
    suspend fun search(
        /*@Field("pid") pid: String = "330000",*/
        /*@Field("cid") cid: String = "330100",*/
        @Field("page") page: Int = 1,
        /*@Field("ontype") ontype: String = "0",*/
        @Field("keywords") keywords: String = "老龙凤冰冰姐",
    ): ApiResponse
}

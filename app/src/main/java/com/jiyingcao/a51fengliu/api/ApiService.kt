package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.response.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("/api/web/info/page.json")
    suspend fun getPageData(
        @Query("perPage") perPage: Int = 60,
        @Query("sort") sort: String = "daily",
    ): ApiResponse2<PageData>

    @GET("/api/web/info/page.json")
    suspend fun getCityData2(
        @Query("cityCode") cityCode: String,
        @Query("sort") sort: String = "publish",
        @Query("page") page: Int = 1,
    ): ApiResponse2<PageData>

    @GET("/api/web/info/page.json")
    suspend fun search2(
        @Query("keywords") keywords: String,
        @Query("page") page: Int = 1,
    ): ApiResponse2<PageData>

    @GET("/api/web/info/detail.json")
    suspend fun getDetail(
        @Query("infoId") id: String
    ): ApiResponse2<Record>



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

package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.response.*
import retrofit2.http.*

interface ApiService {

    /** 首页：热门 */
    @GET("/api/web/info/page.json")
    suspend fun getHotRecords(
        //@Query("perPage") perPage: Int = 30,
        @Query("sort") sort: String = "daily",
        @Query("page") page: Int = 1,
    ): ApiResponse<PageData>

    /** 首页：最新 */
    @GET("/api/web/info/page.json")
    suspend fun getLatestRecords(
        //@Query("perPage") perPage: Int = 30,
        @Query("sort") sort: String = "publish",
        @Query("page") page: Int = 1,
    ): ApiResponse<PageData>

    @GET("/api/web/info/page.json")
    suspend fun getCityData2(
        @Query("cityCode") cityCode: String,
        @Query("sort") sort: String = "publish",
        @Query("page") page: Int = 1,
    ): ApiResponse<PageData>

    @GET("/api/web/info/page.json")
    suspend fun search2(
        @Query("keywords") keywords: String,
        @Query("page") page: Int = 1,
    ): ApiResponse<PageData>

    @GET("/api/web/info/detail.json")
    suspend fun getDetail(
        @Query("infoId") id: String
    ): ApiResponse<RecordInfo>
}

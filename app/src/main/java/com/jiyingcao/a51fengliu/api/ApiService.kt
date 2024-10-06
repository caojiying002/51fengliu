package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.response.*
import retrofit2.http.*

interface ApiService {

    /** 首页：热门or最新 */
    @GET("/api/web/info/page.json")
    suspend fun getPageData(
        //@Query("perPage") perPage: Int = 30,
        @Query("sort") sort: String = "daily",  // "publish"表示最新
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

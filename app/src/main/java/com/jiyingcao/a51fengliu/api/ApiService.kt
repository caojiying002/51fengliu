package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.response.*
import retrofit2.http.*

interface ApiService {

    @GET("/api/web/info/page.json")
    suspend fun getRecords(
        @QueryMap map: Map<String, String>
    ) : ApiResponse<PageData>

    /** 首页：热门or最新 */
    @Deprecated("Use getRecords instead")
    @GET("/api/web/info/page.json")
    suspend fun getPageData(
        //@Query("perPage") perPage: Int = 30,
        @Query("sort") sort: String = "daily",  // "publish"表示最新
        @Query("page") page: Int = 1,
    ): ApiResponse<PageData>

    @Deprecated("Use getRecords instead")
    @GET("/api/web/info/page.json")
    suspend fun getCityData2(
        @Query("cityCode") cityCode: String,
        @Query("sort") sort: String = "publish",
        @Query("page") page: Int = 1,
    ): ApiResponse<PageData>

    @Deprecated("Use getRecords instead")
    @GET("/api/web/info/page.json")
    suspend fun search2(
        @Query("keywords") keywords: String,
        @Query("page") page: Int = 1,
    ): ApiResponse<PageData>

    // TODO @Deprecated("Use getRecords instead")
    @GET("/api/web/info/page.json")
    suspend fun search4(
        @Query("keywords") keywords: String,
        @Query("cityCode") cityCode: String,
        @Query("page") page: Int = 1,
    ): ApiResponse<PageData>

    @GET("/api/web/info/detail.json")
    suspend fun getDetail(
        @Query("infoId") id: String
    ): ApiResponse<RecordInfo>

    /** 登录 */
    @POST("/api/web/auth/login.json")
    suspend fun postLogin(
        @Body body: LoginRequest
    ): ApiResponse<ApiResult<String, LoginErrorData>>

    /** 退出登录 */
    @POST("/api/web/auth/logout.json")
    suspend fun postLogout(): ApiResponse<Nothing>   // {"code":0,"msg":"Ok","data":""}

    /** 个人中心：用户信息（需登录） */
    @GET("/api/web/authUser/detail.json")
    suspend fun getProfile(): ApiResponse<Profile>

    /** 收藏(需登录)：已收藏会报错，code=-2，msg="已经收藏过了" */
    @POST("/api/web/info/favorite.json")
    suspend fun postFavorite(
        @Body body: InfoIdRequest
    ): ApiResponse<Nothing>

    /** 取消收藏(需登录)：未收藏会报错，code=-2，msg="Failed" */
    @POST("/api/web/info/unfavorite.json")
    suspend fun postUnfavorite(
        @Body body: InfoIdRequest
    ): ApiResponse<Nothing>
}

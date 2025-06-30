package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.TokenPolicy.Policy
import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.request.ReportRequest
import com.jiyingcao.a51fengliu.api.response.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    @TokenPolicy(Policy.OPTIONAL)
    @GET("/api/mobile/info/page.json")
    suspend fun getRecords(
        @QueryMap map: Map<String, String>
    ): ApiResponse<PageData<RecordInfo>>

    @TokenPolicy(Policy.OPTIONAL)
    @GET("/api/mobile/info/detail.json")
    suspend fun getDetail(
        @Query("infoId") id: String
    ): ApiResponse<RecordInfo>

    /** 登录 */
    @TokenPolicy(Policy.FORBIDDEN)
    @POST("/api/mobile/auth/login.json")
    suspend fun postLogin(
        @Body body: LoginRequest
    ): ApiResponse<ApiResult<String, LoginErrorData>>

    /** 退出登录 */
    @TokenPolicy(Policy.REQUIRED)
    @POST("/api/mobile/auth/logout.json")
    suspend fun postLogout(): ApiResponse<Nothing>   // {"code":0,"msg":"Ok","data":""}

    /** 个人中心：用户信息（需登录） */
    @TokenPolicy(Policy.REQUIRED)
    @GET("/api/mobile/authUser/detail.json")
    suspend fun getProfile(): ApiResponse<Profile>

    /** 收藏（需登录）：已收藏会报错，code=-2，msg="已经收藏过了" */
    @TokenPolicy(Policy.REQUIRED)
    @POST("/api/mobile/info/favorite.json")
    suspend fun postFavorite(
        @Body body: InfoIdRequest
    ): ApiResponse<Nothing>

    /** 取消收藏（需登录）：未收藏会报错，code=-2，msg="Failed" */
    @TokenPolicy(Policy.REQUIRED)
    @POST("/api/mobile/info/unfavorite.json")
    suspend fun postUnfavorite(
        @Body body: InfoIdRequest
    ): ApiResponse<Nothing>

    /** 我的收藏（需登录） */
    @TokenPolicy(Policy.REQUIRED)
    @GET("/api/mobile/authUser/favoritePage.json")
    suspend fun getFavorites(
        @Query("page") page: Int = 1
    ): ApiResponse<PageData<RecordInfo>>

    /** 上传图片（需登录） */
    @TokenPolicy(Policy.REQUIRED)
    @Multipart
    @POST("/api/mobile/info/upload.json")
    suspend fun postUpload(
        @Part file: MultipartBody.Part
    ): ApiResponse<String>

    /** 举报（居然不登录也能举报） */
    @TokenPolicy(Policy.OPTIONAL)
    @POST("/api/mobile/info/report.json")
    suspend fun postReport(
        @Body body: ReportRequest
    ): ApiResponse<ApiResult<String, ReportErrorData>>

    /** 商家列表（分页） */
    @TokenPolicy(Policy.OPTIONAL)
    @GET("/api/mobile/merchant/page.json")
    suspend fun getMerchants(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int? = null
    ): ApiResponse<PageData<Merchant>>

    /** 商家详情 */
    @TokenPolicy(Policy.OPTIONAL)
    @GET("/api/mobile/merchant/detail.json")
    suspend fun getMerchantDetail(
        @Query("merchantId") id: String
    ): ApiResponse<Merchant>

    /** 商家城市列表 */
    @TokenPolicy(Policy.OPTIONAL)
    @GET("/api/mobile/config/merchantCity.json")
    suspend fun getMerchantCities(): ApiResponse<List<MerchantCity>>
}

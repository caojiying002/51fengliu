package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.TokenPolicy.Policy
import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.request.ReportRequest
import com.jiyingcao.a51fengliu.api.response.*
import com.jiyingcao.a51fengliu.util.City
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * API服务接口
 * 定义所有后端API接口方法，使用统一的路径常量管理
 */
interface ApiService {

    // ========== 信息内容相关接口 ==========

    /**
     * 获取信息列表（分页）
     * 支持首页信息流、城市信息流、搜索结果等场景
     * 
     * @param map 查询参数Map，通常由RecordsRequest.toMap()生成，包含城市代码、关键字、排序方式、页码等
     * @return 分页的信息记录列表
     */
    @TokenPolicy(Policy.OPTIONAL)
    @GET(ApiEndpoints.Records.PAGE)
    suspend fun getRecords(
        @QueryMap map: Map<String, String>
    ): ApiResponse<PageData<RecordInfo>>

    /**
     * 获取信息详情
     * 获取单条信息的详细内容
     * 
     * @param id 信息ID
     * @return 信息详情数据
     */
    @TokenPolicy(Policy.OPTIONAL)
    @GET(ApiEndpoints.Records.DETAIL)
    suspend fun getDetail(
        @Query("infoId") id: String
    ): ApiResponse<RecordInfo>

    /**
     * 收藏信息
     * 需要用户登录状态，已收藏会报错（code=-2，msg="已经收藏过了"）
     * 
     * @param body 包含信息ID的请求体
     * @return 收藏操作结果
     */
    @TokenPolicy(Policy.REQUIRED)
    @POST(ApiEndpoints.Records.FAVORITE)
    suspend fun postFavorite(
        @Body body: InfoIdRequest
    ): ApiResponse<Nothing>

    /**
     * 取消收藏信息
     * 需要用户登录状态，未收藏会报错（code=-2，msg="Failed"）
     * 
     * @param body 包含信息ID的请求体
     * @return 取消收藏操作结果
     */
    @TokenPolicy(Policy.REQUIRED)
    @POST(ApiEndpoints.Records.UNFAVORITE)
    suspend fun postUnfavorite(
        @Body body: InfoIdRequest
    ): ApiResponse<Nothing>

    /**
     * 上传图片
     * 需要用户登录状态
     * 
     * @param file 要上传的图片文件
     * @return 上传成功后的图片URL
     */
    @TokenPolicy(Policy.REQUIRED)
    @Multipart
    @POST(ApiEndpoints.Records.UPLOAD)
    suspend fun postUpload(
        @Part file: MultipartBody.Part
    ): ApiResponse<String>

    /**
     * 举报信息
     * 不需要登录也可以举报
     * 
     * @param body 举报请求参数，包含信息ID、举报内容、举报图片等
     * @return 举报操作结果，包含成功信息或错误详情
     */
    @TokenPolicy(Policy.OPTIONAL)
    @POST(ApiEndpoints.Records.REPORT)
    suspend fun postReport(
        @Body body: ReportRequest
    ): ApiResponse<ApiResult<String, ReportErrorData>>

    // ========== 用户认证相关接口 ==========

    /**
     * 用户登录
     * 
     * @param body 登录请求参数，包含用户名和密码
     * @return 登录结果，成功时返回token，失败时返回错误信息
     */
    @TokenPolicy(Policy.FORBIDDEN)
    @POST(ApiEndpoints.Auth.LOGIN)
    suspend fun postLogin(
        @Body body: LoginRequest
    ): ApiResponse<ApiResult<String, LoginErrorData>>

    /**
     * 用户退出登录
     * 注销当前用户会话
     * 
     * @return 表示退出登录成功或失败的响应
     */
    @TokenPolicy(Policy.REQUIRED)
    @POST(ApiEndpoints.Auth.LOGOUT)
    suspend fun postLogout(): ApiResponse<Nothing>

    // ========== 用户个人资料相关接口 ==========

    /**
     * 获取个人中心用户信息
     * 需要用户登录状态
     * 
     * @return 用户个人资料信息
     */
    @TokenPolicy(Policy.REQUIRED)
    @GET(ApiEndpoints.User.PROFILE)
    suspend fun getProfile(): ApiResponse<Profile>

    /**
     * 获取我的收藏列表
     * 需要用户登录状态
     * 
     * @param page 页码，默认从1开始
     * @return 分页的收藏记录列表
     */
    @TokenPolicy(Policy.REQUIRED)
    @GET(ApiEndpoints.User.FAVORITES)
    suspend fun getFavorites(
        @Query("page") page: Int = 1
    ): ApiResponse<PageData<RecordInfo>>

    // ========== 商家相关接口 ==========

    /**
     * 获取商家列表（分页）
     * 获取所有商家的分页列表
     * 
     * @param page 页码，默认从1开始
     * @param perPage 每页数量，可选参数
     * @return 分页的商家列表
     */
    @TokenPolicy(Policy.OPTIONAL)
    @GET(ApiEndpoints.Merchant.PAGE)
    suspend fun getMerchants(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int? = null
    ): ApiResponse<PageData<Merchant>>

    /**
     * 获取商家详情
     * 获取指定商家的详细信息
     * 
     * @param id 商家ID
     * @return 商家详情数据
     */
    @TokenPolicy(Policy.OPTIONAL)
    @GET(ApiEndpoints.Merchant.DETAIL)
    suspend fun getMerchantDetail(
        @Query("merchantId") id: String
    ): ApiResponse<Merchant>

    /**
     * 获取商家城市列表
     * 获取所有有商家的城市列表
     * 
     * @return 城市列表
     */
    @TokenPolicy(Policy.OPTIONAL)
    @GET(ApiEndpoints.Merchant.CITIES)
    suspend fun getMerchantCities(): ApiResponse<List<City>>
}
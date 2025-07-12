package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.TokenPolicy.Policy
import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.ReportRequest
import com.jiyingcao.a51fengliu.api.response.*
import okhttp3.MultipartBody
import retrofit2.http.*

/**
 * 信息内容相关的API接口
 * 处理信息浏览、收藏、上传和举报等操作
 */
interface RecordApiService {

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
}
package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.TokenPolicy.Policy
import com.jiyingcao.a51fengliu.api.response.*
import retrofit2.http.*

/**
 * 用户个人资料相关的API接口
 * 处理用户信息和收藏列表相关操作
 */
interface UserApiService {

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
}
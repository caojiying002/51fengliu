package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.TokenPolicy.Policy
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.response.*
import retrofit2.http.*

/**
 * 用户认证相关的API接口
 * 处理用户登录和退出登录操作
 */
interface AuthApiService {

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
}
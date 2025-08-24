package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.response.LoginData
import com.jiyingcao.a51fengliu.api.response.Profile
import com.jiyingcao.a51fengliu.domain.exception.LoginException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @return Flow<Result<String>> 包含登录结果的结果流，成功时包含token
     */
    fun login(username: String, password: String): Flow<Result<String>> = flow {
        try {
            val response = apiService.postLogin(LoginRequest(username, password))
            when (val loginData = response.data) {
                is LoginData.Success -> {
                    emit(Result.success(loginData.token))
                }
                is LoginData.Error -> {
                    emit(Result.failure(
                        LoginException(
                            code = response.code,
                            message = response.msg,
                            errors = loginData.errors
                        )
                    ))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取个人中心用户信息
     * @return Flow<Result<Profile>> 包含用户信息的结果流
     */
    fun getProfile(): Flow<Result<Profile?>> = apiCall {
        apiService.getProfile()
    }

    /**
     * 退出登录（注销）
     * @return Flow<Result<*> 表示注销成功或失败的结果流
     */
    fun logout(): Flow<Result<*>> = apiCall {
        apiService.postLogout()
    }
}
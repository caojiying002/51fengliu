package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.response.ApiResult
import com.jiyingcao.a51fengliu.api.response.Profile
import com.jiyingcao.a51fengliu.domain.exception.LoginException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UserRepository(
    private val apiService: ApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO    // TODO 是否移除
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
            when (val result = response.data) {
                is ApiResult.Success -> {
                    emit(Result.success(result.data))
                }
                is ApiResult.Error -> {
                    emit(Result.failure(
                        LoginException(
                            code = result.code,
                            message = result.msg,
                            errorData = result.errorData
                        )
                    ))
                }
                null -> {
                    emit(Result.failure(Exception("Empty response data")))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

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

    companion object {
        // 用于单例模式实现
        @Volatile
        private var instance: UserRepository? = null

        fun getInstance(apiService: ApiService): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository(apiService).also { instance = it }
            }
        }
    }
}
package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.response.ApiResult
import com.jiyingcao.a51fengliu.domain.exception.LoginException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UserRepository(
    private val apiService: ApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
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
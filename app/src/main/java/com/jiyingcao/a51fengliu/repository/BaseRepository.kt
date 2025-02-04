package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Repository基类，处理协程作用域 */
abstract class BaseRepository(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {

    protected fun <T> apiCall(call: suspend () -> ApiResponse<T>): Flow<Result<T>> = flow {
        try {
            val response = call()
            when {
                response.isSuccessful() -> {
                    response.data?.let {
                        emit(Result.success(it))
                    } ?: emit(Result.failure(Exception("Empty response data")))
                }
                else -> {
                    emit(Result.failure(ApiException.createFromResponse(response)))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)
}
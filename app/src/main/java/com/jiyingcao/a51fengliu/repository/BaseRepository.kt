package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import com.jiyingcao.a51fengliu.domain.exception.HttpEmptyResponseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException
import retrofit2.Response

/** Repository基类 */
abstract class BaseRepository {
    /**
     * 组合代替继承！！！！
     * 组合代替继承！！！！
     * 组合代替继承！！！！
     *
     * 除非万不得已，不要在Base类里写逻辑，考虑用其他的方式（e.g. 扩展函数、Kotlin委托）
     */
}

fun <T> apiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    call: suspend () -> Response<ApiResponse<T>>
): Flow<Result<T?>> = flow {
    try {
        val httpResponse = call()
        if (httpResponse.isSuccessful) {
            val apiResponse = httpResponse.body()
            if (apiResponse != null) {
                when {
                    apiResponse.isSuccessful() -> emit(Result.success(apiResponse.data))
                    else -> emit(Result.failure(ApiException.createFromResponse(apiResponse)))
                }
            } else {
                emit(Result.failure(HttpEmptyResponseException()))
            }
        } else {
            emit(Result.failure(HttpException(httpResponse)))
        }
    } catch (e: CancellationException) {
        // 重要：重新抛出[CancellationException]以保持协程取消机制
        // [CancellationException]是结构化并发的一部分，应该让它自然传播
        throw e
    } catch (e: Exception) {
        emit(Result.failure(e))
    }
}.flowOn(dispatcher)
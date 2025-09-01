package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.api.response.NoData
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import com.jiyingcao.a51fengliu.domain.exception.HttpEmptyResponseException
import com.jiyingcao.a51fengliu.domain.exception.MissingDataException
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

/**
 * 旧实现：允许 data 为 null，返回 Flow<Result<T?>>。
 * 迁移期重命名为 apiCallNullable。
 */
fun <T> apiCallNullable(
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

@Deprecated(
    message = "Deprecated nullable version. Use apiCallStrict for non-null data or apiCallNullable explicitly.",
    replaceWith = ReplaceWith("apiCallNullable(dispatcher, call)")
)
fun <T> apiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    call: suspend () -> Response<ApiResponse<T>>
): Flow<Result<T?>> = apiCallNullable(dispatcher, call)

/**
 * 严格版：业务成功时 data 不允许为 null；若为 null 且类型不是 NoData/Unit，抛出 MissingDataException。
 * 返回 Flow<Result<T>>（非空）。
 */
inline fun <reified T : Any> apiCallStrict(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    noinline call: suspend () -> Response<ApiResponse<T>>
): Flow<Result<T>> = flow {
    try {
        val httpResponse = call()
        if (!httpResponse.isSuccessful) {
            emit(Result.failure(HttpException(httpResponse)))
            return@flow
        }

        val apiResponse = httpResponse.body()
        if (apiResponse == null) {
            emit(Result.failure(HttpEmptyResponseException()))
            return@flow
        }

        if (!apiResponse.isSuccessful()) {
            emit(Result.failure(ApiException.createFromResponse(apiResponse)))
            return@flow
        }

        val data = apiResponse.data
        if (data == null) {
            when (T::class) {
                // 【特殊情况】如果调用方明确指定返回类型是Result<NoData>或Result<Unit>，发送默认值
                NoData::class -> emit(Result.success(NoData as T))
                Unit::class -> emit(Result.success(Unit as T))
                // 否则抛出MissingDataException
                else -> emit(Result.failure(MissingDataException()))
            }
            return@flow
        }

        // 请求成功，发送真实数据
        emit(Result.success(data))
        return@flow
    } catch (e: CancellationException) {
        // 重要：重新抛出[CancellationException]以保持协程取消机制
        // [CancellationException]是结构化并发的一部分，应该让它自然传播
        throw e
    } catch (e: Exception) {
        emit(Result.failure(e))
    }
}.flowOn(dispatcher)
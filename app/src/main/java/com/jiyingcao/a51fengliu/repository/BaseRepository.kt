package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.cancellation.CancellationException

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
    call: suspend () -> ApiResponse<T>
): Flow<Result<T?>> = flow {
    try {
        val response = call()
        when {
            // 在其他设备登录的异常处理，包含在else分支中，这里不再需要单独处理
            //response.code == 1003 -> emit(Result.failure(RemoteLoginException(1003, response.msg)))
            response.isSuccessful() -> emit(Result.success(response.data))
            else -> emit(Result.failure(ApiException.createFromResponse(response)))
        }
    } catch (e: CancellationException) {
        // 重要：重新抛出[CancellationException]以保持协程取消机制
        // [CancellationException]是结构化并发的一部分，应该让它自然传播
        throw e
    } catch (e: Exception) {
        emit(Result.failure(e))
    }
}.flowOn(dispatcher)
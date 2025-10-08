package com.jiyingcao.a51fengliu.repository

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.api.response.NoData
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import com.jiyingcao.a51fengliu.domain.exception.HttpEmptyResponseException
import com.jiyingcao.a51fengliu.domain.exception.MissingDataException
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
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
 *
 * ## 适用场景
 * 适用于标准的 `ApiResponse<T>` 接口，自动处理：
 * - HTTP 错误（非 2xx）
 * - 通用业务错误（如 code=1003 远程登录）
 * - data 为 null 的异常情况
 *
 * ## 不适用场景
 * **注意**：以下接口由于 `data` 字段多态性，**无法使用此函数**：
 *
 * 1. **登录接口** - [com.jiyingcao.a51fengliu.repository.UserRepository.login]
 *    - 需要从 [LoginData.Success] 中提取 token 字符串
 *    - 需要将 [LoginData.Error] 转换为 [com.jiyingcao.a51fengliu.domain.exception.LoginException]
 *
 * 2. **举报接口** - [com.jiyingcao.a51fengliu.repository.RecordRepository.report]
 *    - 需要将 [ReportData.Error] 转换为 [com.jiyingcao.a51fengliu.domain.exception.ReportException]
 *
 * 这些接口需要手动实现错误处理逻辑。**重要**：修改此函数时，需同步修改上述两个方法。
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

/**
 * 新版API调用封装，返回 [ApiResult]
 *
 * 相比 [apiCallStrict]，提供更明确的错误分类：
 * - [ApiResult.Success]: 业务成功
 * - [ApiResult.ApiError]: 业务失败（服务端返回错误码）
 * - [ApiResult.NetworkError]: 网络层面失败（IO异常、JSON解析异常、HTTP错误等）
 * - [ApiResult.UnknownError]: 其他未预期的异常
 *
 * ## 适用场景
 * 适用于标准的 `ApiResponse<T>` 接口，推荐所有新代码使用此函数。
 *
 * ## 不适用场景
 * 同 [apiCallStrict]，以下接口由于 `data` 字段多态性需要手动处理：
 * - 登录接口（LoginData多态）
 * - 举报接口（ReportData多态）
 *
 * @param dispatcher 协程调度器，默认为IO
 * @param call API调用函数
 * @return Flow<ApiResult<T>>
 */
inline fun <reified T : Any> apiCallResult(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    noinline call: suspend () -> Response<ApiResponse<T>>
): Flow<ApiResult<T>> = flow {
    try {
        val httpResponse = call()

        // HTTP错误（非2xx）
        if (!httpResponse.isSuccessful) {
            emit(ApiResult.NetworkError(HttpException(httpResponse)))
            return@flow
        }

        // HTTP响应体为空
        val apiResponse = httpResponse.body()
        if (apiResponse == null) {
            emit(ApiResult.NetworkError(HttpEmptyResponseException()))
            return@flow
        }

        // 业务失败（服务端返回错误码）
        if (!apiResponse.isSuccessful()) {
            emit(ApiResult.ApiError(
                code = apiResponse.code,
                message = apiResponse.msg ?: "Unknown API error"
            ))
            return@flow
        }

        // 业务成功但data为null
        val data = apiResponse.data
        if (data == null) {
            when (T::class) {
                // 特殊情况：NoData/Unit类型允许null
                NoData::class -> emit(ApiResult.Success(NoData as T))
                Unit::class -> emit(ApiResult.Success(Unit as T))
                // 其他类型data为null视为异常
                else -> emit(ApiResult.NetworkError(MissingDataException()))
            }
            return@flow
        }

        // 业务成功且有数据
        emit(ApiResult.Success(data))

    } catch (e: CancellationException) {
        // 重要：重新抛出CancellationException以保持协程取消机制
        throw e
    } catch (e: IOException) {
        // 网络异常（包括连接失败、超时等）
        emit(ApiResult.NetworkError(e))
    } catch (e: JsonParseException) {
        // Gson JSON解析异常
        emit(ApiResult.NetworkError(e))
    } catch (e: MalformedJsonException) {
        // Gson JSON格式错误
        emit(ApiResult.NetworkError(e))
    } catch (e: Exception) {
        // 其他未预期的异常
        emit(ApiResult.UnknownError(e))
    }
}.flowOn(dispatcher)
package com.jiyingcao.a51fengliu.repository

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.api.response.NoData
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
 * 标准API调用封装，返回 [ApiResult]
 *
 * 提供明确的错误分类：
 * - [ApiResult.Success]: 业务成功，包含数据
 * - [ApiResult.ApiError]: 业务失败（服务端返回错误码）
 * - [ApiResult.NetworkError]: 网络层面失败（IO异常、JSON解析异常、HTTP错误等）
 * - [ApiResult.UnknownError]: 其他未预期的异常
 *
 * ## 适用场景
 * 适用于标准的 `ApiResponse<T>` 接口，推荐所有新代码使用此函数。
 *
 * ## 不适用场景
 * 以下接口由于 `data` 字段多态性需要手动处理：
 * - **登录接口** ([UserRepository.login]): LoginData 可能是 Success(token) 或 Error(fieldErrors)
 * - **举报接口** ([RecordRepository.report]): ReportData 可能是 Success("") 或 Error(fieldErrors)
 *
 * 这些接口已在各自的 Repository 中实现专门的错误处理逻辑。
 *
 * ## 使用示例
 * ```kotlin
 * // Repository层
 * fun getProfile(): Flow<ApiResult<Profile>> = apiCall {
 *     apiService.getProfile()
 * }
 *
 * // ViewModel层
 * repository.getProfile().collect { result ->
 *     when (result) {
 *         is ApiResult.Success -> updateUiState(result.data)
 *         is ApiResult.ApiError -> showError(result.message)
 *         is ApiResult.NetworkError -> showNetworkError()
 *         is ApiResult.UnknownError -> showUnknownError()
 *     }
 * }
 * ```
 *
 * @param dispatcher 协程调度器，默认为IO
 * @param call API调用函数
 * @return Flow<ApiResult<T>>
 */
inline fun <reified T : Any> apiCall(
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
        // 注意：1003等全局错误码已由BusinessErrorInterceptor处理，这里只需返回ApiError
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
package com.jiyingcao.a51fengliu.repository

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.response.LoginData
import com.jiyingcao.a51fengliu.api.response.Profile
import com.jiyingcao.a51fengliu.domain.exception.HttpEmptyResponseException
import com.jiyingcao.a51fengliu.domain.exception.MissingDataException
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    /**
     * 登录
     *
     * ## 多态响应处理
     * 登录接口的 `data` 字段有两种情况：
     * - `code=0` + `LoginData.Success`: 返回 `ApiResult.Success(token)`
     * - `code=0` + `LoginData.Error`: 返回 `ApiResult.ApiError(code=0, message, data=errors)`
     * - `code!=0`: 返回 `ApiResult.ApiError(code, message)`
     *
     * ## 注意事项
     * - 字段级验证错误时，`code=0` 但会返回 `ApiError`，`data` 字段包含错误Map
     * - 调用方可通过检查 `ApiError.code == 0` 和 `data != null` 来识别字段错误
     *
     * @param username 用户名
     * @param password 密码
     * @return Flow<ApiResult<String>> 成功时包含token
     */
    fun login(username: String, password: String): Flow<ApiResult<String>> = flow {
        try {
            val httpResponse = apiService.postLogin(LoginRequest(username, password))

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

            // 业务失败（通用错误码，如1003远程登录）
            if (!apiResponse.isSuccessful()) {
                emit(ApiResult.ApiError(
                    code = apiResponse.code,
                    message = apiResponse.msg ?: "Unknown API error"
                ))
                return@flow
            }

            // data 不应该为 null（TypeAdapter 保证），此检查仅为 make compiler happy
            val loginData = apiResponse.data
            if (loginData == null) {
                emit(ApiResult.NetworkError(MissingDataException()))
                return@flow
            }

            // 业务成功（code=0），根据 LoginData 类型处理
            when (loginData) {
                is LoginData.Success -> {
                    // 真正的成功：返回token
                    emit(ApiResult.Success(loginData.token))
                }
                is LoginData.Error -> {
                    // code=0 但包含字段验证错误
                    // 用 ApiError 表示，data 字段包含错误信息
                    emit(ApiResult.ApiError(
                        code = apiResponse.code,  // code=0
                        message = apiResponse.msg ?: "Validation Error",
                        data = loginData.errors  // Map<String, String>
                    ))
                }
            }

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
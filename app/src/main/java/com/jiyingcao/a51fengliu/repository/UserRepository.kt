package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.LoginRequest
import com.jiyingcao.a51fengliu.api.response.LoginData
import com.jiyingcao.a51fengliu.api.response.Profile
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import com.jiyingcao.a51fengliu.domain.exception.HttpEmptyResponseException
import com.jiyingcao.a51fengliu.domain.exception.LoginException
import com.jiyingcao.a51fengliu.domain.exception.MissingDataException
import retrofit2.HttpException
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
     *
     * 注意：此方法**无法使用** [BaseRepository.apiCallStrict]，原因如下：
     * 1. 需要从 [LoginData.Success] 中提取 token 字符串返回
     * 2. 需要将 [LoginData.Error] 转换为 [LoginException] 以携带字段级错误信息
     *
     * **重要**：此方法的错误处理逻辑参考自 [BaseRepository.apiCallStrict]。
     * 如果未来修改 apiCallStrict 的逻辑（如增加新的错误处理），需要同步修改此方法。
     *
     * @param username 用户名
     * @param password 密码
     * @return Flow<Result<String>> 包含登录结果的结果流，成功时包含token
     */
    fun login(username: String, password: String): Flow<Result<String>> = flow {
        try {
            val httpResponse = apiService.postLogin(LoginRequest(username, password))
            if (!httpResponse.isSuccessful) {
                emit(Result.failure(HttpException(httpResponse)))
                return@flow
            }

            val apiResponse = httpResponse.body()
            if (apiResponse == null) {
                emit(Result.failure(HttpEmptyResponseException()))
                return@flow
            }

            // 优先检查通用错误码（如 1003 远程登录等）
            if (!apiResponse.isSuccessful()) {
                emit(Result.failure(ApiException.createFromResponse(apiResponse)))
                return@flow
            }

            // data 不应该为 null（TypeAdapter 保证），此检查仅为 make compiler happy
            val loginData = apiResponse.data
            if (loginData == null) {
                emit(Result.failure(MissingDataException()))
                return@flow
            }

            // 业务成功（code=0），根据 LoginData 类型处理
            when (loginData) {
                is LoginData.Success -> {
                    emit(Result.success(loginData.token))
                }
                is LoginData.Error -> {
                    // code=0 但 data 是 Error 类型，包含字段验证错误
                    emit(Result.failure(
                        LoginException(
                            code = apiResponse.code,
                            message = apiResponse.msg,
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
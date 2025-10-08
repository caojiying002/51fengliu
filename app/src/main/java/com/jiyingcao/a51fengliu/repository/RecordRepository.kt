package com.jiyingcao.a51fengliu.repository

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.request.ReportRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.api.response.ReportData
import com.jiyingcao.a51fengliu.domain.exception.HttpEmptyResponseException
import com.jiyingcao.a51fengliu.domain.exception.MissingDataException
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class RecordRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    /**
     * 获取记录列表，带分页功能
     * @param request 请求参数
     */
    fun getRecords(
        request: RecordsRequest
    ): Flow<Result<PageData<RecordInfo>?>> = apiCall {
        apiService.getRecords(request.toMap())
    }

    /**
     * 获取单条记录详情
     * @param id 记录ID
     * @return Flow<Result<RecordInfo>> 包含记录详情的结果流
     */
    fun getDetail(id: String): Flow<Result<RecordInfo?>> = apiCall {
        apiService.getDetail(id)
    }

    /**
     * 收藏
     * @param id 记录ID
     * @return Flow<Result<*>> 表示收藏成功或失败的结果流
     */
    fun favorite(id: String): Flow<Result<*>> = apiCall {
        apiService.postFavorite(InfoIdRequest(id))
    }

    /**
     * 取消收藏
     * @param id 记录ID
     * @return Flow<Result<*>> 表示取消收藏成功或失败的结果流
     */
    fun unfavorite(id: String): Flow<Result<*>> = apiCall {
        apiService.postUnfavorite(InfoIdRequest(id))
    }

    /**
     * 获取我的收藏列表
     * @param page 页码，默认从1开始
     * @return Flow<Result<PageData<RecordInfo>?>> 包含收藏列表的结果流
     */
    fun getFavorites(page: Int = 1): Flow<Result<PageData<RecordInfo>?>> = apiCall {
        apiService.getFavorites(page)
    }

    /**
     * 上传图片
     * @param file 要上传的文件
     * @return Flow<Result<String>> 包含上传后图片URL的结果流
     */
    fun uploadImage(file: File): Flow<Result<String?>> = apiCall {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        apiService.postUpload(body)
    }

    /**
     * 提交举报
     *
     * ## 多态响应处理
     * 举报接口的 `data` 字段有两种情况：
     * - `code=0` + `ReportData.Success`: 返回 `ApiResult.Success(Unit)`
     * - `code=0` + `ReportData.Error`: 返回 `ApiResult.ApiError(code=0, message, data=errors)`
     * - `code!=0`: 返回 `ApiResult.ApiError(code, message)`
     *
     * ## 注意事项
     * - 字段级验证错误时，`code=0` 但会返回 `ApiError`，`data` 字段包含错误Map
     * - 调用方可通过检查 `ApiError.code == 0` 和 `data != null` 来识别字段错误
     *
     * @param infoId Record ID
     * @param content 举报内容
     * @param picture 图片URL（相对路径，不包含BASE_URL）
     * @return Flow<ApiResult<Unit>> 成功时返回Unit
     */
    fun report(infoId: String, content: String, picture: String = ""): Flow<ApiResult<Unit>> = flow {
        try {
            val httpResponse = apiService.postReport(ReportRequest(infoId, content, picture))

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
            val reportData = apiResponse.data
            if (reportData == null) {
                emit(ApiResult.NetworkError(MissingDataException()))
                return@flow
            }

            // 业务成功（code=0），根据 ReportData 类型处理
            when (reportData) {
                is ReportData.Success -> {
                    // 真正的成功：返回Unit
                    emit(ApiResult.Success(Unit))
                }
                is ReportData.Error -> {
                    // code=0 但包含字段验证错误
                    // 用 ApiError 表示，data 字段包含错误信息
                    emit(ApiResult.ApiError(
                        code = apiResponse.code,  // code=0
                        message = apiResponse.msg ?: "Validation Error",
                        data = reportData.errors  // Map<String, String>
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
}
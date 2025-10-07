package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.request.ReportRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.api.response.ReportData
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import com.jiyingcao.a51fengliu.domain.exception.HttpEmptyResponseException
import com.jiyingcao.a51fengliu.domain.exception.MissingDataException
import com.jiyingcao.a51fengliu.domain.exception.ReportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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
     * 注意：此方法**无法使用** [BaseRepository.apiCallStrict]，原因如下：
     * 1. 需要将 [ReportData.Error] 转换为 [ReportException] 以携带字段级错误信息
     *
     * **重要**：此方法的错误处理逻辑参考自 [BaseRepository.apiCallStrict]。
     * 如果未来修改 apiCallStrict 的逻辑（如增加新的错误处理），需要同步修改此方法。
     *
     * @param infoId Record ID
     * @param content 举报内容
     * @param picture 图片URL（相对路径，不包含BASE_URL）
     * @return Flow<Result<*>> 表示举报成功或失败的结果流
     */
    fun report(infoId: String, content: String, picture: String = ""): Flow<Result<*>> = flow {
        try {
            val httpResponse = apiService.postReport(ReportRequest(infoId, content, picture))
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
            val reportData = apiResponse.data
            if (reportData == null) {
                emit(Result.failure(MissingDataException()))
                return@flow
            }

            // 业务成功（code=0），根据 ReportData 类型处理
            when (reportData) {
                is ReportData.Success -> {
                    emit(Result.success(Unit))
                }
                is ReportData.Error -> {
                    // code=0 但 data 是 Error 类型，包含字段验证错误
                    emit(Result.failure(
                        ReportException(
                            code = apiResponse.code,
                            message = apiResponse.msg,
                            errors = reportData.errors
                        )
                    ))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
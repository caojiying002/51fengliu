package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.request.ReportRequest
import com.jiyingcao.a51fengliu.api.response.ApiResult
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.domain.exception.ReportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
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
     * @param infoId Record ID
     * @param content 举报内容
     * @param picture 图片URL（相对路径，不包含BASE_URL）
     * @return Flow<Result<*>> 表示举报成功或失败的结果流
     */
    fun report(infoId: String, content: String, picture: String = ""): Flow<Result<*>> = flow {
        try {
            val response = apiService.postReport(ReportRequest(infoId, content, picture))
            when (val result = response.data) {
                is ApiResult.Success -> {
                    emit(Result.success(result.data))
                }
                is ApiResult.Error -> {
                    emit(Result.failure(
                        ReportException(
                            code = result.code,
                            message = result.msg,
                            errorData = result.errorData
                        )
                    ))
                }
                null -> {
                    emit(Result.failure(Exception("Empty response data")))
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
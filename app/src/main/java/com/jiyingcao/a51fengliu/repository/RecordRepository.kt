package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.request.ReportRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class RecordRepository(
    private val apiService: ApiService
) : BaseRepository() {

    /**
     * 获取记录列表，带分页功能
     * @param request 请求参数
     */
    fun getRecords(
        request: RecordsRequest
    ): Flow<Result<PageData?>> = apiCall {
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
     * @return Flow<Result<PageData?>> 包含收藏列表的结果流
     */
    fun getFavorites(page: Int = 1): Flow<Result<PageData?>> = apiCall {
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
    fun report(infoId: String, content: String, picture: String = ""): Flow<Result<*>> = apiCall {
        apiService.postReport(ReportRequest(infoId, content, picture))
    }

    companion object {
        // 用于单例模式实现
        @Volatile
        private var instance: RecordRepository? = null

        fun getInstance(apiService: ApiService): RecordRepository {
            return instance ?: synchronized(this) {
                instance ?: RecordRepository(apiService).also { instance = it }
            }
        }
    }
}
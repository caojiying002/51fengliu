package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.InfoIdRequest
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import kotlinx.coroutines.flow.Flow

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
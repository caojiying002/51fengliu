package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.domain.exception.BusinessException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class RecordRepository(
    private val apiService: ApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * 获取记录列表，带分页功能
     * @param request 请求参数
     */
    suspend fun getRecords(
        request: RecordsRequest
    ): Flow<Result<PageData>> = flow {
        try {
            val response = apiService.getRecords(request.toMap())
            if (response.isSuccessful()) {
                response.data?.let {
                    emit(Result.success(it))
                } ?: emit(Result.failure(Exception("Empty response data")))
            } else {
                emit(Result.failure(
                    BusinessException.createFromResponse(response) // TODO 有些失败响应data不为空，需要特殊处理
                ))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    /**
     * 获取单条记录详情
     * @param id 记录ID
     * @return Flow<Result<RecordInfo>> 包含记录详情的结果流
     */
    fun getDetail(id: String): Flow<Result<RecordInfo>> = flow {
        try {
            val response = apiService.getDetail(id)
            if (response.isSuccessful()) {
                response.data?.let {
                    emit(Result.success(it))
                } ?: emit(Result.failure(Exception("Empty response data")))
            } else {
                emit(Result.failure(
                    BusinessException.createFromResponse(response) // TODO 有些失败响应data不为空，需要特殊处理
                ))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)


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
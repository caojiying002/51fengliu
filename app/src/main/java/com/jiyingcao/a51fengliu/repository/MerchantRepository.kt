package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.api.response.PageData
import kotlinx.coroutines.flow.Flow

class MerchantRepository(
    private val apiService: ApiService
) : BaseRepository() {

    /**
     * 获取商家列表，带分页功能
     * @param page 页码，默认从1开始
     * @return Flow<Result<PageData<Merchant>?>> 包含商家列表的结果流
     */
    fun getMerchants(page: Int = 1): Flow<Result<PageData<Merchant>?>> = apiCall {
        apiService.getMerchants(page)
    }

    /**
     * 获取商家详情
     * @param id 商家ID
     * @return Flow<Result<Merchant?>> 包含商家详情的结果流
     */
    fun getMerchantDetail(id: String): Flow<Result<Merchant?>> = apiCall {
        apiService.getMerchantDetail(id)
    }

    companion object {
        // 用于单例模式实现
        @Volatile
        private var instance: MerchantRepository? = null

        fun getInstance(apiService: ApiService = RetrofitClient.apiService): MerchantRepository {
            return instance ?: synchronized(this) {
                instance ?: MerchantRepository(apiService).also { instance = it }
            }
        }
    }
}
package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    /**
     * 获取商家列表，带分页功能
     * @param page 页码，默认从1开始
     * @return Flow<ApiResult<PageData<Merchant>>> 包含商家列表的结果流
     */
    fun getMerchants(page: Int = 1): Flow<ApiResult<PageData<Merchant>>> = apiCallResult {
        apiService.getMerchants(page)
    }

    /**
     * 获取商家详情
     * @param id 商家ID
     * @return Flow<ApiResult<Merchant>> 包含商家详情的结果流
     */
    fun getMerchantDetail(id: String): Flow<ApiResult<Merchant>> = apiCallResult {
        apiService.getMerchantDetail(id)
    }
}
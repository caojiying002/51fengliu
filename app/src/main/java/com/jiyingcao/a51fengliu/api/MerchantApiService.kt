package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.TokenPolicy.Policy
import com.jiyingcao.a51fengliu.api.response.*
import com.jiyingcao.a51fengliu.util.City
import retrofit2.http.*

/**
 * 商家相关的API接口
 * 处理商家列表、详情和城市信息查询等操作
 */
interface MerchantApiService {

    /**
     * 获取商家列表（分页）
     * 获取所有商家的分页列表
     * 
     * @param page 页码，默认从1开始
     * @param perPage 每页数量，可选参数
     * @return 分页的商家列表
     */
    @TokenPolicy(Policy.OPTIONAL)
    @GET(ApiEndpoints.Merchant.PAGE)
    suspend fun getMerchants(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int? = null
    ): ApiResponse<PageData<Merchant>>

    /**
     * 获取商家详情
     * 获取指定商家的详细信息
     * 
     * @param id 商家ID
     * @return 商家详情数据
     */
    @TokenPolicy(Policy.OPTIONAL)
    @GET(ApiEndpoints.Merchant.DETAIL)
    suspend fun getMerchantDetail(
        @Query("merchantId") id: String
    ): ApiResponse<Merchant>

    /**
     * 获取商家城市列表
     * 获取所有有商家的城市列表
     * 
     * @return 城市列表
     */
    @TokenPolicy(Policy.OPTIONAL)
    @GET(ApiEndpoints.Merchant.CITIES)
    suspend fun getMerchantCities(): ApiResponse<List<City>>
}
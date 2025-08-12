package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.StreetIdRequest
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.Street
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreetRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    /**
     * 获取暗巷列表，带分页功能
     * @param cityCode 城市代码
     * @param sort 排序方式，默认为"publish"
     * @param page 页码，默认从1开始
     * @param perPage 每页数量，默认30
     * @return Flow<Result<PageData<Street>?>> 包含暗巷列表的结果流
     */
    fun getStreets(
        cityCode: String,
        sort: String = "publish",
        page: Int = 1,
        perPage: Int = 30
    ): Flow<Result<PageData<Street>?>> = apiCall {
        apiService.getStreets(cityCode, sort, page, perPage)
    }

    /**
     * 获取暗巷详情
     * @param streetId 暗巷ID
     * @return Flow<Result<Street?>> 包含暗巷详情的结果流
     */
    fun getStreetDetail(streetId: String): Flow<Result<Street?>> = apiCall {
        apiService.getStreetDetail(streetId)
    }

    /**
     * 收藏暗巷
     * @param streetId 暗巷ID
     * @return Flow<Result<*>> 表示收藏成功或失败的结果流
     */
    fun favoriteStreet(streetId: String): Flow<Result<*>> = apiCall {
        apiService.postStreetFavorite(StreetIdRequest(streetId))
    }

    /**
     * 取消收藏暗巷
     * @param streetId 暗巷ID
     * @return Flow<Result<*>> 表示取消收藏成功或失败的结果流
     */
    fun unfavoriteStreet(streetId: String): Flow<Result<*>> = apiCall {
        apiService.postStreetUnfavorite(StreetIdRequest(streetId))
    }

    /**
     * 获取我的暗巷收藏列表
     * @param page 页码，默认从1开始
     * @param perPage 每页数量，默认30
     * @return Flow<Result<PageData<Street>?>> 包含暗巷收藏列表的结果流
     */
    fun getFavoriteStreets(page: Int = 1, perPage: Int = 30): Flow<Result<PageData<Street>?>> = apiCall {
        apiService.getFavoriteStreets(page, perPage)
    }
}
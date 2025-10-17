package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.request.StreetIdRequest
import com.jiyingcao.a51fengliu.api.response.NoData
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.Street
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 暗巷数据仓库
 *
 * ## 关于数据去重
 * 使用 [PageDataDeduplicator] 对分页数据进行去重（包括同页去重和跨页去重）。
 * 详细说明参见 [MerchantRepository] 和 [PageDataDeduplicator] 的注释。
 */
@Singleton
class StreetRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    // 分页数据去重器
    private val streetDeduplicator = PageDataDeduplicator<Street> { it.id }
    private val favoriteDeduplicator = PageDataDeduplicator<Street> { it.id }

    /**
     * 获取暗巷列表，带分页功能和自动去重
     * @param cityCode 城市代码
     * @param sort 排序方式，默认为"publish"
     * @param page 页码，默认从1开始
     * @param perPage 每页数量，默认30
     * @return Flow<ApiResult<PageData<Street>>> 包含暗巷列表的结果流（已去重）
     */
    fun getStreets(
        cityCode: String,
        sort: String = "publish",
        page: Int = 1,
        perPage: Int = 30
    ): Flow<ApiResult<PageData<Street>>> =
        streetDeduplicator.deduplicate(
            flow = apiCall { apiService.getStreets(cityCode, sort, page, perPage) },
            page = page
        )

    /**
     * 获取暗巷详情
     * @param streetId 暗巷ID
     * @return Flow<ApiResult<Street>> 包含暗巷详情的结果流
     */
    fun getStreetDetail(streetId: String): Flow<ApiResult<Street>> = apiCall {
        apiService.getStreetDetail(streetId)
    }

    /**
     * 收藏暗巷
     * @param streetId 暗巷ID
     * @return Flow<ApiResult<NoData>> 表示收藏成功或失败的结果流
     */
    fun favoriteStreet(streetId: String): Flow<ApiResult<NoData>> = apiCall {
        apiService.postStreetFavorite(StreetIdRequest(streetId))
    }

    /**
     * 取消收藏暗巷
     * @param streetId 暗巷ID
     * @return Flow<ApiResult<NoData>> 表示取消收藏成功或失败的结果流
     */
    fun unfavoriteStreet(streetId: String): Flow<ApiResult<NoData>> = apiCall {
        apiService.postStreetUnfavorite(StreetIdRequest(streetId))
    }

    /**
     * 获取我的暗巷收藏列表，带自动去重
     * @param page 页码，默认从1开始
     * @param perPage 每页数量，默认30
     * @return Flow<ApiResult<PageData<Street>>> 包含暗巷收藏列表的结果流（已去重）
     */
    fun getFavoriteStreets(page: Int = 1, perPage: Int = 30): Flow<ApiResult<PageData<Street>>> =
        favoriteDeduplicator.deduplicate(
            flow = apiCall { apiService.getFavoriteStreets(page, perPage) },
            page = page
        )
}
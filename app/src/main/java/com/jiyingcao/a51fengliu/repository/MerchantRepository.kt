package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 商家数据仓库
 *
 * ## 关于数据去重
 *
 * 使用 [PageDataDeduplicator] 进行跨页去重，解决后端可能返回重复数据的问题。
 *
 * ### 架构说明
 *
 * 1. **问题背景**：后端接口可能返回跨页重复数据（如第二页包含第一页的 ID），
 *    导致 Compose LazyColumn 使用 ID 作为 key 时崩溃
 * 2. **当前方案**：使用 [PageDataDeduplicator] 在 Repository 层进行去重
 *    - 封装了去重逻辑和状态管理，避免代码重复
 *    - 对上层透明，所有调用方自动受益
 *    - 防御性编程，即使后端有 bug 也不会崩溃
 * 3. **架构权衡**：虽然违背了"Repository 应该无状态"的原则，但：
 *    - 引入 UseCase 层对中小型项目投入产出比不划算
 *    - 在 ViewModel 层去重依赖调用方"记得"处理，容易遗漏
 *    - 影响范围可控（状态封装在 deduplicator 内部）
 *
 * ### 未来迁移路径
 *
 * 当项目规模增长、业务逻辑复杂度增加时，可以：
 * 1. 创建 `GetMerchantsUseCase` 接管去重逻辑
 * 2. Repository 恢复为纯数据获取层（移除 deduplicator）
 * 3. ViewModel 切换到 UseCase 注入
 */
@Singleton
class MerchantRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    // 分页数据去重器
    private val deduplicator = PageDataDeduplicator<Merchant> { it.id }

    /**
     * 获取商家列表，带分页功能和自动去重
     *
     * @param page 页码，默认从1开始
     * @return Flow<ApiResult<PageData<Merchant>>> 包含商家列表的结果流（已去重）
     */
    fun getMerchants(page: Int = 1): Flow<ApiResult<PageData<Merchant>>> =
        deduplicator.deduplicate(
            flow = apiCall { apiService.getMerchants(page) },
            page = page
        )

    /**
     * 获取商家详情
     * @param id 商家ID
     * @return Flow<ApiResult<Merchant>> 包含商家详情的结果流
     */
    fun getMerchantDetail(id: String): Flow<ApiResult<Merchant>> = apiCall {
        apiService.getMerchantDetail(id)
    }
}
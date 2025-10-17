package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/**
 * 分页数据去重器
 *
 * ## 用途
 * 解决后端接口可能返回跨页重复数据的问题，避免 Compose LazyColumn 使用 ID 作为 key 时崩溃。
 *
 * ## 设计说明
 * - 维护已加载 ID 集合，进行跨页去重
 * - 第一页（page=1）时自动清空缓存
 * - 使用 `transform` 操作符而非 `map`，语义更清晰
 * - 异常会自然传播到上层处理，无需内部 try-catch
 *
 * ## 使用示例
 * ```kotlin
 * @Singleton
 * class MerchantRepository @Inject constructor(
 *     private val apiService: ApiService
 * ) : BaseRepository() {
 *     private val deduplicator = PageDataDeduplicator<Merchant> { it.id }
 *
 *     fun getMerchants(page: Int = 1): Flow<ApiResult<PageData<Merchant>>> =
 *         deduplicator.deduplicate(
 *             flow = apiCall { apiService.getMerchants(page) },
 *             page = page
 *         )
 * }
 * ```
 *
 * @param T 数据项类型
 * @param idSelector 从数据项提取唯一标识的函数，例如 `{ it.id }` 或 `{ merchant -> merchant.id }`
 */
class PageDataDeduplicator<T>(
    private val idSelector: (T) -> String
) {
    // 维护已加载的 ID 集合
    private val loadedIds = mutableSetOf<String>()

    /**
     * 对 Flow 应用去重逻辑
     *
     * @param flow 原始的 API 调用 Flow
     * @param page 当前页码，page=1 时会清空已加载 ID 缓存
     * @return 去重后的 Flow
     */
    fun deduplicate(
        flow: Flow<ApiResult<PageData<T>>>,
        page: Int
    ): Flow<ApiResult<PageData<T>>> = flow.transform { result ->
        when (result) {
            is ApiResult.Success -> {
                // 第一页时清空缓存（下拉刷新场景）
                if (page == 1) {
                    loadedIds.clear()
                }

                // 过滤已加载的数据
                // add() 返回 true 表示之前不存在（新数据），返回 false 表示已存在（重复数据）
                val dedupedRecords = result.data.records.filter { item ->
                    loadedIds.add(idSelector(item))
                }

                // emit 去重后的结果
                emit(result.copy(
                    data = result.data.copy(records = dedupedRecords)
                ))
            }
            // 其他结果类型（错误）直接透传
            else -> emit(result)
        }
    }

    /**
     * 手动清空已加载 ID 缓存
     *
     * 通常不需要手动调用，因为 page=1 时会自动清空。
     * 此方法用于特殊场景，如数据源完全变更时。
     */
    fun clear() {
        loadedIds.clear()
    }
}

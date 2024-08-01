package com.jiyingcao.a51fengliu.api.response

import android.os.Parcelable
import com.jiyingcao.a51fengliu.api.SubUrl
import kotlinx.parcelize.Parcelize

/** A list of [ItemData]. 简化使用泛型时的引用 */
typealias ItemDataList = List<ItemData>

/**
 * A wrapper class for wrapping paged data.
 * 一个包装类，用于包装分页数据
 *
 * @param data 数据列表
 * @param page 当前页码
 */
@Deprecated("51风流不需要自己包装页码")
open class PagedItemData (
    open val data: List<ItemData> = emptyList(),
    open val page: Int = 1,
)
fun PagedItemData.isEmpty() = data.isEmpty()

/**
 * A wrapper class for wrapping paged search result data.
 * 一个包装类，用于包装分页搜索结果数据
 *
 * @param keywords 搜索关键词
 * @param data 数据列表
 * @param page 当前页码
 */
class SearchItemData(
    override val data: List<ItemData> = emptyList(),
    override val page: Int = 1,
    val keywords: String = "",
) : PagedItemData(data, page)

@Parcelize
data class ItemData(
    val id: Int,
    val title: String,
    val age: String,
    val price: String,
    val project: String,
    val process: String,
    val qq: String,
    val wechat: String,  // TODO: 2024/3/13 13:05 有可能为空
    val phone: String,  // TODO: 2024/6/05 14:39 有可能为空
    val address: String,
    val dz: String,
    val pid: String,
    val cid: String,
    val privacy: String,
    val status: String,
    val reason: String,
    val browse: String,
    val create_time: String,
    val earn_points: String,
    val is_ad: String,
    val author: String,

    // TODO 直接获取完整URL

    /**
     * e.g. "/uploads/picture2/24c6017407bf9f76db7affb36a738e84.jpg,/uploads/picture2/4661f2d51c83dbac1114cc410d6d0164.jpg,/uploads/picture2/63c926d271fe4857f47c87c74818da17.jpg"
     */
    val file: SubUrl,

    /**
     * e.g. "/uploads/thumb2/24c6017407bf9f76db7affb36a738e84.jpg"
     */
    val img: SubUrl,
    val face_value: String,
    val view_points: String,
    val viewlist: String,
) : Parcelable {
    val isAd: Boolean
        get() = ("0" == is_ad)
    val fileList: List<String>
        get() = file.split(',')
}
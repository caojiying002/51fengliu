package com.jiyingcao.a51fengliu.api.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 关于字段能否为空：
 * 1. 有些字段在详情页有，列表页没有，所以在Record中是可空的
 * 2. 有些字段在未登录态下不可见，所以在Record中是可空的
 * 3. 目前只假定id、title不可空（返回null会crash）
 */
@Parcelize
data class Record(  // TODO 重命名为RecordItem，避免和java.lang.Record重名
    val id: String, // Int,
    val userId: String?, // Int,
    val status: String?, //Int,
    val type: String?, //Int,
    val title: String,
    val isRecommend: Boolean?,
    val isMerchant: Boolean?,
    val isExpired: Boolean?,
    val source: String?, //Int,
    val score: String?, //Int,
    val viewCount: String?, //Int,
    val cityCode: String?, //Int,
    val girlNum: String?,
    val girlAge: String?,
    val girlBeauty: String?,
    val environment: String?,
    val consumeLv: String?,
    val consumeAllNight: String?,
    val serveList: String?,
    val serveLv: String?,
    /** 列表页比详情页更简短 */ val desc: String?,
    val picture: String?,
    val coverPicture: String?,
    val anonymous: Boolean?,
    val publishedAt: String?, //Long,
    val createdAt: String?, //Long,
    /** 列表页为null，详情页为true/false */ val isFavorite: Boolean?,
    /** 列表页为null，详情页为1（未登录态） */ val vipProfileStatus: Int?,
    val publisher: String?,
    val userName: String?,
    val userReputation: String?,
    val userStatus: String?,
    val style: String?,
    val guestView: String?,

    // 列表页无，详情页有
    val userView: String?,

    // 登录后可见（列表页无，详情页有）
    val qq: String?,
    val wechat: String?,
    val telegram: String?,
    val yuni: String?,
    val phone: String?,
    val address: String?,
) : Parcelable

fun Record.getPictures(): List<String> {
    return picture?.split(",") ?: emptyList()
}
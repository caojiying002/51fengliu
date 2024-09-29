package com.jiyingcao.a51fengliu.api.response

/**
 * 关于字段能否为空：
 * 1. 有些字段在详情页有，列表页没有，所以在Record中是可空的
 * 2. 有些字段在未登录态下不可见，所以在Record中是可空的
 * 3. 目前只假定id、title不可空（返回null会crash）
 */
data class RecordInfo(  // 2024.09.13重命名为RecordInfo，避免和java.lang.Record重名
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
    val publisher: Publisher?,
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
) {
    @Deprecated(message = "use getPictures() instead", replaceWith = ReplaceWith("getPictures()"))
    fun getPicturesDeprecated(): List<String> {
        return if (picture.isNullOrBlank()) {
            emptyList()
        } else {
            picture.split(",")
        }
    }
    fun getPictures(): List<String> =
        picture?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
}

/*
"publisher": {
    "id": 1294831,
    "name": "369258111",
    "email": "1379065592@qq.com",
    "status": 0,
    "isAdmin": false,
    "isMuted": false,
    "reputation": 0,
    "agentId": null,
    "score": 250,
    "canAgent": false,
    "agentRate": 0,
    "source": 1,
    "infoPrivate": true,
    "comment": null,
    "sign": null,
    "certPics": null,
    "infoPassCount": 1,
    "infoRefuseCount": 0,
    "expiredAt": null,
    "forbiddenAt": null,
    "lastLogin": 1725977385000,
    "createdAt": 1725977385000,
    "agentName": null,
    "userType": 0,
    "isFrozen": false,
    "isForbidden": false,
    "isVip": false,
    "publishedInfoCount": 1,
    "refusedInfoCount": 0
}
 */
data class Publisher(
    val id: String,
    val name: String,

    /* 剩下的用不到不解析了，字段越多解析失败的可能性就越大 */
    /*
    val email: String,
    val status: String,
    val isAdmin: Boolean,
    val isMuted: Boolean,
    val reputation: String,
    val agentId: String?,
    val score: String,
    val canAgent: Boolean,
    val agentRate: String,
    val source: String,
    val infoPrivate: Boolean,
    val comment: String?,
    val sign: String?,
    val certPics: String?,
    val infoPassCount: String,
    val infoRefuseCount: String,
    val expiredAt: String?,
    val forbiddenAt: String?,
    val lastLogin: String,
    val createdAt: String,
    val agentName: String?,
    val userType: String,
    val isFrozen: Boolean,
    val isForbidden: Boolean,
    val isVip: Boolean,
    val publishedInfoCount: String,
    val refusedInfoCount: String,
     */
)
package com.jiyingcao.a51fengliu.api.response

/**
 * {
 *     "code": 0,
 *     "msg": "Ok",
 *     "data": {
 *         "id": 1248413,
 *         "name": "jiyingcao",
 *         "email": "jiyingcao2020@outlook.com",
 *         "status": 0,
 *         "isAdmin": false,
 *         "isMuted": false,
 *         "reputation": 0,
 *         "agentId": null,
 *         "score": 0,
 *         "canAgent": false,
 *         "agentRate": 0,
 *         "source": 1,
 *         "infoPrivate": true,
 *         "comment": null,
 *         "sign": null,
 *         "certPics": null,
 *         "infoPassCount": 0,
 *         "infoRefuseCount": 0,
 *         "expiredAt": null,
 *         "forbiddenAt": null,
 *         "lastLogin": 1721524824000,
 *         "createdAt": 1721523046000,
 *         "agentName": null,
 *         "isFrozen": false,
 *         "isForbidden": false,
 *         "userType": 0,
 *         "isVip": false,
 *         "publishedInfoCount": 0,
 *         "refusedInfoCount": 0
 *     }
 * }
 */
data class Profile(
    val id: String,
    val name: String,
    val email: String,
    val status: Int,
    val isAdmin: Boolean,
    val isMuted: Boolean,
    val reputation: Int,
    val agentId: Any?,
    val score: String?,  // Int
    val canAgent: Boolean,
    val agentRate: Int,
    val source: Int,
    val infoPrivate: Boolean,
    val comment: Any?,
    val sign: Any?,
    val certPics: Any?,
    val infoPassCount: String?, // Int
    val infoRefuseCount: String?, // Int

    /** 会员过期时间。永久会员的值是“2147443200000”，也就是2038-01-19 00:00:00 */
    val expiredAt: String?, // Long
    val forbiddenAt: String?, // Long
    val lastLogin: String?, // Long
    val createdAt: String?, // Long
    val agentName: Any?,
    val isFrozen: Boolean,
    val isForbidden: Boolean,
    val userType: Int,
    val isVip: Boolean?,
    val publishedInfoCount: String?, // Int
    val refusedInfoCount: String? // Int
)

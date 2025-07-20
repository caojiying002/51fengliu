package com.jiyingcao.a51fengliu.api.response

/**
 * {
 *   "code": 0,
 *   "msg": "Ok",
 *   "data": {
 *     "records": [
 *       {
 *         "id": 352,
 *         "userId": 1456534,
 *         "status": 2,
 *         "cityCode": 110105,
 *         "title": "北京扫街",
 *         "desc": null,
 *         "picture": null,
 *         "coverPicture": "250528/3238b77d-81a8-4607-b8f7-cc978e86ce5e_t.jpg",
 *         "score": null,
 *         "viewCount": 10335,
 *         "publishedAt": 1748442267000,
 *         "createdAt": null,
 *         "isFavorite": null,
 *         "userName": null,
 *         "userReputation": null,
 *         "userStatus": null
 *       },
 *       {
 *         "id": 173,
 *         "userId": 1225715,
 *         "status": 2,
 *         "cityCode": 110105,
 *         "title": "北京扫街攻略",
 *         "desc": null,
 *         "picture": null,
 *         "coverPicture": "250514/0cdd12f2-05e6-415f-b111-6a98ab08f1ab_t.jpg",
 *         "score": null,
 *         "viewCount": 9350,
 *         "publishedAt": 1747216201000,
 *         "createdAt": null,
 *         "isFavorite": null,
 *         "userName": null,
 *         "userReputation": null,
 *         "userStatus": null
 *       }
 *     ],
 *     "total": 2,
 *     "size": 30,
 *     "current": 1,
 *     "orders": [],
 *     "optimizeCountSql": true,
 *     "searchCount": true,
 *     "countId": null,
 *     "maxLimit": null,
 *     "pages": 1
 *   }
 * }
 */

/**
 * {
 *   "code": 0,
 *   "msg": "Ok",
 *   "data": {
 *     "id": 330,
 *     "userId": 1468305,
 *     "status": 2,
 *     "cityCode": 110101,
 *     "title": "北京 扫街",
 *     "desc": "北京导航益民一巷，北边和南边的巷子",
 *     "picture": "250525/e193af17-ca83-48ae-9ce5-6493563f9ab0.jpeg,250525/6157383d-e7bd-4da5-b07f-a86a398a247a.jpeg",
 *     "coverPicture": "250528/a088ab91-9e4f-4cc7-be12-3e9aba5a2ace_t.jpg",
 *     "score": 300,
 *     "viewCount": 9288,
 *     "publishedAt": 1748370077000,
 *     "createdAt": 1748105602000,
 *     "isFavorite": true,
 *     "userName": null,
 *     "userReputation": null,
 *     "userStatus": null
 *   }
 * }
 */
data class Street(
    /* 假定id/title/cityCode字段不会为null */
    val id: String, // Int,
    val title: String,
    val cityCode: String,
    
    /* 其余字段都按照可空类型处理 */
    val userId: String?, // Int,
    val status: String?, // Int, (VIP登录态是2, 其他情况待定) 
    val desc: String?, // 列表页null/详情页有值
    val picture: String?,   // 列表页null/详情页按逗号拆分为集合
    val coverPicture: String?,  // 列表页详情页都有值
    val score: String?, // Int, 列表页null/详情页有值
    val viewCount: String?, // Int, 浏览次数
    val publishedAt: String? = null, // 发布时间, 列表页详情页都有值
    val createdAt: String? = null,   // 创建时间, 列表页null/详情页有值
    val isFavorite: String? = null,   // Boolean, 是否收藏, 列表页null/详情页根据登录状态

    /* 一些总是为null的字段暂不解析, 降低出错可能 */
    /*val userName: String? = null,
    val userReputation: String? = null,
    val userStatus: String? = null,*/
) {
    fun getPictures(): List<String> =
        picture?.split(',')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
}
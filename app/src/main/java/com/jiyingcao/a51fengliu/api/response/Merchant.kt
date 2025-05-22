package com.jiyingcao.a51fengliu.api.response

/**
 * {
 *                 "id": 55,
 *                 "name": "厦门可选不限次数",
 *                 "cityCode": 350000,
 *                 "showLv": null,
 *                 "price": null,
 *                 "viewCount": null,
 *                 "picture": null,
 *                 "coverPicture": "250518/5500aba0-d2d0-455d-9904-609f72ca7246_t.jpg",
 *                 "comment": null,
 *                 "intro": "无套路、不办卡、没有任何隐形消费！",
 *                 "desc": null,
 *                 "validStart": null,
 *                 "validEnd": null,
 *                 "vipProfileStatus": null,
 *                 "style": "merchant",
 *                 "status": 2
 *             }
 */

/**
 * {
 *     "code": 0,
 *     "msg": "Ok",
 *     "data": {
 *         "id": 55,
 *         "name": "厦门可选不限次数",
 *         "cityCode": 350000,
 *         "showLv": 3,
 *         "price": null,
 *         "viewCount": null,
 *         "picture": "250518/efba2c11-208e-45bb-a2f8-ed1c2be3389d.jpg,250518/59486cd1-1556-4255-a477-ced3c55a9423.jpg",
 *         "coverPicture": null,
 *         "comment": null,
 *         "intro": "无套路、不办卡、没有任何隐形消费！",
 *         "desc": "主要口碑和回头客  以服务为宗旨，\n店里明码标价无任何的隐形消费， \n诚信经营服务没得挑剔。各种挑逗  各\n种诱惑  各种制服。让人很痴迷  颜值在线 每次去都是满载而归  走进去简直看不完  \n\n全程干磨服务\n\n\n为回馈新老客户  推出大型优惠活动    \n\n现价/598\n\n 主打高颜值    性感风骚   \n \n大胸 蜜桃臀 筷子腿 A4腰 各种高颜值嫩菜 \n\n制定角色，制定制服，满足您的所有幻想（免费服装丝袜搭配）             \n   制服 角色 丝足 玉女 胸滑 情趣 空姐 白领 少妇  女仆  学生  护士  女王       看照片非常的真实，地方离得近，\n看的我眼花缭乱的 妹妹们太多太漂亮  我都看不过来  ，娇娇欲滴的感觉，讲话说起话来也很温柔，大家值得来尝试一下，很多情趣内衣，服务真的是做的一流，特别的舒服，来了之后就会让你欲仙欲死流连忘返，有想法的大哥都可以来体验一下，我刚体验完，确实不错，\n\n服务时间60分钟出水不限次数 \n\n大爽了 我要了二次 时间不够 不然我还得来一次 而且人长的很漂亮特别有气质 是个不错的地方 \n约 友、请客有面，这里玩的嗨、玩的刺激、让您体验前所未有的感觉，不来是你的损失，全程接待陪护，直至选到满意为止‼️\n\n无套路、不办卡、没有任何隐形消费！ 先体验后买单，用心做口碑‼️👍👍👍   \n   门店环境 楼下就是停车场 每个房间都有独立的洗浴 所有洗浴用品都是一次性  一课一换\n",
 *         "validStart": 1736309837000,
 *         "validEnd": 1750220237000,
 *         "vipProfileStatus": 2,
 *         "style": "merchant",
 *         "status": 1
 *     }
 * }
 */
data class Merchant(
    /* 假定id/name/cityCode字段不会为null */
    val id: String, // Int,
    val name: String,
    val cityCode: String,
    
    /* 其余字段都按照可空类型处理 */
    val showLv: String?, // Int, (与是否登录/是否VIP无关) 列表页null/详情页3
    val picture: String?,   // 列表页null/详情页按逗号拆分为集合
    val coverPicture: String?,  // 列表页有值/详情页null
    val intro: String?,  // 一般不为空
    val desc: String?,  // 列表页null/详情页长文字
    val validStart: String? = null, // 时间戳, 列表页null/详情页有值
    val validEnd: String? = null,   // 时间戳, 列表页null/详情页有值
    val vipProfileStatus: String? = null,   // Int, 列表页null, 详情页未登录2/普通会员3/VIP用户1
    val style: String?,  // 常量"merchant"
    val status: String?, // Int, (与是否登录/是否VIP无关) 列表页2/详情页1

    /* 一些总是为null的字段暂不解析, 降低出错可能 */
    /*val price: String? = null,
    val viewCount: String? = null,
    val comment: String? = null,*/

    /* 以下字段仅VIP用户(或积分购买)可见 */
    val contact: String?,   // "电话 17685112251\n\n微信vm200922\n\nQ 3972462031"
)

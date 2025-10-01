package com.jiyingcao.a51fengliu.api.response

/**
 * APP弹窗通知数据模型
 * 用于APP启动时显示促销等信息的弹窗
 *
 * 响应示例:
 * {
 *   "code": 0,
 *   "msg": "Ok",
 *   "data": {
 *     "enable": true,
 *     "title": "国庆中秋双节促销",
 *     "content": "国庆中秋双节特别促销！\n限时开放永久会员,\n年度会员只要399元！\n永久会员只需再加299元！\n手快有，手慢无！\n同时在此祝各位会员节日快乐！",
 *     "period": 14400
 *   }
 * }
 *
 * @property enable 是否启用弹窗 - Boolean
 * @property title 弹窗标题
 * @property content 弹窗内容，支持\n换行
 * @property period 拉取节流周期（秒）- Int，至少每 period 秒才会再次请求一次接口
 */
data class AppPopupNotice(
    val enable: Boolean? = null,
    val title: String? = null,
    val content: String? = null,
    val period: String? = null  // Int
)
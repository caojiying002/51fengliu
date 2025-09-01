package com.jiyingcao.a51fengliu.api.response

/**
 * 语义化“无数据”占位类型。
 * 部分接口（例如收藏/取消收藏、登出等）虽然返回形如 {"code":0,"msg":"Ok","data":""}，
 * 但 data 字段没有业务含义。使用该单例相比 Kotlin 的 `Nothing` 更符合语义，也便于统一反序列化。\n
 * Gson 通过自定义的 TypeAdapter （注册在 `NetworkModule.provideGson()` 中）实现以下映射：
 *  - ""（空字符串） -> NoData
 *  - null -> NoData
 *  - {}（空对象） -> NoData
 *  - 其它任何意外内容 -> 打印警告日志后仍返回 NoData（“宽容 + 记录”策略）
 */
object NoData
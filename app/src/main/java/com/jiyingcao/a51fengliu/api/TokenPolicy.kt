package com.jiyingcao.a51fengliu.api

/**
 * 标注接口对于 token 的要求策略
 *
 * @property REQUIRED 必须携带 token
 * @property FORBIDDEN 禁止携带 token（例如登录接口）
 * @property OPTIONAL 可选携带 token（例如商品详情，携带则返回用户收藏状态）
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TokenPolicy(
    val value: Policy
) {
    enum class Policy {
        REQUIRED,    // 必须携带
        FORBIDDEN,   // 禁止携带
        OPTIONAL     // 可选携带
    }
}
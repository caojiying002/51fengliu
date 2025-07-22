package com.jiyingcao.a51fengliu.ui.compose.navigation

/**
 * Compose 页面路由定义
 * 统一管理所有 Compose 页面的路由常量
 *
 * 使用方式:
 * - navController.navigate(ComposeDestinations.MERCHANT_DETAIL)
 * - NavHost startDestination = ComposeDestinations.MERCHANT_DETAIL
 */
object ComposeDestinations {

    /**
     * 商户详情页
     */
    const val MERCHANT_DETAIL = "merchant_detail"

    /**
     * 个人资料页
     */
    const val PROFILE = "profile"

    /**
     * 设置页
     */
    const val SETTINGS = "settings"

    /**
     * 登录页
     */
    const val LOGIN = "login"

    /**
     * 注册页
     */
    const val REGISTER = "register"

    /**
     * VIP升级页
     */
    const val VIP_UPGRADE = "vip_upgrade"

    // 可以继续添加其他页面路由...

    /**
     * 获取所有路由列表 - 用于调试和统计
     */
    fun getAllRoutes(): List<String> = listOf(
        MERCHANT_DETAIL,
        PROFILE,
        SETTINGS,
        LOGIN,
        REGISTER,
        VIP_UPGRADE
    )

    /**
     * 检查路由是否有效
     */
    fun isValidRoute(route: String): Boolean {
        return route in getAllRoutes()
    }
}
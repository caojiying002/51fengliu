package com.jiyingcao.a51fengliu.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jiyingcao.a51fengliu.ui.MainScreen
import com.jiyingcao.a51fengliu.ui.compose.screens.MerchantDetailScreen

/**
 * 主界面相关的导航图，集中管理 ComposeMainActivity 的路由。
 */
@Composable
fun MainNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = MainDestinations.MAIN
    ) {
        // 首页容器
        composable(MainDestinations.MAIN) {
            MainScreen(
                onNavigate = { destination ->
                    navController.navigate(destination)
                }
            )
        }

        // 商家详情页（带参数）
        composable(
            route = MainDestinations.MERCHANT_DETAIL_WITH_ID,
            arguments = listOf(
                navArgument(MainDestinations.ARG_MERCHANT_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val merchantId = backStackEntry
                .arguments
                ?.getString(MainDestinations.ARG_MERCHANT_ID)
                .orEmpty()
            MerchantDetailScreen(
                merchantId = merchantId,
                onBackClick = { navController.popBackStack() },
                onNavigate = { destination -> navController.navigate(destination) }
            )
        }
    }
}

/**
 * 主导航相关路由常量与辅助函数。
 */
object MainDestinations {
    const val MAIN = "main"
    const val ARG_MERCHANT_ID = "merchantId"
    const val MERCHANT_DETAIL_WITH_ID = "${ComposeDestinations.MERCHANT_DETAIL}/{$ARG_MERCHANT_ID}"

    fun merchantDetail(merchantId: String): String =
        "${ComposeDestinations.MERCHANT_DETAIL}/$merchantId"
}


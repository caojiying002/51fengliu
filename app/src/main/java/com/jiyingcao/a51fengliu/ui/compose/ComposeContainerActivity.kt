package com.jiyingcao.a51fengliu.ui.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.compose.navigation.ComposeDestinations
import com.jiyingcao.a51fengliu.ui.compose.screens.MerchantDetailScreen
import com.jiyingcao.a51fengliu.ui.compose.screens.FavoriteScreen
import com.jiyingcao.a51fengliu.ui.compose.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 统一的 Compose 容器 Activity
 * 管理所有 Compose 页面的导航和生命周期
 *
 * 使用方式:
 * - ComposeContainerActivity.startMerchantDetail(context, merchantId)
 * - ComposeContainerActivity.startProfile(context)
 */
@AndroidEntryPoint
class ComposeContainerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 获取启动参数
        val initialRoute = intent.getStringExtra(KEY_INITIAL_ROUTE)
            ?: ComposeDestinations.MERCHANT_DETAIL
        val merchantId = intent.getStringExtra(KEY_MERCHANT_ID) ?: ""

        setContent {
            AppTheme {
                ComposeNavigationHost(
                    initialRoute = initialRoute,
                    merchantId = merchantId,
                    onFinish = { finish() }
                )
            }
        }
    }

    companion object {
        private const val TAG = "ComposeContainerActivity"
        private const val KEY_INITIAL_ROUTE = "initial_route"
        private const val KEY_MERCHANT_ID = "merchant_id"

        /**
         * 启动商户详情页
         * @param context 上下文
         * @param merchantId 商户ID
         */
        @JvmStatic
        fun startMerchantDetail(context: Context, merchantId: String) {
            context.startActivity(createMerchantDetailIntent(context, merchantId))
        }

        /**
         * 启动收藏列表页
         * @param context 上下文
         */
        @JvmStatic
        fun startFavorite(context: Context) {
            context.startActivity(createFavoriteIntent(context))
        }

        /**
         * 启动个人资料页
         * @param context 上下文
         */
        @JvmStatic
        fun startProfile(context: Context) {
            val intent = Intent(context, ComposeContainerActivity::class.java).apply {
                putExtra(KEY_INITIAL_ROUTE, ComposeDestinations.PROFILE)
            }
            context.startActivity(intent)
        }

        /**
         * 创建商户详情页Intent - 用于需要自定义启动的场景
         */
        @JvmStatic
        fun createMerchantDetailIntent(context: Context, merchantId: String): Intent {
            return Intent(context, ComposeContainerActivity::class.java).apply {
                putExtra(KEY_INITIAL_ROUTE, ComposeDestinations.MERCHANT_DETAIL)
                putExtra(KEY_MERCHANT_ID, merchantId)
            }
        }

        /**
         * 创建收藏列表页Intent - 用于需要自定义启动的场景
         */
        @JvmStatic
        fun createFavoriteIntent(context: Context): Intent {
            return Intent(context, ComposeContainerActivity::class.java).apply {
                putExtra(KEY_INITIAL_ROUTE, ComposeDestinations.FAVORITE)
            }
        }
    }
}

/**
 * Compose 导航宿主
 * 负责页面间的导航管理
 */
@Composable
private fun ComposeNavigationHost(
    initialRoute: String,
    merchantId: String,
    onFinish: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = initialRoute
    ) {
        // 商户详情页路由
        composable(ComposeDestinations.MERCHANT_DETAIL) {
            MerchantDetailScreen(
                merchantId = merchantId,
                onBackClick = onFinish,
                onNavigate = { destination ->
                    navController.navigate(destination)
                }
            )
        }

        // 收藏列表页路由
        composable(ComposeDestinations.FAVORITE) {
            FavoriteScreen(
                onBackClick = onFinish,
                onNavigate = { destination ->
                    navController.navigate(destination)
                },
                viewModel = hiltViewModel()
            )
        }

        // 个人资料页路由 - 预留
        composable(ComposeDestinations.PROFILE) {
            // TODO: 实现 ProfileScreen
            // ProfileScreen(
            //     onBackClick = onFinish,
            //     onNavigate = { destination ->
            //         navController.navigate(destination)
            //     }
            // )
        }

        // 设置页路由 - 预留
        composable(ComposeDestinations.SETTINGS) {
            // TODO: 实现 SettingsScreen
        }
    }
}
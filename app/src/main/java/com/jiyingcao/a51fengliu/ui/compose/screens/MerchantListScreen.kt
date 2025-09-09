package com.jiyingcao.a51fengliu.ui.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jiyingcao.a51fengliu.viewmodel.MerchantListIntent
import com.jiyingcao.a51fengliu.viewmodel.MerchantListViewModel

/**
 * 商家列表页面，对应的View实现是 [MerchantListFragment]
 */
@Composable
fun MerchantListScreen(
    viewModel: MerchantListViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 首次可见时从ViewModel获取数据，实现懒加载
    LaunchedEffect(Unit) {
        viewModel.processIntent(MerchantListIntent.InitialLoad)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.showFullScreenLoading -> {
                // 显示全屏加载状态
                CircularProgressIndicator(
                    color = Color(0xFF007AFF)
                )
            }
            uiState.showFullScreenError -> {
                // 显示错误状态
                Text(
                    text = "加载失败: ${uiState.errorMessage}",
                    fontSize = 16.sp,
                    color = Color(0xFFFF3B30)
                )
            }
            uiState.showEmpty -> {
                // 显示空状态
                Text(
                    text = "暂无商家数据",
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93)
                )
            }
            uiState.showContent -> {
                // 显示实际内容（暂时只显示数量）
                // 目前没有显示商家列表项
                // 第三步，点击下面表示数量的Text，从列表中随机获取一条数据，跳转到商家详情页
                Text(
                    text = "商家列表\n共${uiState.merchants.size}条数据",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    // 点击后随机跳转到某个商家详情
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // 从列表中随机选择一个商家，取其id进行导航
                        val merchantId = uiState.merchants.random().id
                        onNavigateToDetail(merchantId)
                    }
                )
            }
            else -> {
                // 默认状态
                Text(
                    text = "商家",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            }
        }
    }
}

@Preview
@Composable
fun MerchantListScreenPreview() {
    MerchantListScreen()
}

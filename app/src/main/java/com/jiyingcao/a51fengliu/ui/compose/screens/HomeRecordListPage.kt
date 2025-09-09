package com.jiyingcao.a51fengliu.ui.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jiyingcao.a51fengliu.ui.compose.components.AppErrorLayout
import com.jiyingcao.a51fengliu.ui.compose.components.AppLoadingLayout
import com.jiyingcao.a51fengliu.viewmodel.HomeRecordListIntent
import com.jiyingcao.a51fengliu.viewmodel.HomeRecordListViewModel

@Composable
fun HomeRecordListPage(
    sort: String
) {
    // 使用 Hilt + AssistedFactory 创建需要sort参数的 ViewModel
    val viewModel = hiltViewModel<HomeRecordListViewModel, HomeRecordListViewModel.Factory>(
        key = "HomeRecordList-$sort",   // 【重要】使用sort作为key，确保两个子页面使用不同的ViewModel实例
        creationCallback = { factory -> factory.create(sort) }
    )
    val uiState by viewModel.uiState.collectAsState()

    // 首次进入或 sort 变化时加载数据
    LaunchedEffect(sort) {
        viewModel.processIntent(HomeRecordListIntent.InitialLoad)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.showFullScreenLoading -> {
                AppLoadingLayout()
            }
            uiState.showFullScreenError -> {
                AppErrorLayout(
                    errorMessage = uiState.errorMessage,
                    onButtonClick = { viewModel.processIntent(HomeRecordListIntent.Retry) }
                )
            }
            uiState.showContent || uiState.records.isNotEmpty() -> {
                // 本次不关注UI，简单显示数据条数即可
                Text(
                    text = "共${uiState.records.size}条数据",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )
            }
        }
    }
}


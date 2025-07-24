package com.jiyingcao.a51fengliu.ui.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.DetailActivity
import com.jiyingcao.a51fengliu.ui.compose.components.*
import com.jiyingcao.a51fengliu.ui.compose.theme.*
import com.jiyingcao.a51fengliu.util.timestampToDay
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.*

/**
 * 收藏列表页面
 *
 * @param onBackClick 返回按钮点击回调
 * @param onNavigate 导航回调，用于页面内跳转
 */
@Composable
fun FavoriteScreen(
    onBackClick: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // ViewModel 实例化，使用工厂创建
    val viewModel: FavoriteViewModel = viewModel(
        factory = FavoriteViewModelFactory(
            repository = RecordRepository.getInstance(RetrofitClient.apiService)
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    // 初始化加载
    LaunchedEffect(Unit) {
        viewModel.processIntent(FavoriteIntent.InitialLoad)
    }

    // 页面布局
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 标题栏
        AppTitleBar(
            backText = "我的收藏",
            onBackClick = onBackClick
        )

        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.showFullScreenLoading -> {
                    AppLoadingLayout()
                }

                uiState.showFullScreenError -> {
                    AppErrorLayout(
                        errorMessage = uiState.errorMessage.ifEmpty { "出错了，请稍后重试" },
                        onRetryClick = {
                            viewModel.processIntent(FavoriteIntent.Retry)
                        }
                    )
                }

                uiState.showEmpty -> {
                    FavoriteEmptyContent()
                }

                uiState.showContent -> {
                    FavoriteContent(
                        records = uiState.records,
                        onRecordClick = { record ->
                            // 跳转到详情页，这里使用原有的 DetailActivity
                            DetailActivity.start(context, record.id)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * 空状态内容
 */
@Composable
private fun FavoriteEmptyContent(
    modifier: Modifier = Modifier
) {
    AppErrorLayout(
        errorMessage = "暂无收藏内容",
        onRetryClick = null, // 空状态不显示重试按钮
        modifier = modifier
    )
}

/**
 * 收藏列表内容
 */
@Composable
private fun FavoriteContent(
    records: List<RecordInfo>,
    onRecordClick: (RecordInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        /*contentPadding = PaddingValues(
            horizontal = DefaultHorizontalSpace,
            vertical = DividerHeight
        ),*/
        verticalArrangement = Arrangement.spacedBy(DividerHeight)
    ) {
        items(
            items = records,
            key = { record -> record.id }
        ) { record ->
            RecordCard(
                record = record,
                onClick = { onRecordClick(record) }
            )
        }

        // 底部导航栏占位，适配EdgeToEdge效果
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    )
            )
        }
    }
}

/**
 * 收藏记录卡片组件 - 基于原有的 RecordListItem 但直接接受 RecordInfo
 */
@Composable
private fun RecordCard(
    record: RecordInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.clickable { onClick() }
    ) {
        RecordListItem(
            title = record.title,
            createTime = timestampToDay(record.publishedAt),
            browseCount = record.viewCount.orEmpty(),
            process = record.desc.orEmpty(),
            dz = record.cityCode.to2LevelName(),
            imageUrl = if (!record.coverPicture.isNullOrBlank()) {
                AppConfig.Network.BASE_IMAGE_URL + record.coverPicture
            } else {
                ""
            }
        )
    }
}
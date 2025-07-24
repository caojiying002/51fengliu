package com.jiyingcao.a51fengliu.ui.compose.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
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
                        isRefreshing = uiState.isRefreshing,
                        isLoadingMore = uiState.isLoadingMore,
                        hasLoadMoreError = uiState.isError && uiState.loadingType == LoadingType.LOAD_MORE,
                        noMoreData = uiState.noMoreData,
                        onRefresh = {
                            viewModel.processIntent(FavoriteIntent.Refresh)
                        },
                        onLoadMore = {
                            viewModel.processIntent(FavoriteIntent.LoadMore)
                        },
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteContent(
    records: List<RecordInfo>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasLoadMoreError: Boolean,
    noMoreData: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRecordClick: (RecordInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    
    // 监听滚动状态，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            
            // 当滚动到倒数第3个item时触发加载更多
            lastVisibleItemIndex > (totalItemsNumber - 3)
        }
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingMore && !noMoreData && !hasLoadMoreError && records.isNotEmpty()) {
                onLoadMore()
            }
        }
    }
    
    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
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

            // 加载更多指示器
            if (records.isNotEmpty()) {
                item {
                    LoadMoreIndicator(
                        isLoading = isLoadingMore,
                        hasError = hasLoadMoreError,
                        noMoreData = noMoreData,
                        onRetryClick = onLoadMore
                    )
                }
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

/**
 * Preview - 用于开发时预览
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun FavoriteScreenPreview() {
    AppTheme {
        FavoriteContent(
            records = listOf(
                RecordInfo(
                    id = "1",
                    userId = "123",
                    status = "1",
                    type = "1",
                    title = "示例收藏记录标题",
                    isRecommend = false,
                    isMerchant = false,
                    isExpired = false,
                    source = "1",
                    score = "5",
                    viewCount = "1234",
                    cityCode = "440300", // 深圳
                    girlNum = null,
                    girlAge = null,
                    girlBeauty = null,
                    environment = null,
                    consumeLv = null,
                    consumeAllNight = null,
                    serveList = null,
                    serveLv = null,
                    desc = "这是一个示例的收藏记录描述信息，用于展示列表项的样式效果。",
                    picture = null,
                    coverPicture = "250716/f296f278-81a2-4c24-87bb-1f6e48338561_t.jpeg",
                    anonymous = false,
                    ip = null,
                    publishedAt = "1690876800000", // 时间戳
                    createdAt = "1690876800000",
                    isFavorite = true,
                    vipProfileStatus = 4,
                    publisher = null,
                    userName = "测试用户",
                    userReputation = null,
                    userStatus = null,
                    style = null,
                    vipView = null,
                    userView = null,
                    guestView = null,
                    qq = null,
                    wechat = null,
                    telegram = null,
                    yuni = null,
                    phone = null,
                    address = null
                ),
                RecordInfo(
                    id = "2",
                    userId = "456",
                    status = "1",
                    type = "1",
                    title = "另一个收藏记录",
                    isRecommend = true,
                    isMerchant = false,
                    isExpired = false,
                    source = "1",
                    score = "4",
                    viewCount = "5678",
                    cityCode = "110000", // 北京
                    girlNum = null,
                    girlAge = null,
                    girlBeauty = null,
                    environment = null,
                    consumeLv = null,
                    consumeAllNight = null,
                    serveList = null,
                    serveLv = null,
                    desc = "这是另一个示例记录，展示多条记录的显示效果。",
                    picture = null,
                    coverPicture = "250721/5fc3d363-06a1-45ea-a0af-ba8cc35f250b_t.jpg",
                    anonymous = false,
                    ip = null,
                    publishedAt = "1690790400000",
                    createdAt = "1690790400000",
                    isFavorite = true,
                    vipProfileStatus = 3,
                    publisher = null,
                    userName = "测试用户2",
                    userReputation = null,
                    userStatus = null,
                    style = null,
                    vipView = null,
                    userView = null,
                    guestView = null,
                    qq = null,
                    wechat = null,
                    telegram = null,
                    yuni = null,
                    phone = null,
                    address = null
                )
            ),
            isRefreshing = false,
            isLoadingMore = false,
            hasLoadMoreError = false,
            noMoreData = false,
            onRefresh = {},
            onLoadMore = {},
            onRecordClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoriteEmptyContentPreview() {
    AppTheme {
        FavoriteEmptyContent()
    }
}
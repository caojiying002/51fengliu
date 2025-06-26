package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.theme.*
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.*
import com.jiyingcao.a51fengliu.config.AppConfig
import androidx.lifecycle.ViewModelProvider

class MerchantDetailComposeActivity : BaseActivity() {
    private lateinit var viewModel: MerchantDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val merchantId = intent.getMerchantId()
        if (merchantId == null) {
            // TODO: 显示错误提示
            finish()
            return
        }

        setupViewModel(merchantId)

        setContent {
            AppTheme {
                MerchantDetailScreen(
                    merchantId = merchantId,
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }

        // 触发初始加载
        viewModel.processIntent(MerchantDetailIntent.LoadDetail)
    }

    private fun setupViewModel(merchantId: String) {
        viewModel = ViewModelProvider(
            this,
            MerchantDetailViewModelFactory(
                merchantId,
                RecordRepository.getInstance(RetrofitClient.apiService)
            )
        )[MerchantDetailViewModel::class.java]
    }

    companion object {
        private const val TAG = "MerchantDetailComposeActivity"
        private const val KEY_EXTRA_MERCHANT_ID = "extra_merchant_id"

        @JvmStatic
        fun createIntent(context: Context, id: String): Intent =
            Intent(context, MerchantDetailComposeActivity::class.java)
                .putExtra(KEY_EXTRA_MERCHANT_ID, id)

        @JvmStatic
        fun start(context: Context, id: String) {
            context.startActivity(createIntent(context, id))
        }

        private fun Intent.getMerchantId(): String? = getStringExtra(KEY_EXTRA_MERCHANT_ID)
    }
}


@Composable
fun MerchantDetailScreen(
    merchantId: String,
    viewModel: MerchantDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CustomTitleBar(onBackClick = onBackClick)

        // 内容区域 - 根据ViewModel状态显示不同的布局
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.showFullScreenLoading -> {
                    LoadingLayout()
                }
                
                uiState.showFullScreenError -> {
                    ErrorLayout(
                        errorMessage = uiState.errorMessage.ifEmpty { "出错了，请稍后重试" },
                        onRetryClick = {
                            viewModel.processIntent(MerchantDetailIntent.Retry)
                        }
                    )
                }
                
                uiState.showContent -> {
                    MerchantDetailContent(
                        merchant = uiState.merchant!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun MerchantDetailContent(
    merchant: Merchant,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = DefaultHorizontalSpace, vertical = 0.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 图片容器 - 对应 merchant_content_detail.xml 的 image_container
        MerchantImageContainer(merchant = merchant)

        Spacer(modifier = Modifier.height(DividerHeight))

        // 基本信息容器 - 对应 merchant_content_detail.xml 的 basic_info_container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Surface,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 商户名称 - 对应 @+id/name
                Text(
                    text = merchant.name,
                    fontSize = 18.sp,
                    color = TextTitle,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 省份信息 - 对应 @+id/province
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_dz),
                        contentDescription = "位置图标",
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = merchant.cityCode.to2LevelName(),
                        fontSize = 14.sp,
                        color = Primary,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 描述信息 - 对应 @+id/desc
                Text(
                    text = merchant.desc ?: merchant.intro ?: "",
                    fontSize = 14.sp,
                    color = TextContent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(DividerHeight))

        // 联系信息容器 - 对应 merchant_content_detail.xml 的 contact_info_container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    color = Surface,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "联系信息容器",
                color = OnSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 占位内容，暂时不要删掉
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "商户详情页面",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "商户ID: ${merchant.id}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "这是使用Jetpack Compose构建的新页面。\n你可以在这里添加商户的详细信息、图片、介绍等内容。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 示例按钮，暂时不要删掉
        Button(
            onClick = { /* 添加你的点击逻辑 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("示例操作按钮")
        }
    }
}

@Composable
fun MerchantImageContainer(merchant: Merchant) {
    val pictures = merchant.getPictures()
    
    // 如果没有图片，隐藏整个容器
    if (pictures.isEmpty()) {
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Surface,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 显示最多4张图片，每个位置都占1/4宽度
            for (index in 0 until 4) {
                val imageUrl = pictures.getOrNull(index)
                
                if (imageUrl != null) {
                    // 显示实际图片
                    AsyncImage(
                        model = AppConfig.Network.BASE_IMAGE_URL + imageUrl,
                        contentDescription = "商户图片 ${index + 1}",
                        modifier = Modifier
                            .weight(1f) // 每个图片占1/4宽度
                            .aspectRatio(2f / 3f), // 2:3比例
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.placeholder),
                        error = painterResource(R.drawable.image_broken)
                    )
                } else {
                    // 空位置用Spacer填充，保持1/4宽度
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CustomTitleBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 状态栏占位, 使用WindowInsets.statusBars.asPaddingValues().calculateTopPadding()获取系统状态栏高度
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding()
                )
        )

        // 标题栏内容 - 对应原layout中的FrameLayout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ToolbarHeight) // 使用theme中定义的56.dp
                .padding(horizontal = 16.dp)
        ) {
            // 返回按钮 - 对应原layout中的TextView
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { onBackClick() }
                    .padding(vertical = 8.dp), // 增加点击区域
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "返回",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "返回",
                    fontSize = 18.sp,
                    color = Primary,
                    fontWeight = FontWeight.Normal
                )
            }
        }

    }
}

// 加载布局组件 - 对应 default_loading.xml
@Composable
fun LoadingLayout(
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false // 是否作为覆盖层显示
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isOverlay) {
                    // 如果是覆盖层，添加半透明背景
                    Modifier.background(Color.Black.copy(alpha = 0.3f))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Primary,
            modifier = Modifier.size(48.dp)
        )
    }
}

// 错误布局组件 - 对应 default_error.xml
@Composable
fun ErrorLayout(
    errorMessage: String = "出错了，请稍后重试",
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = errorMessage,
            fontSize = 14.sp,
            color = TextContent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )

        // 重试按钮 - 只有当 onRetryClick 不为 null 时才显示
        onRetryClick?.let { retryAction ->
            Text(
                text = "点击重试",
                fontSize = 14.sp,
                color = Primary,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { retryAction() }
                    .padding(horizontal = 16.dp, vertical = 8.dp) // 增加点击区域
            )
        }
    }
}

// ==== 以下是保留的Material TopAppBar实现，作为学习参考 ====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDetailScreenWithMaterialTopBar(
    merchantId: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "商户详情", // 你可以根据需要修改标题
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        MerchantDetailContentOriginal(
            merchantId = merchantId,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
fun MerchantDetailContentOriginal(
    merchantId: String,
    modifier: Modifier = Modifier
) {
    // 原来的占位内容实现，保留作为参考
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "商户详情页面 (Material版本)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "商户ID: $merchantId",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MerchantDetailContentPreview() {
    AppTheme {
        MerchantDetailContent(
            merchant = Merchant(
                id = "55",
                name = "厦门可选不限次数",
                cityCode = "350000",
                showLv = null,
                picture = "240824/3628f597-86b9-4dda-845d-fadc3172ba9d.jpg,240824/08f4a449-2cec-4478-835b-9bdee191607a.jpg",
                coverPicture = null,
                intro = "无套路、不办卡、没有任何隐形消费！",
                desc = "这是一个示例描述信息，用于展示商户的详细信息内容。",
                validStart = null,
                validEnd = null,
                vipProfileStatus = null,
                style = "merchant",
                status = "1",
                contact = null
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CustomTitleBarPreview() {
    AppTheme {
        CustomTitleBar(
            onBackClick = { }
        )
    }
}
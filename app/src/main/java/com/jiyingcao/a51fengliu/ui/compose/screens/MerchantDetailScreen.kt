package com.jiyingcao.a51fengliu.ui.compose.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.ui.auth.AuthActivity
import com.jiyingcao.a51fengliu.ui.compose.components.*
import com.jiyingcao.a51fengliu.ui.compose.navigation.ComposeDestinations
import com.jiyingcao.a51fengliu.ui.compose.theme.*
import com.jiyingcao.a51fengliu.viewmodel.*

/**
 * 商户详情页面
 *
 * @param merchantId 商户ID
 * @param onBackClick 返回按钮点击回调
 * @param onNavigate 导航回调，用于页面内跳转
 */
@Composable
fun MerchantDetailScreen(
    merchantId: String,
    onBackClick: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    if (merchantId.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppTitleBar(
                onBackClick = onBackClick
            )
            AppErrorLayout(
                errorMessage = "商家信息不存在",
                buttonText = "返回",
                onButtonClick = onBackClick,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
        return
    }

    val context = LocalContext.current

    // 缓存 ViewModel 工厂，避免每次重组都创建
    val factory = remember(merchantId) {
        MerchantDetailViewModel.Factory(merchantId = merchantId)
    }

    val viewModel: MerchantDetailViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()

    // 初始化加载
    LaunchedEffect(merchantId) {
        viewModel.processIntent(MerchantDetailIntent.InitialLoad)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AppTitleBar(
            onBackClick = onBackClick
        )

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
                        onButtonClick = {
                            viewModel.processIntent(MerchantDetailIntent.Retry)
                        }
                    )
                }

                uiState.showContent -> {
                    MerchantDetailContent(
                        uiState = uiState,
                        onContactAction = { actionType ->
                            handleContactAction(
                                actionType = actionType,
                                context = context,
                                onNavigate = onNavigate
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * 商户详情内容
 */
@Composable
private fun MerchantDetailContent(
    uiState: MerchantDetailUiState,
    onContactAction: (ContactActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val merchant = uiState.merchant!!
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = DefaultHorizontalSpace, vertical = 0.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 图片容器
        MerchantImageContainer(merchant = merchant)

        Spacer(modifier = Modifier.height(DividerHeight))

        // 基本信息容器
        MerchantBasicInfoCard(merchant = merchant)

        Spacer(modifier = Modifier.height(DividerHeight))

        // 联系信息容器
        MerchantContactCard(
            showContact = uiState.showContact,
            contactText = uiState.contactText,
            promptMessage = uiState.contactPromptMessage,
            actionButtonText = uiState.contactActionButtonText,
            actionType = uiState.contactActionType,
            onContactAction = onContactAction
        )

        Spacer(modifier = Modifier.height(DividerHeight))

        // 底部导航栏占位，适配EdgeToEdge效果
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

/**
 * 处理联系方式相关的操作
 */
private fun handleContactAction(
    actionType: ContactActionType,
    context: Context,
    onNavigate: (String) -> Unit
) {
    when (actionType) {
        ContactActionType.LOGIN -> {
            // 跳转到登录页，这里暂时使用原有的 AuthActivity
            // 未来可以迁移到 Compose 版本的登录页
            AuthActivity.start(context)
        }
        ContactActionType.UPGRADE_VIP -> {
            // 跳转到VIP升级页
            //onNavigate(ComposeDestinations.VIP_UPGRADE)
        }
        ContactActionType.NONE -> {
            // 无操作
        }
    }
}

/**
 * Preview - 用于开发时预览
 */
@Preview(showBackground = true)
@Composable
private fun MerchantDetailScreenPreview() {
    AppTheme {
        MerchantDetailContent(
            uiState = MerchantDetailUiState(
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
                ),
                isLoggedIn = false
            ),
            onContactAction = {}
        )
    }
}
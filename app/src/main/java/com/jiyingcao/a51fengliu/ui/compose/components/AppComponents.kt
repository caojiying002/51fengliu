package com.jiyingcao.a51fengliu.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.compose.theme.*

/**
 * 通用标题栏组件
 * 参考原来的 TitleBarBack 设计：
 * - 没有居中标题
 * - 返回按钮文字可自定义，同时承担标题显示功能
 * - 保持原有的视觉效果
 */
@Composable
fun AppTitleBar(
    backText: String = "返回",
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 状态栏占位
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
            // 返回按钮 - 对应原layout中的TextView，同时承担标题功能
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
                    text = backText,
                    fontSize = 18.sp,
                    color = Primary,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 通用加载布局组件
 */
@Composable
fun AppLoadingLayout(
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isOverlay) {
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

/**
 * 通用错误布局组件
 */
@Composable
fun AppErrorLayout(
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

        onRetryClick?.let { retryAction ->
            Spacer(modifier = Modifier.height(16.dp))
            AppButton(
                text = "点击重试",
                onClick = retryAction,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

/**
 * 通用卡片容器组件
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Surface,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        content()
    }
}

/**
 * 通用按钮组件
 * 对应View版本的@style/ButtonStyle样式
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor = when {
        !enabled -> ButtonDisabled
        isPressed -> ButtonPressed
        else -> Primary
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp), // 与View版本按钮高度保持一致
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        shadowElevation = if (enabled) 2.dp else 0.dp,
        interactionSource = interactionSource
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 10.dp) // 对应selector中的padding
        ) {
            Text(
                text = text,
                fontSize = ButtonTextDefaultSize.value.sp, // 15sp
                color = White,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ===== Preview 组件 =====
@Preview(showBackground = true, name = "默认返回按钮")
@Composable
private fun AppTitleBarDefaultPreview() {
    AppTheme {
        AppTitleBar(
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "自定义返回文字")
@Composable
private fun AppTitleBarCustomPreview() {
    AppTheme {
        Column {
            AppTitleBar(
                backText = "商户详情",
                onBackClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            AppTitleBar(
                backText = "个人资料",
                onBackClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            AppTitleBar(
                backText = "设置",
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppLoadingLayoutPreview() {
    AppTheme {
        AppLoadingLayout()
    }
}

/**
 * 加载更多指示器组件
 * 用于显示列表底部的加载状态
 */
@Composable
fun LoadMoreIndicator(
    isLoading: Boolean,
    hasError: Boolean,
    noMoreData: Boolean,
    errorMessage: String = "加载失败，点击重试",
    noMoreDataMessage: String = "没有更多数据了",
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacingMedium),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "加载中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            hasError -> {
                TextButton(
                    onClick = onRetryClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            noMoreData -> {
                Text(
                    text = noMoreDataMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppErrorLayoutPreview() {
    AppTheme {
        AppErrorLayout(
            errorMessage = "网络连接失败，请检查网络设置",
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadMoreIndicatorPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // 加载中状态
            LoadMoreIndicator(
                isLoading = true,
                hasError = false,
                noMoreData = false
            )
            
            // 错误状态
            LoadMoreIndicator(
                isLoading = false,
                hasError = true,
                noMoreData = false,
                onRetryClick = {}
            )
            
            // 无更多数据状态
            LoadMoreIndicator(
                isLoading = false,
                hasError = false,
                noMoreData = true
            )
        }
    }
}
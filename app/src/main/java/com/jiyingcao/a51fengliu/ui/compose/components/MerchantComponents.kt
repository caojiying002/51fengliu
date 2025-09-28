package com.jiyingcao.a51fengliu.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.ui.compose.theme.*
import com.jiyingcao.a51fengliu.util.to2LevelName
import com.jiyingcao.a51fengliu.viewmodel.ContactActionType

/**
 * 商户图片容器组件
 * 显示商户的图片，最多显示4张
 */
@Composable
fun MerchantImageContainer(
    merchant: Merchant,
    modifier: Modifier = Modifier
) {
    val pictures = merchant.getPictures()

    // 如果没有图片，隐藏整个容器
    if (pictures.isEmpty()) {
        return
    }

    AppCard(modifier = modifier) {
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
                        error = painterResource(R.drawable.picture_loading_failed)
                    )
                } else {
                    // 空位置用Spacer填充，保持1/4宽度
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 商户基本信息卡片组件
 * 显示商户名称、位置和描述信息
 */
@Composable
fun MerchantBasicInfoCard(
    merchant: Merchant,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 商户名称
            Text(
                text = merchant.name,
                fontSize = 18.sp,
                color = TextTitle,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 省份信息
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

            // 描述信息
            Text(
                text = merchant.desc ?: merchant.intro ?: "",
                fontSize = 14.sp,
                color = TextContent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 商户联系信息卡片组件
 * 根据登录状态和VIP状态显示不同的内容
 */
@Composable
fun MerchantContactCard(
    showContact: Boolean,
    contactText: String?,
    promptMessage: String,
    actionButtonText: String,
    actionType: ContactActionType,
    onContactAction: (ContactActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        if (showContact && !contactText.isNullOrBlank()) {
            // 显示实际联系方式
            Text(
                text = contactText,
                fontSize = 14.sp,
                color = TextContent,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // 显示提示信息和操作按钮
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 提示信息
                Text(
                    text = promptMessage,
                    fontSize = 14.sp,
                    color = TextContent,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // 操作按钮
                if (actionButtonText.isNotEmpty()) {
                    ContactActionButton(
                        text = actionButtonText,
                        actionType = actionType,
                        onClick = { onContactAction(actionType) }
                    )
                }
            }
        }
    }
}

/**
 * 联系方式操作按钮组件
 * 使用统一的按钮样式，对应View版本的@style/ButtonStyle
 */
@Composable
private fun ContactActionButton(
    text: String,
    actionType: ContactActionType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = modifier
            .width(140.dp) // 指定固定宽度，类似wrap_content但有最小宽度
            .padding(vertical = 8.dp)
    )
}

// ===== Preview 组件 =====
@Preview(showBackground = true)
@Composable
private fun MerchantBasicInfoCardPreview() {
    AppTheme {
        MerchantBasicInfoCard(
            merchant = Merchant(
                id = "55",
                name = "厦门可选不限次数",
                cityCode = "350000",
                showLv = null,
                picture = "",
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
private fun MerchantContactCardPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 显示联系方式的情况
            MerchantContactCard(
                showContact = true,
                contactText = "微信：example123\n电话：138-0000-0000",
                promptMessage = "",
                actionButtonText = "",
                actionType = ContactActionType.NONE,
                onContactAction = {}
            )

            // 需要登录的情况
            MerchantContactCard(
                showContact = false,
                contactText = null,
                promptMessage = "你需要登录才能继续查看联系方式。",
                actionButtonText = "立即登录",
                actionType = ContactActionType.LOGIN,
                onContactAction = {}
            )

            // 需要升级VIP的情况
            MerchantContactCard(
                showContact = false,
                contactText = null,
                promptMessage = "你需要VIP才能继续查看联系方式。",
                actionButtonText = "立即升级VIP",
                actionType = ContactActionType.UPGRADE_VIP,
                onContactAction = {}
            )
        }
    }
}
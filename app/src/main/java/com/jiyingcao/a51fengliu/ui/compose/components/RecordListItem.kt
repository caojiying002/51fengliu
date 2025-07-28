package com.jiyingcao.a51fengliu.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jiyingcao.a51fengliu.R

/**
 * 记录列表项组件
 * 响应式高度布局，左侧内容区域自适应，右侧图片区域固定尺寸
 * 默认字体字号下可以认为是固定高度174dp（160dp内容 + 7dp上下padding）
 * 支持大字体模式下的响应式布局，内容溢出时自动上下撑开
 */
@Composable
fun RecordListItem(
    title: String,
    createTime: String,
    browseCount: String,
    process: String,
    dz: String,
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 174.dp) // 最小总高度 160 + 7*2 = 174dp
            .padding(start = 8.dp, top = 7.dp, end = 7.dp, bottom = 7.dp), // 左8dp，右上下7dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧内容区域 - 自适应高度
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF222222), // @color/text_item_title
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = createTime,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFA3A3A3), // @color/text_light
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = process,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF525252), // @color/text_content
                    fontSize = 14.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 地址信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_dz),
                    contentDescription = "地址图标",
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = dz,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFEC4899),
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 右侧图片区域 - 固定高度160dp，4:3比例，纵向居中
        AsyncImage(
            model = imageUrl,
            contentDescription = "封面配图",
            modifier = Modifier
                .height(160.dp)
                .width(120.dp) // 160 * 3/4 = 120dp，保持4:3比例
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true, name = "Normal Content")
@Composable
private fun RecordListItemPreview() {
    RecordListItem(
        title = "Sample Title",
        createTime = "2024-07-18",
        browseCount = "8900",
        process = "Process content may be long long long and might need multiple lines to display properly",
        dz = "广东-深圳市",
        imageUrl = ""
    )
}

@Preview(showBackground = true, name = "Long Content", fontScale = 1.5f)
@Composable
private fun RecordListItemLongContentPreview() {
    RecordListItem(
        title = "Very Very Very Long Title That Might Be Truncated",
        createTime = "2024-07-18 15:30:25",
        browseCount = "8900",
        process = "This is a very long process description that will definitely take up multiple lines and might even need more space when the system font size is increased by users in accessibility settings",
        dz = "广东省-深圳市南山区",
        imageUrl = ""
    )
}

@Preview(showBackground = true, name = "Large Font", fontScale = 2.0f)
@Composable
private fun RecordListItemLargeFontPreview() {
    RecordListItem(
        title = "Large Font Test",
        createTime = "2024-07-18",
        browseCount = "8900",
        process = "测试大字体情况下的显示效果，看看响应式高度是否能够正确处理内容溢出的问题",
        dz = "广东-深圳市",
        imageUrl = ""
    )
}
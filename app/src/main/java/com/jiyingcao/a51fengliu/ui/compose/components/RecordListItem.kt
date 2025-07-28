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
 * 固定高度布局，左侧内容区域，右侧图片区域
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
            .height(174.dp) // 固定总高度 160 + 7*2
            .padding(7.dp), // 统一边距
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧内容区域
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
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

        // 右侧图片区域 - 固定高度160dp，4:3比例
        AsyncImage(
            model = imageUrl,
            contentDescription = "记录配图",
            modifier = Modifier
                .height(160.dp)
                .width(120.dp) // 160 * 3/4 = 120dp，保持4:3比例
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true)
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
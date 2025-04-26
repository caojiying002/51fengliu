package com.jiyingcao.a51fengliu.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.theme.TextContent
import com.jiyingcao.a51fengliu.ui.theme.TextItemTitle
import com.jiyingcao.a51fengliu.ui.theme.Primary

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
    var contentHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左半部分内容
        Column(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { coordinates ->
                    contentHeightPx = coordinates.size.height
                }
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF222222), // @color/text_item_title
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = createTime,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFA3A3A3), // @color/text_light
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = process,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF525252), // @color/text_content
                    fontSize = 14.sp
                ),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon
                Image(
                    painter = painterResource(id = R.drawable.icon_dz),
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dz,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFEC4899), // @color/primary
                        fontSize = 14.sp
                    ),
                    maxLines = 1
                )
            }
        }

        // 右半部分图片
        val contentHeightDp = with(density) { contentHeightPx.toDp() }
        val imageWidth = contentHeightDp * 4 / 3
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .height(contentHeightDp)
                .width(imageWidth)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RecordListItemPreview() {
    RecordListItem(
        title = "Sample Title",
        createTime = "2024-07-18",
        browseCount = "8900",
        process = "Process content may be long long long and might need multiple lines to display properly",
        dz = "广东-深圳市",
        imageUrl = ""
    )
}
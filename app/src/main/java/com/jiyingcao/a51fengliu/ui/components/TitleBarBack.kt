package com.jiyingcao.a51fengliu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.theme.AppTheme
import com.jiyingcao.a51fengliu.ui.theme.TextStrong
import com.jiyingcao.a51fengliu.ui.theme.TitleBarBackTextSize
import com.jiyingcao.a51fengliu.ui.theme.ToolbarHeight
import com.jiyingcao.a51fengliu.ui.theme.spacingLarge

@Composable
fun TitleBarBack(
    modifier: Modifier = Modifier,
    text: String = "返回",
    onBackClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ToolbarHeight)
            .padding(horizontal = spacingLarge),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.clickable { onBackClick?.invoke() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                contentDescription = text,
                tint = TextStrong,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = text,
                color = TextStrong,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(
    name = "TitleBarBack Preview",
    showBackground = true
)
@Composable
fun TitleBarBackPreview() {
    AppTheme {
        TitleBarBack()
    }
}

@Preview(
    name = "TitleBarBack with Custom Text",
    showBackground = true
)
@Composable
fun TitleBarBackCustomTextPreview() {
    AppTheme {
        TitleBarBack(
            text = "自定义返回",
            onBackClick = {}
        )
    }
}

@Preview(
    name = "TitleBarBack Dark Theme",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun TitleBarBackDarkPreview() {
    AppTheme(darkTheme = true) {
        TitleBarBack(
            text = "返回",
            onBackClick = {}
        )
    }
}
package com.jiyingcao.a51fengliu.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.ui.compose.navigation.MainDestinations
import com.jiyingcao.a51fengliu.ui.compose.navigation.MainNavGraph
import com.jiyingcao.a51fengliu.ui.compose.theme.AppTheme
import com.jiyingcao.a51fengliu.ui.compose.screens.MerchantListScreen
import com.jiyingcao.a51fengliu.ui.compose.theme.*
import dagger.hilt.android.AndroidEntryPoint

/**
 * Compose版本的APP主页，用于替代View版本的主页 [MainActivity]
 */
@AndroidEntryPoint
class ComposeMainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                val navController = rememberNavController()
                // 将路由管理抽取到独立的导航图中，便于扩展与维护
                MainNavGraph(navController = navController)
            }
        }
    }
}

@Composable
fun MainScreen(
    // 路由跳转回调，由外层 NavHost 提供
    onNavigate: (String) -> Unit = {}
) {
    // 配置更改（旋转屏幕、暗色模式）后，使用 rememberSaveable 保持所选 tab 状态
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> RecordScreen()
                2 -> StreetScreen()
                3 -> MerchantListScreen(
                    onNavigateToDetail = { merchantId ->
                        // 构建详情页路由并跳转（使用统一的路由辅助函数）
                        onNavigate(MainDestinations.merchantDetail(merchantId))
                    }
                )
                4 -> ProfileScreen()
            }
        }
        
        // 自定义底部导航栏，保证UI样式灵活性
        CustomBottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            backgroundColor = Surface
        )
        
        // edge-to-edge适配，高度设置为系统底部导航栏高度，背景色与CustomBottomNavigation保持一致
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(Surface)
        )
    }
}

@Composable
fun CustomBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    backgroundColor: Color = Surface
) {
    val tabs = listOf(
        TabItem("首页", R.drawable.ic_home),
        TabItem("信息", R.drawable.ic_record),
        TabItem("暗巷", R.drawable.ic_street),
        TabItem("商家", R.drawable.ic_merchant),
        TabItem("我的", R.drawable.ic_profile)
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                CustomTabItem(
                    tab = tab,
                    isSelected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CustomTabItem(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedColor = Primary
    val unselectedColor = TextMainTab
    
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = tab.iconRes),
            contentDescription = tab.title,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) selectedColor else unselectedColor
        )
        
        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tab.title,
            fontSize = 12.sp,
            color = if (isSelected) selectedColor else unselectedColor
        )
    }
}

data class TabItem(
    val title: String,
    val iconRes: Int
)

// Placeholder screens for each tab
@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "首页",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}

@Composable
fun RecordScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "信息",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}

@Composable
fun StreetScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "暗巷",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}


@Composable
fun ProfileScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "我的",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AppTheme {
        MainScreen()
    }
}

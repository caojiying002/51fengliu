package com.jiyingcao.a51fengliu.ui.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.ui.theme.AppTheme

/**
 * 统一的 Compose 容器 Activity
 * 所有 Compose 页面都在这里管理，避免为每个页面创建单独的 Activity/Fragment
 */
class ComposeContainerActivity : BaseActivity() {

}
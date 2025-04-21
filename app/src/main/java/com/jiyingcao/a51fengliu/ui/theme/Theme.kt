package com.jiyingcao.a51fengliu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    //primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
    primary = MdThemePrimary,
    onPrimary = Color.White,
    background = Background,
    onBackground = Color.Black,
    error = MdThemeError,
    outline = MdThemeOutline,
    surface = Color.White,
    onSurface = Color.Black,
    // 可根据需要继续补充
)

private val DarkColorScheme = darkColorScheme(
    //primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,

    primary = MdThemePrimary,
    onPrimary = Color.Black,
    background = Background,
    onBackground = Color.White,
    error = MdThemeError,
    outline = MdThemeOutline,
    surface = Color.Black,
    onSurface = Color.White,
    // 可根据需要继续补充
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        //shapes = Shapes,         // 可自定义 Shape.kt
        content = content
    )
}
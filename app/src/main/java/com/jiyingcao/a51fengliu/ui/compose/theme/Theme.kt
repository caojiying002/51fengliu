package com.jiyingcao.a51fengliu.ui.compose.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Background,
    onPrimaryContainer = OnBackground,
    
    // Background and surface colors
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = Surface,
    onSurfaceVariant = OnSurface,
    
    // Secondary colors (keeping some original colors)
    secondary = PurpleGrey40,
    tertiary = Pink40,
    
    // Other important colors
    error = Error,
    outline = Outline,
)

private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = PrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = BackgroundDark,
    onPrimaryContainer = OnBackgroundDark,
    
    // Background and surface colors
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceDark,
    
    // Secondary colors (keeping some original colors)
    secondary = PurpleGrey80,
    tertiary = Pink80,
    
    // Other important colors
    error = ErrorDark,
    outline = OutlineDark,
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
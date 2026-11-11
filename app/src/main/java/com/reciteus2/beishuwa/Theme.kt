package com.reciteus2.beishuwa

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    // 浅色模式：天蓝主题
    primary = Color(0xFF0277BD),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6ECFA),
    onPrimaryContainer = Color(0xFF00324A),
    secondary = Color(0xFF535353),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDDEE0),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF4A4A4A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF0F1F3),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFDDDEE0),
    onSurfaceVariant = Color(0xFF535353),
    outline = Color(0xFF808183),
    outlineVariant = Color(0xFFDDDEE0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    // 深色模式：金黄主题
    primary = Color(0xFFFFC83D),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF4A3A00),
    onPrimaryContainer = Color(0xFFFFE7A3),
    secondary = Color(0xFFCACBCD),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFF0F1F3),
    tertiary = Color(0xFFBFC1C4),
    onTertiary = Color(0xFF1A1A1A),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF0F1F3),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFF0F1F3),
    surfaceVariant = Color(0xFF2A2B2D),
    onSurfaceVariant = Color(0xFFDDDEE0),
    outline = Color(0xFF808183),
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF1A1A1A),
)

@Composable
fun BeishuwaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(0.dp),
            small = RoundedCornerShape(0.dp),
            medium = RoundedCornerShape(0.dp),
            large = RoundedCornerShape(0.dp),
            extraLarge = RoundedCornerShape(0.dp),
        ),
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

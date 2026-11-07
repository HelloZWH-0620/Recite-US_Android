package com.reciteus2.beishuwa

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A6A1F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3E3B8),
    onPrimaryContainer = Color(0xFF2B1F00),
    secondary = Color(0xFF6F5A2E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9DDC3),
    onSecondaryContainer = Color(0xFF241A05),
    tertiary = Color(0xFF7A6A88),
    background = Color(0xFFF7F3EA),
    onBackground = Color(0xFF221F19),
    surface = Color(0xFFFBF8F1),
    onSurface = Color(0xFF221F19),
    surfaceVariant = Color(0xFFEDE4D2),
    onSurfaceVariant = Color(0xFF5C5647),
    outline = Color(0xFFB7AC94),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0BC63),
    onPrimary = Color(0xFF3D2E00),
    primaryContainer = Color(0xFF5A4510),
    onPrimaryContainer = Color(0xFFF7E4B0),
    secondary = Color(0xFFD5C39A),
    onSecondary = Color(0xFF3A2F12),
    secondaryContainer = Color(0xFF51452A),
    onSecondaryContainer = Color(0xFFE9DDC3),
    tertiary = Color(0xFFCFC0DC),
    background = Color(0xFF12100C),
    onBackground = Color(0xFFEDE6D8),
    surface = Color(0xFF1B1812),
    onSurface = Color(0xFFEDE6D8),
    surfaceVariant = Color(0xFF2A251C),
    onSurfaceVariant = Color(0xFFB9AF9B),
    outline = Color(0xFF857C68),
    error = Color(0xFFF2B8B5),
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
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

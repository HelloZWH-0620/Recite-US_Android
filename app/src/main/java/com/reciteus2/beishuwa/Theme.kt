package com.reciteus2.beishuwa

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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

/** 自定义字体（resource/wordtype/Regular.ttf），失败时回退系统默认字体。 */
object AppFonts {
    private var cached: FontFamily? = null

    fun custom(context: Context): FontFamily {
        cached?.let { return it }
        val family = runCatching {
            FontFamily(Typeface.createFromAsset(context.assets, "fonts/Regular.ttf"))
        }.getOrDefault(FontFamily.Default)
        cached = family
        return family
    }
}

private fun Typography.withFontFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

@Composable
fun BeishuwaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    val context = LocalContext.current
    val useCustomFont = SettingsRepository.useCustomFont.value

    val typography = remember(useCustomFont) {
        val base = Typography()
        if (useCustomFont) base.withFontFamily(AppFonts.custom(context)) else base
    }
    val defaultTextStyle = LocalTextStyle.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(
        LocalTextStyle provides if (useCustomFont) {
            defaultTextStyle.copy(fontFamily = AppFonts.custom(context))
        } else {
            defaultTextStyle
        },
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = typography,
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
}

/** 按「阅读字体大小」设置缩放文字（sp 随 fontScale 放大，布局 dp 不变）。 */
@Composable
fun AppFontScaled(content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val extra = SettingsRepository.fontScale.floatValue
    Box {
        CompositionLocalProvider(
            LocalDensity provides Density(base.density, base.fontScale * extra),
        ) {
            content()
        }
        FontScaleHint()
    }
}

/** 双指缩放时在屏幕中央悬浮显示当前字体大小（以 10% 为单位）。 */
@Composable
fun FontScaleHint() {
    val active = SettingsRepository.pinchActive.value
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (active) {
            visible = true
        } else {
            delay(600)
            visible = false
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val percent = (SettingsRepository.fontScale.floatValue * 100f / 10f)
                .roundToInt() * 10
            Text(
                text = "字体大小 $percent%",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .background(Color(0xCC000000))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * 双指缩放手势：开启「双指缩放调整字体大小」时，两指捏合即可实时调整阅读字体大小。
 */
fun Modifier.pinchToFontScale(enabled: Boolean, context: Context): Modifier {
    if (!enabled) return this
    return this.pointerInput(context) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var lastDistance = 0f
            var pinching = false
            var changed = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                if (pressed.size < 2) {
                    lastDistance = 0f
                    continue
                }
                if (!pinching) {
                    pinching = true
                    SettingsRepository.pinchActive.value = true
                }
                val distance = (pressed[0].position - pressed[1].position).getDistance()
                if (lastDistance > 0f && distance > 0f) {
                    val zoom = distance / lastDistance
                    if (zoom != 1f) {
                        SettingsRepository.fontScale.floatValue =
                            (SettingsRepository.fontScale.value * zoom)
                                .coerceIn(SettingsRepository.MIN_FONT_SCALE, SettingsRepository.MAX_FONT_SCALE)
                        changed = true
                    }
                }
                lastDistance = distance
                // 双指期间拦截全部事件，避免缩放的同时触发列表滚动
                event.changes.forEach { it.consume() }
            }
            if (pinching) {
                SettingsRepository.pinchActive.value = false
                if (changed) {
                    SettingsRepository.setFontScale(context, SettingsRepository.fontScale.value)
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

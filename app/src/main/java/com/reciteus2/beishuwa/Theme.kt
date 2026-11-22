package com.reciteus2.beishuwa

import android.content.Context
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme as materialDark
import androidx.compose.material3.lightColorScheme as materialLight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import ui.feice.mochi.theme.MochiColorScheme
import ui.feice.mochi.theme.MochiGlassDefaults
import ui.feice.mochi.theme.MochiShapes
import ui.feice.mochi.theme.MochiTextStyles
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.ios.MochiIosTheme
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.theme.defaultShapes
import ui.feice.mochi.theme.defaultTextStyles

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

private fun MochiTextStyles.withFontFamily(family: FontFamily) = copy(
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
    largeTitle = largeTitle.copy(fontFamily = family),
    navTitle = navTitle.copy(fontFamily = family),
    body = body.copy(fontFamily = family),
    subhead = subhead.copy(fontFamily = family),
    footnote = footnote.copy(fontFamily = family),
)

private fun MochiTextStyles.toMaterialTypography() = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall,
)

private fun MochiShapes.toMaterialShapes() = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun MochiColorScheme.toMaterialColors() = if (isDark) {
    materialDark(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        // iOS 分组列表底色：卡片浮在略深一层的背景上
        background = if (isDark) surfaceContainerLowest else surfaceContainer,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
} else {
    materialLight(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        // iOS 分组列表底色：卡片浮在略深一层的背景上
        background = if (isDark) surfaceContainerLowest else surfaceContainer,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
    )
}

@Composable
fun BeishuwaTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val useCustomFont = SettingsRepository.useCustomFont.value

    val mochiTextStyles = remember(useCustomFont) {
        val base = defaultTextStyles()
        if (useCustomFont) base.withFontFamily(AppFonts.custom(context)) else base
    }
    val mochiShapes = remember { defaultShapes() }

    // 全局使用 mochi 的 iOS 外观：系统语义色（浅色蓝、深色亮蓝）与 iOS 控件配色
    MochiIosTheme(
        isDark = dark,
        textStyles = mochiTextStyles,
        shapes = mochiShapes,
        glass = true,
        blur = true,
        glassMaterial = MochiGlassDefaults.Thick,
    ) {
        val mochiColors = MochiTheme.colorScheme
        MaterialTheme(
            colorScheme = mochiColors.toMaterialColors(),
            typography = mochiTextStyles.toMaterialTypography(),
            shapes = mochiShapes.toMaterialShapes(),
        ) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyLarge) {
                content()
            }
        }
    }
}

/** Mochi 风格的悬浮玻璃圆形按钮（返回、更多等）。 */
@Composable
fun MochiGlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MochiButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        style = MochiButtonStyle.Glass,
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(0.dp),
        minHeight = 44.dp,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            // 跟随玻璃按钮前景色，避免深色模式下图标变黑
            tint = ui.feice.mochi.theme.LocalContentColor.current,
            modifier = Modifier.size(20.dp),
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

package com.reciteus2.beishuwa

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

object SettingsRepository {
    private const val PREF = "settings"
    private const val KEY_EXPERIMENTAL = "experimental"
    private const val KEY_CUSTOM_FONT = "custom_font"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_PINCH_ZOOM = "pinch_zoom"

    /** 正文、字词等处允许的最小 / 最大字体缩放倍数 */
    const val MIN_FONT_SCALE = 0.85f
    const val MAX_FONT_SCALE = 1.6f

    val experimental = mutableStateOf(false)

    /** 是否使用 wordtype/Regular.ttf 自定义字体（默认开启） */
    val useCustomFont = mutableStateOf(true)

    /** 阅读字体缩放倍数，1f 为默认大小 */
    val fontScale = mutableFloatStateOf(1f)

    /** 是否开启双指缩放调整字体大小 */
    val pinchZoomEnabled = mutableStateOf(true)

    /** 当前是否正在双指缩放（用于显示悬浮字号提示） */
    val pinchActive = mutableStateOf(false)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        experimental.value = prefs.getBoolean(KEY_EXPERIMENTAL, false)
        useCustomFont.value = prefs.getBoolean(KEY_CUSTOM_FONT, true)
        fontScale.floatValue = prefs.getFloat(KEY_FONT_SCALE, 1f)
            .coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        pinchZoomEnabled.value = prefs.getBoolean(KEY_PINCH_ZOOM, true)
    }

    fun setExperimental(context: Context, value: Boolean) {
        experimental.value = value
        prefs(context).edit().putBoolean(KEY_EXPERIMENTAL, value).apply()
    }

    fun setCustomFont(context: Context, value: Boolean) {
        useCustomFont.value = value
        prefs(context).edit().putBoolean(KEY_CUSTOM_FONT, value).apply()
    }

    fun setFontScale(context: Context, value: Float) {
        val scale = value.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        fontScale.floatValue = scale
        prefs(context).edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }

    fun setPinchZoom(context: Context, value: Boolean) {
        pinchZoomEnabled.value = value
        prefs(context).edit().putBoolean(KEY_PINCH_ZOOM, value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}

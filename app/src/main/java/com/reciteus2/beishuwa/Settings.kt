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
    private const val KEY_AI_MODEL = "ai_model"
    private const val KEY_AI_BASE_URL = "ai_base_url"
    private const val KEY_AI_API_KEY = "ai_api_key"
    private const val KEY_AI_USE_AAAS = "ai_use_aaas"
    private const val KEY_TABLET_ROTATION = "tablet_rotation"
    private const val KEY_AUDIO_ENABLED = "audio_enabled"
    private const val KEY_AUDIO_SOURCE = "audio_source"
    private const val KEY_SCREENSHOT_FEEDBACK = "screenshot_feedback"

    /** 朗读音源：系统 TTS */
    const val AUDIO_SOURCE_SYSTEM = "system"
    /** 朗读音源：Edge 在线 TTS */
    const val AUDIO_SOURCE_EDGE = "edge"
    /** 朗读音源：内置录音 */
    const val AUDIO_SOURCE_MEDIA = "media"

    /** 可选的 AI 模型标识 */
    const val AI_MODEL_FLASH = "deepseek-v4.1-flash-1017"
    const val AI_MODEL_PRO = "deepseek-v4.1-pro-1017"
    const val DEFAULT_AI_BASE_URL = "https://api.deepseek.com/v1"

    /** 正文、字词等处允许的最小 / 最大字体缩放倍数 */
    const val MIN_FONT_SCALE = 0.85f
    const val MAX_FONT_SCALE = 1.6f

    val experimental = mutableStateOf(false)

    /** 是否开启正文音频朗读（默认开启，已从实验性功能移出） */
    val audioEnabled = mutableStateOf(true)

    /** 朗读音源，默认系统 TTS */
    val audioSource = mutableStateOf(AUDIO_SOURCE_SYSTEM)

    /** 截图后是否提示反馈问题（默认开启） */
    val screenshotFeedbackEnabled = mutableStateOf(true)

    /** 是否使用 wordtype/Regular.ttf 自定义字体（默认开启） */
    val useCustomFont = mutableStateOf(true)

    /** 阅读字体缩放倍数，1f 为默认大小 */
    val fontScale = mutableFloatStateOf(1f)

    /** 是否开启双指缩放调整字体大小 */
    val pinchZoomEnabled = mutableStateOf(true)

    /** 当前是否正在双指缩放（用于显示悬浮字号提示） */
    val pinchActive = mutableStateOf(false)

    /** 平板是否允许横竖屏自由切换（实验性） */
    val tabletRotation = mutableStateOf(false)

    /** 是否按 AAAS 题库规范出题（实验性，默认关闭） */
    val useAaasSpec = mutableStateOf(false)

    /** 出题使用的 AI 模型 */
    val aiModel = mutableStateOf(AI_MODEL_FLASH)

    /** OpenAI 兼容接口地址（不含 /chat/completions） */
    val aiBaseUrl = mutableStateOf(DEFAULT_AI_BASE_URL)

    /** 接口密钥，仅保存在本机 */
    val aiApiKey = mutableStateOf("")

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        experimental.value = prefs.getBoolean(KEY_EXPERIMENTAL, false)
        audioEnabled.value = prefs.getBoolean(KEY_AUDIO_ENABLED, true)
        audioSource.value = prefs.getString(KEY_AUDIO_SOURCE, AUDIO_SOURCE_SYSTEM) ?: AUDIO_SOURCE_SYSTEM
        screenshotFeedbackEnabled.value = prefs.getBoolean(KEY_SCREENSHOT_FEEDBACK, true)
        useCustomFont.value = prefs.getBoolean(KEY_CUSTOM_FONT, true)
        fontScale.floatValue = prefs.getFloat(KEY_FONT_SCALE, 1f)
            .coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        pinchZoomEnabled.value = prefs.getBoolean(KEY_PINCH_ZOOM, true)
        aiModel.value = prefs.getString(KEY_AI_MODEL, AI_MODEL_FLASH) ?: AI_MODEL_FLASH
        aiBaseUrl.value = prefs.getString(KEY_AI_BASE_URL, DEFAULT_AI_BASE_URL) ?: DEFAULT_AI_BASE_URL
        aiApiKey.value = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        useAaasSpec.value = prefs.getBoolean(KEY_AI_USE_AAAS, false)
        tabletRotation.value = prefs.getBoolean(KEY_TABLET_ROTATION, false)
    }

    fun setExperimental(context: Context, value: Boolean) {
        experimental.value = value
        prefs(context).edit().putBoolean(KEY_EXPERIMENTAL, value).apply()
    }

    fun setAudioEnabled(context: Context, value: Boolean) {
        audioEnabled.value = value
        prefs(context).edit().putBoolean(KEY_AUDIO_ENABLED, value).apply()
    }

    fun setAudioSource(context: Context, value: String) {
        audioSource.value = value
        prefs(context).edit().putString(KEY_AUDIO_SOURCE, value).apply()
    }

    fun setScreenshotFeedback(context: Context, value: Boolean) {
        screenshotFeedbackEnabled.value = value
        prefs(context).edit().putBoolean(KEY_SCREENSHOT_FEEDBACK, value).apply()
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

    fun setAiModel(context: Context, value: String) {
        aiModel.value = value
        prefs(context).edit().putString(KEY_AI_MODEL, value).apply()
    }

    fun setAiBaseUrl(context: Context, value: String) {
        aiBaseUrl.value = value
        prefs(context).edit().putString(KEY_AI_BASE_URL, value).apply()
    }

    fun setAiApiKey(context: Context, value: String) {
        aiApiKey.value = value
        prefs(context).edit().putString(KEY_AI_API_KEY, value).apply()
    }

    fun setUseAaasSpec(context: Context, value: Boolean) {
        useAaasSpec.value = value
        prefs(context).edit().putBoolean(KEY_AI_USE_AAAS, value).apply()
    }

    fun setTabletRotation(context: Context, value: Boolean) {
        tabletRotation.value = value
        prefs(context).edit().putBoolean(KEY_TABLET_ROTATION, value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}

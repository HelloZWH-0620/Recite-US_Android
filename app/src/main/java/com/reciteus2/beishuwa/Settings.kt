package com.reciteus2.beishuwa

import android.content.Context
import androidx.compose.runtime.mutableStateOf

object SettingsRepository {
    private const val PREF = "settings"
    private const val KEY_EXPERIMENTAL = "experimental"

    val experimental = mutableStateOf(false)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        experimental.value = prefs.getBoolean(KEY_EXPERIMENTAL, false)
    }

    fun setExperimental(context: Context, value: Boolean) {
        experimental.value = value
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EXPERIMENTAL, value)
            .apply()
    }
}

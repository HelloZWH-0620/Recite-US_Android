package com.reciteus2.beishuwa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableIntStateOf

/**
 * 截图纠错：记录用户截图事件（由 MainActivity 的系统截屏回调触发），
 * 并在用户点击提示时复制群号、跳转 QQ 用户群。
 */
object ScreenshotFeedback {
    const val QQ_GROUP_NUMBER = "950739151"

    private const val TAG = "ScreenshotFeedback"
    private const val QQ_PACKAGE = "com.tencent.mobileqq"
    private const val QQ_GROUP_CARD_URI =
        "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$QQ_GROUP_NUMBER&card_type=group&source=qrcode"

    /** 每次截图自增，Compose 侧监听该值弹出提示 */
    val captureCount = mutableIntStateOf(0)

    fun onScreenCaptured() {
        if (!SettingsRepository.screenshotFeedbackEnabled.value) return
        captureCount.intValue++
        Log.i(TAG, "screenshot captured, count=${captureCount.intValue}")
    }

    /** 复制群号并跳转 QQ 用户群；QQ 不可用时提示用户手动打开 */
    fun reportProblem(context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("QQ 群号", QQ_GROUP_NUMBER))

        val groupIntent = Intent(Intent.ACTION_VIEW, Uri.parse(QQ_GROUP_CARD_URI))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val qqIntent = context.packageManager
            .getLaunchIntentForPackage(QQ_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val opened = runCatching { context.startActivity(groupIntent) }.isSuccess ||
            (qqIntent != null && runCatching { context.startActivity(qqIntent) }.isSuccess)
        if (!opened) {
            Toast.makeText(context, "群号已复制，请打开 QQ 加入群聊", Toast.LENGTH_LONG).show()
        }
    }
}

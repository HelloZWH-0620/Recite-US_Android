package com.reciteus2.beishuwa

import android.content.Context

/**
 * 朗读控制入口：实际播放由 [PlaybackService] 前台服务负责，
 * 支持熄屏播放、单曲循环、通知栏/锁屏播放控制与进度拖动。
 */
class AudioPlayback(
    private val context: Context,
    private val onFinished: () -> Unit,
) {
    val isPlaying: Boolean
        get() = PlaybackService.isPlaying

    fun start(assetName: String, title: String, author: String) {
        PlaybackService.start(context, assetName, title, author)
    }

    fun toggle() {
        PlaybackService.toggle(context)
    }

    fun stop() {
        PlaybackService.stop(context)
    }

    val title: String
        get() = PlaybackService.currentTitle
}

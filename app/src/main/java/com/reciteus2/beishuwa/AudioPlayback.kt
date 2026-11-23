package com.reciteus2.beishuwa

import android.content.Context

/**
 * 朗读控制入口：实际播放由 [PlaybackService] 前台服务负责，
 * 支持熄屏播放、通知栏/锁屏播放控制；TTS 音源逐句朗读并同步媒体卡片句子。
 */
class AudioPlayback(
    private val context: Context,
    private val onFinished: () -> Unit,
) {
    val isPlaying: Boolean
        get() = PlaybackService.isPlaying

    fun start(
        assetName: String?,
        filePath: String?,
        source: String,
        sentences: List<String>,
        title: String,
        author: String,
    ) {
        PlaybackService.start(context, assetName, filePath, source, sentences, title, author)
    }

    fun toggle() {
        PlaybackService.toggle(context)
    }

    fun stop() {
        PlaybackService.stop(context)
    }

    fun seekTo(positionMs: Long) {
        PlaybackService.seekTo(context, positionMs)
    }

    val title: String
        get() = PlaybackService.currentTitle
}

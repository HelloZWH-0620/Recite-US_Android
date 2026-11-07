package com.reciteus2.beishuwa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver

/**
 * 实验性朗读：播放内置音频，并通过系统媒体中心（MediaSession + MediaStyle 通知）控制。
 * 播完单篇即停止。
 */
class AudioPlayback(
    private val context: Context,
    private val onFinished: () -> Unit,
) {
    private var player: MediaPlayer? = null
    private var session: MediaSessionCompat? = null
    private var currentTitle: String = ""
    private var currentAuthor: String = ""

    companion object {
        private const val CHANNEL_ID = "beishuwa_playback"
        private const val NOTIFICATION_ID = 2026
    }

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    fun start(assetName: String, title: String, author: String) {
        release()

        val mp = MediaPlayer()
        try {
            context.assets.openFd(assetName).use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.prepare()
        } catch (e: Exception) {
            // 资源缺失或解码失败：释放半初始化的对象后抛给调用方提示
            try { mp.release() } catch (ignored: Exception) {}
            player = null
            session?.release()
            session = null
            throw e
        }
        player = mp

        val mediaSession = MediaSessionCompat(context, "BeishuwaPlayback").apply {
            setPlaybackState(buildState(PlaybackStateCompat.STATE_PLAYING))
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    player?.start()
                    setPlaybackState(buildState(PlaybackStateCompat.STATE_PLAYING))
                    postNotification()
                }

                override fun onPause() {
                    player?.pause()
                    setPlaybackState(buildState(PlaybackStateCompat.STATE_PAUSED))
                    postNotification()
                }

                override fun onStop() {
                    stop()
                    onFinished()
                }
            })
            isActive = true
        }
        session = mediaSession
        currentTitle = title
        currentAuthor = author

        mp.setOnCompletionListener {
            release()
            onFinished()
        }
        mp.start()
        postNotification()
    }

    fun toggle() {
        val mp = player ?: return
        if (mp.isPlaying) {
            mp.pause()
            session?.setPlaybackState(buildState(PlaybackStateCompat.STATE_PAUSED))
        } else {
            mp.start()
            session?.setPlaybackState(buildState(PlaybackStateCompat.STATE_PLAYING))
        }
        postNotification()
    }

    fun stop() {
        release()
    }

    private fun buildState(playbackState: Int): PlaybackStateCompat =
        PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_STOP
            )
            .setState(playbackState, player?.currentPosition?.toLong() ?: 0L, 1f)
            .build()

    private fun postNotification() {
        val mediaSession = session ?: return
        val playing = isPlaying

        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "朗读播放",
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
        // API 33+ 未授予通知权限时直接跳过，避免 SecurityException
        if (Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!manager.areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleAction = if (playing) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "暂停",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context, PlaybackStateCompat.ACTION_PAUSE
                ),
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "播放",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context, PlaybackStateCompat.ACTION_PLAY
                ),
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_play)
            .setContentTitle(currentTitle)
            .setContentText(currentAuthor)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(toggleAction)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun release() {
        // 只调用 release()：任何状态下都安全；stop() 在部分状态会抛 IllegalStateException
        player?.release()
        player = null
        session?.release()
        session = null
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}

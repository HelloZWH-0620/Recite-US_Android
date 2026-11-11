package com.reciteus2.beishuwa

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver

/**
 * 朗读前台服务：负责熄屏播放、单曲循环、通知栏/锁屏媒体控制与进度拖动。
 */
class PlaybackService : Service() {

    private var player: MediaPlayer? = null
    private var session: MediaSessionCompat? = null
    private var title: String = ""
    private var author: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (player != null) {
                publishState()
                postNotification()
                handler.postDelayed(this, 1000L)
            }
        }
    }

    companion object {
        const val ACTION_START = "com.reciteus2.beishuwa.action.START"
        const val ACTION_TOGGLE = "com.reciteus2.beishuwa.action.TOGGLE"
        const val ACTION_STOP = "com.reciteus2.beishuwa.action.STOP"
        const val ACTION_SEEK = "com.reciteus2.beishuwa.action.SEEK"
        const val EXTRA_ASSET = "asset"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AUTHOR = "author"
        const val EXTRA_POSITION = "position"

        private const val CHANNEL_ID = "beishuwa_playback"
        private const val NOTIFICATION_ID = 2026

        @Volatile
        var isPlaying: Boolean = false
            private set

        @Volatile
        var currentTitle: String = ""
            private set

        fun start(context: Context, assetName: String, title: String, author: String) {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ASSET, assetName)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_AUTHOR, author)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun toggle(context: Context) {
            context.startService(Intent(context, PlaybackService::class.java).apply { action = ACTION_TOGGLE })
        }

        fun stop(context: Context) {
            context.startService(Intent(context, PlaybackService::class.java).apply { action = ACTION_STOP })
        }

        fun seekTo(context: Context, positionMs: Long) {
            context.startService(
                Intent(context, PlaybackService::class.java).apply {
                    action = ACTION_SEEK
                    putExtra(EXTRA_POSITION, positionMs)
                }
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val mediaSession = MediaSessionCompat(this, "BeishuwaPlayback").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    player?.start()
                    isPlaying = true
                    publishState()
                    postNotification()
                }

                override fun onPause() {
                    player?.pause()
                    isPlaying = false
                    publishState()
                    postNotification()
                }

                override fun onStop() {
                    stopPlayback()
                }

                override fun onSeekTo(pos: Long) {
                    player?.seekTo(pos.toInt())
                    publishState()
                    postNotification()
                }
            })
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setMediaButtonReceiver(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@PlaybackService,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE,
                )
            )
            setSessionActivity(
                PendingIntent.getActivity(
                    this@PlaybackService,
                    0,
                    Intent(this@PlaybackService, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            isActive = true
        }
        session = mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                author = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
                currentTitle = title
                val asset = intent.getStringExtra(EXTRA_ASSET)
                if (asset != null) {
                    ensureForeground()
                    startPlayback(asset)
                }
            }

            ACTION_TOGGLE -> {
                val mp = player
                if (mp == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (mp.isPlaying) {
                    mp.pause()
                    isPlaying = false
                } else {
                    mp.start()
                    isPlaying = true
                }
                publishState()
                postNotification()
            }

            ACTION_SEEK -> {
                player?.seekTo(intent.getLongExtra(EXTRA_POSITION, 0L).toInt())
                publishState()
                postNotification()
            }

            ACTION_STOP -> {
                stopPlayback()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun startPlayback(assetName: String) {
        releasePlayer()
        val mp = MediaPlayer()
        assets.openFd(assetName).use { afd ->
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        }
        mp.isLooping = true
        mp.prepare()
        mp.start()
        player = mp
        isPlaying = true

        session?.apply {
            setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, author)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mp.duration.toLong())
                    .build()
            )
            isActive = true
        }
        publishState()
        ensureForeground()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun publishState() {
        val mp = player ?: return
        val state = when {
            mp.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        val actions = PlaybackStateCompat.ACTION_PLAY
            .or(PlaybackStateCompat.ACTION_PAUSE)
            .or(PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .or(PlaybackStateCompat.ACTION_STOP)
            .or(PlaybackStateCompat.ACTION_SEEK_TO)
        session?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, mp.currentPosition.toLong(), if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f)
                .build()
        )
    }

    private fun ensureForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val mediaSession = session
        val playing = player?.isPlaying == true
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val playPause = if (playing) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "暂停",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE),
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "播放",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY),
            )
        }
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "停止",
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP),
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_play)
            .setContentTitle(title)
            .setContentText(author)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setColorized(true)
            .setColor(accentColor())
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
            .addAction(playPause)
            .addAction(stopAction)

        session?.let { builder.setStyle(MediaStyle().setMediaSession(it.sessionToken).setShowActionsInCompactView(0, 1)) }

        player?.let { mp ->
            val duration = mp.duration.coerceAtLeast(0)
            val position = mp.currentPosition.coerceIn(0, duration)
            if (duration > 0) {
                builder.setProgress(duration, position, false)
            }
        }
        return builder.build()
    }

    /** 浅色模式天蓝、深色模式金黄，与 App 主题保持一致 */
    private fun accentColor(): Int {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (night) 0xFFFFC83D.toInt() else 0xFF0277BD.toInt()
    }

    private fun postNotification() {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun stopPlayback() {
        handler.removeCallbacks(ticker)
        releasePlayer()
        isPlaying = false
        session?.isActive = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun releasePlayer() {
        player?.let {
            runCatching { if (it.isPlaying) it.stop() }
            it.release()
        }
        player = null
        isPlaying = false
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(this).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "朗读播放", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        releasePlayer()
        session?.release()
        session = null
        isPlaying = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

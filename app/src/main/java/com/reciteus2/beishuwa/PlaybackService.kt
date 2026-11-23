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
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 朗读前台服务：负责熄屏播放、通知栏/锁屏媒体控制。
 *
 * 支持三种音源：
 * - [SettingsRepository.AUDIO_SOURCE_MEDIA]：内置录音，整段循环播放，可拖动进度；
 * - [SettingsRepository.AUDIO_SOURCE_SYSTEM]：系统 TTS，逐句朗读；
 * - [SettingsRepository.AUDIO_SOURCE_EDGE]：Edge 在线 TTS，逐句合成后播放。
 *
 * TTS 模式下通知/媒体卡片展示当前正在朗读的句子，播放位置用「第 i/n 句」表示。
 */
class PlaybackService : Service() {

    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var session: MediaSessionCompat? = null

    private var articleTitle: String = ""
    private var articleAuthor: String = ""
    private var source: String = SettingsRepository.AUDIO_SOURCE_MEDIA
    private var sentences: List<String> = emptyList()
    private var index: Int = 0
    private var pausedByUser: Boolean = false

    private var cardTitle: String = ""
    private var cardSubtitle: String = ""

    /** Edge 合成的任务代次，切换/停止后丢弃旧的合成结果 */
    private var edgeGeneration: Int = 0
    private val edgeExecutor = Executors.newSingleThreadExecutor()

    private val handler = Handler(Looper.getMainLooper())
    private var tickCount = 0

    private val ticker = object : Runnable {
        override fun run() {
            if (player != null) {
                // 进度条需要更细的位置更新，通知仍然每秒刷新一次
                publishState()
                tickCount++
                if (tickCount % 4 == 0) postNotification()
                handler.postDelayed(this, 250L)
            }
        }
    }

    companion object {
        const val ACTION_START = "com.reciteus2.beishuwa.action.START"
        const val ACTION_TOGGLE = "com.reciteus2.beishuwa.action.TOGGLE"
        const val ACTION_STOP = "com.reciteus2.beishuwa.action.STOP"
        const val ACTION_SEEK = "com.reciteus2.beishuwa.action.SEEK"
        const val EXTRA_ASSET = "asset"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_SENTENCES = "sentences"
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

        /** 当前播放位置（毫秒），供页面进度条读取 */
        @Volatile
        var currentPositionMs: Long = 0L
            private set

        /** 当前音频总时长（毫秒） */
        @Volatile
        var durationMs: Long = 0L
            private set

        /** 当前生效的音源（内置录音缺失时可能从 media 自动降级为 edge） */
        @Volatile
        var currentSource: String = SettingsRepository.AUDIO_SOURCE_MEDIA
            private set

        /** 当前朗读到第几句（从 0 开始），仅 TTS 模式有意义 */
        @Volatile
        var sentenceIndex: Int = 0
            private set

        /** 朗读句子总数，仅 TTS 模式有意义 */
        @Volatile
        var sentenceCount: Int = 0
            private set

        fun start(
            context: Context,
            assetName: String?,
            source: String,
            sentences: List<String>,
            title: String,
            author: String,
        ) {
            val clean = sentences.filter { it.isNotBlank() }
            currentSource = source
            sentenceIndex = 0
            sentenceCount = clean.size
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ASSET, assetName)
                putExtra(EXTRA_SOURCE, source)
                putStringArrayListExtra(EXTRA_SENTENCES, ArrayList(clean))
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
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onStop() {
                    stopPlayback()
                }

                override fun onSeekTo(pos: Long) {
                    if (source == SettingsRepository.AUDIO_SOURCE_SYSTEM) return
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
                startFromIntent(intent)
            }

            ACTION_TOGGLE -> {
                if (!isPlaying && player == null && tts == null && sentences.isEmpty()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (isPlaying) pausePlayback() else resumePlayback()
            }

            ACTION_SEEK -> {
                if (source == SettingsRepository.AUDIO_SOURCE_SYSTEM) return START_STICKY
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

    private fun startFromIntent(intent: Intent) {
        articleTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        articleAuthor = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
        source = intent.getStringExtra(EXTRA_SOURCE) ?: SettingsRepository.AUDIO_SOURCE_MEDIA
        sentences = intent.getStringArrayListExtra(EXTRA_SENTENCES)
            ?.filter { it.isNotBlank() }
            .orEmpty()
        index = 0
        pausedByUser = false
        sentenceIndex = 0
        sentenceCount = sentences.size
        currentSource = source
        cardTitle = articleTitle
        cardSubtitle = articleAuthor
        currentTitle = articleTitle

        ensureForeground()

        val asset = intent.getStringExtra(EXTRA_ASSET)
        when (source) {
            SettingsRepository.AUDIO_SOURCE_SYSTEM -> startSystemPlayback()
            SettingsRepository.AUDIO_SOURCE_EDGE -> startEdgePlayback()
            else -> {
                if (asset != null && assetExists(asset)) {
                    startMediaPlayback(asset)
                } else if (sentences.isNotEmpty()) {
                    // 没有内置录音时自动改用 Edge 在线朗读
                    source = SettingsRepository.AUDIO_SOURCE_EDGE
                    currentSource = source
                    startEdgePlayback()
                } else {
                    stopPlayback()
                }
            }
        }
    }

    // ---------------- 内置录音 ----------------

    private fun startMediaPlayback(assetName: String) {
        releaseTts()
        edgeGeneration++
        releasePlayer()

        val mp = MediaPlayer()
        try {
            assets.openFd(assetName).use { afd ->
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            mp.isLooping = true
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            runCatching { mp.release() }
            stopPlayback()
            return
        }

        player = mp
        isPlaying = true
        currentPositionMs = 0L
        durationMs = mp.duration.toLong()
        cardTitle = articleTitle
        cardSubtitle = articleAuthor
        currentTitle = articleTitle
        sentenceIndex = 0
        sentenceCount = 0

        session?.apply {
            isActive = true
            setMetadata(buildMetadata())
        }
        publishState()
        ensureForeground()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    // ---------------- 系统 TTS ----------------

    private fun startSystemPlayback() {
        releasePlayer()
        releaseTts()
        edgeGeneration++
        index = 0
        isPlaying = false
        cardSubtitle = "系统朗读 · $articleTitle"
        setSentencesCard()
        session?.apply {
            isActive = true
            setMetadata(buildMetadata())
        }
        publishTtsState()

        val engine = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                stopPlayback()
                return@TextToSpeech
            }
            val e = tts ?: return@TextToSpeech
            runCatching { e.language = Locale.SIMPLIFIED_CHINESE }
            e.setOnUtteranceProgressListener(utteranceListener)
            if (!pausedByUser) speakCurrentSentence()
        }
        tts = engine
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            isPlaying = true
            publishTtsState()
            postNotification()
        }

        override fun onDone(utteranceId: String?) {
            handler.post {
                if (source != SettingsRepository.AUDIO_SOURCE_SYSTEM || pausedByUser) return@post
                index = if (index + 1 >= sentences.size) 0 else index + 1
                speakCurrentSentence()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            handler.post { stopPlayback() }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            handler.post { stopPlayback() }
        }
    }

    private fun speakCurrentSentence() {
        val e = tts ?: return
        val text = sentences.getOrNull(index) ?: return
        sentenceIndex = index
        setSentencesCard()
        cardTitle = text
        currentTitle = text
        isPlaying = true
        e.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sys-$index-${System.nanoTime()}")
        session?.setMetadata(buildMetadata())
        publishTtsState()
        postNotification()
    }

    // ---------------- Edge 在线 TTS ----------------

    private fun startEdgePlayback() {
        releasePlayer()
        releaseTts()
        index = 0
        pausedByUser = false
        edgeGeneration++
        isPlaying = true
        cardSubtitle = "Edge 朗读 · $articleTitle"
        setSentencesCard()
        session?.apply {
            isActive = true
            setMetadata(buildMetadata())
        }
        publishTtsState()
        playEdgeSentence()
    }

    private fun playEdgeSentence() {
        val text = sentences.getOrNull(index) ?: return
        sentenceIndex = index
        setSentencesCard()
        cardTitle = text
        currentTitle = text
        isPlaying = true
        session?.setMetadata(buildMetadata())
        publishTtsState()
        postNotification()

        val generation = edgeGeneration
        val cacheFile = edgeCacheFile(text)
        edgeExecutor.execute {
            val ok = EdgeTts.synthesize(text, cacheFile)
            handler.post {
                if (generation != edgeGeneration || source != SettingsRepository.AUDIO_SOURCE_EDGE) return@post
                if (!ok) {
                    stopPlayback()
                } else {
                    playEdgeFile(cacheFile)
                }
            }
        }
    }

    private fun edgeCacheFile(text: String): File {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { "%02x".format(it) }
        val dir = File(cacheDir, "edge_tts")
        dir.mkdirs()
        return File(dir, "$name.mp3")
    }

    private fun playEdgeFile(file: File) {
        releasePlayer()
        val mp = MediaPlayer()
        try {
            mp.setDataSource(file.absolutePath)
            mp.prepare()
        } catch (e: Exception) {
            runCatching { mp.release() }
            handler.post { stopPlayback() }
            return
        }
        player = mp
        durationMs = mp.duration.toLong()
        currentPositionMs = 0L
        if (!pausedByUser) mp.start()
        isPlaying = !pausedByUser

        mp.setOnCompletionListener {
            handler.post {
                if (player !== mp) return@post
                if (source != SettingsRepository.AUDIO_SOURCE_EDGE || pausedByUser) return@post
                index = if (index + 1 >= sentences.size) 0 else index + 1
                playEdgeSentence()
            }
        }

        publishState()
        postNotification()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    // ---------------- 播放控制 ----------------

    private fun pausePlayback() {
        pausedByUser = true
        when (source) {
            SettingsRepository.AUDIO_SOURCE_SYSTEM -> runCatching { tts?.stop() }
            else -> runCatching { player?.pause() }
        }
        isPlaying = false
        if (source == SettingsRepository.AUDIO_SOURCE_SYSTEM) publishTtsState() else publishState()
        postNotification()
    }

    private fun resumePlayback() {
        pausedByUser = false
        when (source) {
            SettingsRepository.AUDIO_SOURCE_SYSTEM -> speakCurrentSentence()
            else -> {
                runCatching { player?.start() }
                isPlaying = player != null
                publishState()
                postNotification()
            }
        }
    }

    private fun setSentencesCard() {
        cardSubtitle = when (source) {
            SettingsRepository.AUDIO_SOURCE_SYSTEM -> "系统朗读 · $articleTitle"
            SettingsRepository.AUDIO_SOURCE_EDGE -> "Edge 朗读 · $articleTitle"
            else -> articleAuthor
        }
    }

    private fun publishState() {
        val mp = player
        if (mp != null) {
            currentPositionMs = mp.currentPosition.toLong()
            durationMs = mp.duration.toLong()
        }
        val playing = if (mp != null) mp.isPlaying else isPlaying
        isPlaying = playing
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY
            .or(PlaybackStateCompat.ACTION_PAUSE)
            .or(PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .or(PlaybackStateCompat.ACTION_STOP)
            .or(PlaybackStateCompat.ACTION_SEEK_TO)
        session?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, mp?.currentPosition?.toLong() ?: 0L, if (playing) 1f else 0f)
                .build()
        )
    }

    private fun publishTtsState() {
        val actions = PlaybackStateCompat.ACTION_PLAY
            .or(PlaybackStateCompat.ACTION_PAUSE)
            .or(PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .or(PlaybackStateCompat.ACTION_STOP)
        session?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    0L,
                    if (isPlaying) 1f else 0f,
                )
                .build()
        )
    }

    private fun buildMetadata(): MediaMetadataCompat =
        MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, cardTitle.ifBlank { articleTitle })
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, cardSubtitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, articleTitle)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
            .build()

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
        val playing = isPlaying
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
            .setContentTitle(cardTitle.ifBlank { articleTitle }.take(120))
            .setContentText(cardSubtitle)
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

        val mp = player
        if (mp != null && source == SettingsRepository.AUDIO_SOURCE_MEDIA) {
            val duration = mp.duration.coerceAtLeast(0)
            val position = mp.currentPosition.coerceIn(0, duration)
            if (duration > 0) {
                builder.setProgress(duration, position, false)
            }
        } else if (source != SettingsRepository.AUDIO_SOURCE_MEDIA && sentences.isNotEmpty()) {
            builder.setSubText("第 ${sentenceIndex + 1}/${sentences.size} 句")
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
        // Android 13+ 没有通知权限时 notify() 会抛 SecurityException，
        // areNotificationsEnabled() 不足以覆盖权限被拒的情况，这里显式检查并兜底。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
    }

    private fun stopPlayback() {
        handler.removeCallbacks(ticker)
        edgeGeneration++
        releasePlayer()
        releaseTts()
        isPlaying = false
        pausedByUser = false
        sentenceIndex = 0
        session?.isActive = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun releasePlayer() {
        player?.let {
            runCatching { if (it.isPlaying) it.stop() }
            it.setOnCompletionListener(null)
            it.release()
        }
        player = null
        currentPositionMs = 0L
        durationMs = 0L
    }

    private fun releaseTts() {
        tts?.let {
            runCatching { it.stop() }
            runCatching { it.shutdown() }
        }
        tts = null
    }

    private fun assetExists(assetName: String): Boolean =
        runCatching { assets.openFd(assetName).use { } }.isSuccess

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(this).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "朗读播放", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        edgeGeneration++
        releasePlayer()
        releaseTts()
        session?.release()
        session = null
        isPlaying = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

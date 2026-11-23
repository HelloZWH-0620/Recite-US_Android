package com.reciteus2.beishuwa

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.delay
import ui.feice.mochi.basic.MochiBarGroup
import ui.feice.mochi.basic.MochiBarIconButton
import ui.feice.mochi.basic.MochiBottomToolbar
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiCheckbox
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiPopoverButton
import ui.feice.mochi.basic.MochiSlider
import ui.feice.mochi.basic.MochiSliderDefaults
import ui.feice.mochi.basic.MochiToolbarScaffold
import ui.feice.mochi.theme.MochiTheme

private fun audioAssetExists(context: Context, articleId: String): Boolean =
    try {
        context.assets.list("audio")?.contains("$articleId.mp3") == true
    } catch (e: Exception) {
        false
    }

/** 把正文切成适合逐句朗读的短句：按句号、问号、叹号、分号与换行断开 */
private fun splitSpeechSentences(text: String): List<String> {
    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    fun flush() {
        val sentence = buffer.toString().trim()
        if (sentence.isNotEmpty()) result.add(sentence)
        buffer.clear()
    }
    for (ch in text) {
        if (ch == '\n' || ch == '\r') {
            flush()
            continue
        }
        buffer.append(ch)
        if (ch in "。！？；!?;") flush()
    }
    flush()
    return result
}

@Composable
fun ArticleDetailScreen(
    article: Article,
    onBack: () -> Unit,
    onOpenAuthor: (Author?) -> Unit,
) {
    val context = LocalContext.current
    val data = remember(context) { ReciteRepository.load(context) }
    val author = remember(article) { data.authorsById[article.authorId] }

    var notesOn by rememberSaveable { mutableStateOf(false) }
    var translationOn by rememberSaveable { mutableStateOf(false) }
    // 正文字号缩放：100% ~ 125%，按 5% 一档
    var textZoomPercent by rememberSaveable { mutableIntStateOf(100) }


    val sections = remember(article) { splitSections(article.text) }
    val translationParts = remember(article, sections) { splitTranslation(article.translation, sections.size) }
    val pairedTranslation = remember(sections, translationParts) {
        pairTranslation(sections, translationParts)
    }
    val sectionNotes = remember(article, sections) { assignNotes(sections, article.notes) }

    // 正文统一字号；作品详情沿用它，只靠字重和颜色区分层级
    val textZoom = textZoomPercent / 100f
    val bodyFontSize = (18f * textZoom).sp
    val bodyLineHeight = (32f * textZoom).sp
    // 注释、翻译跟随正文字号等比缩放
    val translationFontSize = (15f * textZoom).sp
    val translationLineHeight = (24f * textZoom).sp
    val noteFontSize = (14f * textZoom).sp
    val noteLineHeight = (22f * textZoom).sp

    // 说明：朗读统一由 PlaybackService 承担，支持系统 TTS / Edge TTS / 内置录音三种音源。

    // 音频朗读：普通启用项（在「我的」中开关），音源与句子切分后交给前台服务
    val audioEnabled = SettingsRepository.audioEnabled.value
    val audioSource by remember { SettingsRepository.audioSource }
    var audioPlaying by remember { mutableStateOf(false) }
    var audioExpanded by remember { mutableStateOf(false) }
    // 是否已有播放器实例；暂停不销毁，只有自动收起/停止才重置
    var audioStarted by remember { mutableStateOf(false) }
    val audioPlayer = remember { AudioPlayback(context) { audioPlaying = false } }
    val audioAvailable = remember(article.id) { audioAssetExists(context, article.id) }
    val speechSentences = remember(article) { splitSpeechSentences(article.text) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    // 同步通知栏/锁屏对播放状态的操作
    LaunchedEffect(Unit) {
        while (true) {
            delay(700)
            audioPlaying = audioPlayer.isPlaying
        }
    }

    val toggleAudio: () -> Unit = {
        if (audioStarted) {
            // 播放/暂停切换，保留当前进度/当前句
            audioPlayer.toggle()
            audioPlaying = !audioPlaying
        } else {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            // 音频缺失/损坏时 start() 可能抛异常，避免点击直接崩溃
            runCatching {
                audioPlayer.start(
                    assetName = if (audioAvailable) "audio/${article.id}.mp3" else null,
                    source = audioSource,
                    sentences = speechSentences,
                    title = article.title,
                    author = article.author,
                )
                audioPlaying = true
                audioStarted = true
            }.onFailure {
                audioPlaying = false
                Toast.makeText(context, "朗读播放失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 底部工具栏：播放/暂停，首次点击展开进度条
    val onToolbarPlayPause: () -> Unit = {
        if (!audioExpanded) audioExpanded = true
        toggleAudio()
    }

    // 7 秒无操作后收起进度条，同时结束播放（媒体卡片消失），下次从头播放
    val onAudioAutoCollapse: () -> Unit = {
        audioPlayer.stop()
        audioPlaying = false
        audioStarted = false
        audioExpanded = false
    }

    val onAudioSeek: (Long) -> Unit = { positionMs ->
        audioPlayer.seekTo(positionMs)
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val pinchEnabled = SettingsRepository.pinchZoomEnabled.value

    if (isLandscape) {
        AppFontScaled { Box(Modifier.fillMaxSize().pinchToFontScale(pinchEnabled, context)) {
        LandscapeArticleDetail(
            article = article,
            author = author,
            sections = sections,
            pairedTranslation = pairedTranslation,
            sectionNotes = sectionNotes,
            audioEnabled = audioEnabled,
            audioPlaying = audioPlaying,
            onToggleAudio = toggleAudio,
            onBack = onBack,
            onOpenAuthor = onOpenAuthor,
        )
        } }
        return
    }

    val portraitScroll = rememberScrollState()
    val collapseDistancePx = with(LocalDensity.current) { 56.dp.toPx() }
    val compactTitleAlpha = (portraitScroll.value / collapseDistancePx).coerceIn(0f, 1f)

    AppFontScaled {
        MochiToolbarScaffold(
            background = MaterialTheme.colorScheme.background,
            toolbar = {
                ArticleBottomControls(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    audioEnabled = audioEnabled,
                    audioExpanded = audioExpanded,
                    audioPlaying = audioPlaying,
                    onPlayPause = onToolbarPlayPause,
                    onSeek = onAudioSeek,
                    onAutoCollapse = onAudioAutoCollapse,
                    translationOn = translationOn,
                    notesOn = notesOn,
                    onTranslationChange = { translationOn = it },
                    onNotesChange = { notesOn = it },
                    zoomPercent = textZoomPercent,
                    onZoomPercentChange = { textZoomPercent = it },
                )
            },
        ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pinchToFontScale(pinchEnabled, context)
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                // 平板上限制正文宽度，保持舒适的阅读行宽
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.TopCenter)
                .verticalScroll(portraitScroll),
        ) {
            // 固定顶栏的占位
            Spacer(
                Modifier
                    .statusBarsPadding()
                    .height(56.dp),
            )

            Column(Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = article.title,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 26.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    MochiButton(
                        onClick = { onOpenAuthor(author) },
                        style = MochiButtonStyle.Tonal,
                        minHeight = 30.dp,
                        cornerRadius = 15.dp,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = article.author,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                sections.forEachIndexed { index, section ->
                    // 文言文：每段首行缩进；词/曲：仅首段缩进；诗歌：不缩进
                    val indent = when {
                        !article.isPoem -> TextIndent(firstLine = 2.em)
                        article.keepFirstLineIndent && index == 0 -> TextIndent(firstLine = 2.em)
                        else -> TextIndent.None
                    }
                    Text(
                        text = if (notesOn) {
                            highlightSection(
                                section,
                                article.notes,
                                MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            AnnotatedString(section)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = bodyFontSize,
                            lineHeight = bodyLineHeight,
                            color = MaterialTheme.colorScheme.onBackground,
                            textIndent = indent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (translationOn) {
                        pairedTranslation.getOrNull(index)?.let { translation ->
                            GrayCard {
                                Text(
                                    text = translation,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = translationFontSize,
                                    lineHeight = translationLineHeight,
                                )
                            }
                        }
                    }

                    if (notesOn) {
                        val notes = sectionNotes.getOrNull(index).orEmpty()
                        if (notes.isNotEmpty()) {
                            GrayCard {
                                notes.forEachIndexed { noteIndex, note ->
                                    if (noteIndex > 0) Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                                append(note.phrase.ifBlank { note.pos })
                                            }
                                            append("：")
                                            append(note.explain)
                                        },
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontSize = noteFontSize,
                                        lineHeight = noteLineHeight,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                }

                if (article.appreciation.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = MochiTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(
                                text = "作品简介",
                                fontSize = bodyFontSize,
                                lineHeight = bodyLineHeight,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = article.appreciation,
                                fontSize = bodyFontSize,
                                lineHeight = bodyLineHeight,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }

        // 固定顶栏：正文上滑后标题淡入，形成大标题推入标题栏的效果
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MochiGlassIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = article.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = compactTitleAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

    }
        }
    }
}

/**
 * 横屏分屏：左侧固定正文，右侧固定显示「赏析 / 注释 / 翻译」中的一种（标题栏右侧下拉切换）。
 * 左右共用同一个滚动容器，天然同步滚动；段落内容按行配对，
 * 行高取两侧较大者，内容较短的一侧自动留白，形成逐段对齐的视觉效果。
 * 左右各占屏幕实际宽度的一半，分割线始终位于屏幕中心。
 */
@Composable
private fun LandscapeArticleDetail(
    article: Article,
    author: Author?,
    sections: List<String>,
    pairedTranslation: List<String?>,
    sectionNotes: List<List<Note>>,
    audioEnabled: Boolean,
    audioPlaying: Boolean,
    onToggleAudio: () -> Unit,
    onBack: () -> Unit,
    onOpenAuthor: (Author?) -> Unit,
) {
    // 0 赏析 / 1 注释 / 2 翻译
    var rightTab by rememberSaveable { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 16.sp,
        lineHeight = 28.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )
    val tabLabel = when (rightTab) {
        1 -> "注释"
        2 -> "翻译"
        else -> "赏析"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左半区：返回、标题、作者
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MochiGlassIconButton(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack,
                )
                Text(
                    text = article.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TextButton(onClick = { onOpenAuthor(author) }) {
                    Text(article.author, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            // 右半区：显示内容切换按钮，在右半区内水平居中
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                MochiPopoverButton(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                    width = 150.dp,
                    popover = {
                        listOf("赏析", "注释", "翻译").forEachIndexed { index, label ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        rightTab = index
                                        menuExpanded = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = label,
                                    style = MochiTheme.textStyles.body,
                                    color = MochiTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                if (rightTab == index) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MochiTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    },
                    buttonContent = {
                        Text(tabLabel, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = "切换显示内容",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            // 翻译模式：右半区整块底板，所有段落共用一张“卡片”
            if (rightTab == 2) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding(),
            ) {
                Spacer(Modifier.height(8.dp))

                if (rightTab == 0) {
                    // 赏析：左右各一整块，不与正文逐段并列
                    Row(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                                .padding(start = 18.dp, end = 18.dp),
                        ) {
                            sections.forEach { section ->
                                Text(text = section, style = bodyStyle)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                                .padding(start = 18.dp, end = 18.dp),
                        ) {
                            if (article.appreciation.isNotBlank()) {
                                Text(
                                    text = article.appreciation,
                                    fontSize = 16.sp,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    sections.forEachIndexed { index, section ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                text = if (rightTab == 1) {
                                    highlightSection(
                                        section,
                                        article.notes,
                                        MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    AnnotatedString(section)
                                },
                                style = bodyStyle,
                                modifier = Modifier
                                    .weight(1f)
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                                    .padding(start = 18.dp, end = 18.dp),
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
                                    .padding(start = 18.dp, end = 18.dp),
                            ) {
                                when (rightTab) {
                                    1 -> LandscapeNotes(sectionNotes.getOrNull(index).orEmpty())
                                    2 -> LandscapeTranslation(pairedTranslation.getOrNull(index))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
                Spacer(Modifier.height(96.dp))
            }

            // 中缝分割线：位于屏幕实际中心
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            )

            // 朗读按钮：悬浮于左半区下方居中
            if (audioEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.5f),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Surface(
                        onClick = onToggleAudio,
                        shape = RectangleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "朗读",
                            tint = if (audioPlaying) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeNotes(notes: List<Note>) {
    if (notes.isEmpty()) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        // 不带上下内边距，保证与左侧段落首行对齐
        Column(Modifier.padding(horizontal = 12.dp)) {
            notes.forEachIndexed { index, note ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append(note.phrase.ifBlank { note.pos })
                        }
                        append("：")
                        append(note.explain)
                    },
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 16.sp,
                    lineHeight = 28.sp,
                )
            }
        }
    }
}

@Composable
private fun LandscapeTranslation(translation: String?) {
    if (translation.isNullOrBlank()) return
    Text(
        text = translation,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        fontSize = 16.sp,
        lineHeight = 28.sp,
    )
}

@Composable
private fun GrayCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RectangleShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            content = content,
        )
    }
}

/**
 * 正文底部：左侧播放/暂停按钮，点击后向右展开进度条；
 * 展开时「显示」淡出隐藏，暂停 7 秒无触摸自动收起并结束播放。
 */
@Composable
private fun ArticleBottomControls(
    modifier: Modifier,
    audioEnabled: Boolean,
    audioExpanded: Boolean,
    audioPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onAutoCollapse: () -> Unit,
    translationOn: Boolean,
    notesOn: Boolean,
    onTranslationChange: (Boolean) -> Unit,
    onNotesChange: (Boolean) -> Unit,
    zoomPercent: Int,
    onZoomPercentChange: (Int) -> Unit,
) {
    var panelExpanded by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableLongStateOf(0L) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playingSource by remember { mutableStateOf(SettingsRepository.AUDIO_SOURCE_MEDIA) }
    var sentenceIndex by remember { mutableIntStateOf(0) }
    var sentenceCount by remember { mutableIntStateOf(0) }

    // 展开时轮询播放进度与当前音源/句子
    LaunchedEffect(audioExpanded) {
        while (audioExpanded) {
            if (!dragging) positionMs = PlaybackService.currentPositionMs
            durationMs = PlaybackService.durationMs
            playingSource = PlaybackService.currentSource
            sentenceIndex = PlaybackService.sentenceIndex
            sentenceCount = PlaybackService.sentenceCount
            delay(250L)
        }
    }

    // 暂停后 7 秒内没有触摸播放组件，则自动收起并结束播放
    LaunchedEffect(audioExpanded, audioPlaying, interaction) {
        if (audioExpanded && !audioPlaying) {
            delay(7000L)
            onAutoCollapse()
        }
    }

    val displayedPositionMs = if (dragging) dragPositionMs else positionMs
    val totalMs = durationMs.coerceAtLeast(1L)

    MochiBottomToolbar(
        modifier = modifier,
        // 底部毛玻璃/渐隐遮罩：用页面背景色，高度随工具栏（按钮 + 上下内边距 + 导航栏）
        edgeColor = MaterialTheme.colorScheme.background,
    ) {
        if (audioEnabled) {
            MochiButton(
                onClick = {
                    interaction++
                    onPlayPause()
                },
                modifier = Modifier.size(44.dp),
                style = MochiButtonStyle.ProminentGlass,
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(0.dp),
                minHeight = 44.dp,
            ) {
                Icon(
                    imageVector = if (audioPlaying) {
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = if (audioPlaying) "暂停" else "朗读",
                    // 跟随按钮玻璃前景色，避免蓝底蓝图标看不清
                    tint = ui.feice.mochi.theme.LocalContentColor.current,
                    modifier = Modifier.size(22.dp),
                )
            }
            AnimatedVisibility(
                visible = audioExpanded,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        // 用层级更明显的容器色，浅色模式下白色拇指/轨道也能看清
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                interaction++
                            }
                        }
                        .padding(start = 6.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (playingSource == SettingsRepository.AUDIO_SOURCE_MEDIA) {
                        MochiSlider(
                            value = (displayedPositionMs.toFloat() / totalMs).coerceIn(0f, 1f),
                            onValueChange = { fraction ->
                                dragging = true
                                dragPositionMs = (fraction * totalMs).toLong()
                                interaction++
                            },
                            onValueChangeFinished = {
                                dragging = false
                                positionMs = dragPositionMs
                                onSeek(dragPositionMs)
                                interaction++
                            },
                            modifier = Modifier
                                .width(140.dp)
                                .height(32.dp),
                            // 浅色模式下未播放轨道容易和白底混在一起，显式给出可辨识的轨道与拇指颜色
                            colors = MochiSliderDefaults.colors(
                                activeTrack = MochiTheme.colorScheme.primary,
                                inactiveTrack = MochiTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                                thumb = MochiTheme.colorScheme.surface,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatClock(displayedPositionMs),
                            style = MochiTheme.textStyles.footnote,
                            color = MochiTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // TTS 音源无法按时间拖动，展示当前朗读到第几句
                        Text(
                            text = "第 ${sentenceIndex + 1}/${sentenceCount.coerceAtLeast(1)} 句",
                            style = MochiTheme.textStyles.footnote,
                            color = MochiTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = !audioExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            MochiPopoverButton(
                expanded = panelExpanded,
                onExpandedChange = { panelExpanded = it },
                width = 240.dp,
                buttonMinHeight = 44.dp,
                popover = {
                    DisplayPanel(
                        translationOn = translationOn,
                        notesOn = notesOn,
                        onTranslationChange = onTranslationChange,
                        onNotesChange = onNotesChange,
                        zoomPercent = zoomPercent,
                        onZoomPercentChange = onZoomPercentChange,
                    )
                },
                buttonContent = {
                    Text(
                        text = "显示",
                        style = MochiTheme.textStyles.body,
                        color = MochiTheme.colorScheme.onSurface,
                    )
                },
            )
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

@Composable
private fun ColumnScope.DisplayPanel(
    translationOn: Boolean,
    notesOn: Boolean,
    onTranslationChange: (Boolean) -> Unit,
    onNotesChange: (Boolean) -> Unit,
    zoomPercent: Int,
    onZoomPercentChange: (Int) -> Unit,
) {
    DisplayCheckRow(
        label = "翻译",
        checked = translationOn,
        onCheckedChange = onTranslationChange,
    )
    DisplayCheckRow(
        label = "注释",
        checked = notesOn,
        onCheckedChange = onNotesChange,
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "字号",
            style = MochiTheme.textStyles.body,
            color = MochiTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        MochiIconButton(
            onClick = { onZoomPercentChange((zoomPercent - 5).coerceAtLeast(100)) },
            enabled = zoomPercent > 100,
            style = MochiButtonStyle.Tonal,
            size = 34.dp,
        ) {
            Icon(
                Icons.Rounded.TextDecrease,
                contentDescription = "缩小字号",
                tint = MochiTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "$zoomPercent%",
            style = MochiTheme.textStyles.footnote,
            color = MochiTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        MochiIconButton(
            onClick = { onZoomPercentChange((zoomPercent + 5).coerceAtMost(125)) },
            enabled = zoomPercent < 125,
            style = MochiButtonStyle.Tonal,
            size = 34.dp,
        ) {
            Icon(
                Icons.Rounded.TextIncrease,
                contentDescription = "放大字号",
                tint = MochiTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DisplayCheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MochiTheme.textStyles.body,
            color = MochiTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        MochiCheckbox(checked = checked, onCheckedChange = null)
    }
}

private fun splitSections(text: String): List<String> {
    val normalized = text.replace("\r\n", "\n").trim()
    if (normalized.isEmpty()) return emptyList()
    return normalized
        .split(Regex("\\n[ \\t]*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

/**
 * 译文分段：优先按空行分段；若译文只有一段但正文分成多段（古诗常见，译文按行排版），
 * 则回退为按单换行分段，保证左右逐段对应。
 */
private fun splitTranslation(text: String, bodyCount: Int): List<String> {
    val byBlank = splitSections(text)
    if (bodyCount <= 1 || byBlank.size == bodyCount) return byBlank
    val byLine = text.replace("\r\n", "\n").trim()
        .split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return if (byLine.size == bodyCount) byLine else byBlank
}

private val sentenceBoundary = Regex("(?<=[。！？；!?;])")

/**
 * 将译文与正文逐段配对：
 * 段落数一致时直接一一对应；不一致时把译文拆成句子，按正文各段长度比例分组，
 * 保证每一段正文都有对应译文、不会出现大段空白或译文全部堆在首段。
 */
private fun pairTranslation(sections: List<String>, translation: List<String>): List<String?> {
    if (sections.isEmpty() || translation.isEmpty()) return List(sections.size) { null }
    if (translation.size == sections.size) return translation

    val units = translation.flatMap { part ->
        part.replace("\r\n", "\n")
            .split("\n")
            .flatMap { line -> line.split(sentenceBoundary) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    if (units.isEmpty()) return List(sections.size) { null }
    if (units.size == sections.size) return units

    val weights = sections.map { section -> section.count { !it.isWhitespace() }.coerceAtLeast(1) }
    val result = MutableList<String?>(sections.size) { null }
    var index = 0
    for (i in sections.indices) {
        if (index >= units.size) break
        val remainingRows = sections.size - i
        val remainingUnits = units.size - index
        // 按长度占比分配句子，同时保证后续每段至少留一句
        var take = Math.round(remainingUnits.toFloat() * weights[i] / weights.drop(i).sum())
            .coerceAtLeast(1)
        take = take.coerceAtMost(remainingUnits - (remainingRows - 1)).coerceAtLeast(1)
        result[i] = units.subList(index, index + take).joinToString("")
        index += take
    }
    return result
}

private fun assignNotes(sections: List<String>, notes: List<Note>): List<List<Note>> {
    val result = MutableList(sections.size) { mutableListOf<Note>() }
    val used = mutableSetOf<Int>()

    // 优先使用数据中已匹配好的章节序号
    notes.forEachIndexed { noteIndex, note ->
        if (note.section in sections.indices) {
            result[note.section].add(note)
            used.add(noteIndex)
        }
    }

    // 未标注序号时，按词句在正文中出现的位置归属
    sections.forEachIndexed { index, section ->
        notes.forEachIndexed { noteIndex, note ->
            if (noteIndex !in used) {
                val key = note.phrase.ifBlank { note.pos }
                if (key.isNotEmpty() && section.contains(key)) {
                    result[index].add(note)
                    used.add(noteIndex)
                }
            }
        }
    }
    val leftovers = notes.filterIndexed { index, _ -> index !in used }
    if (leftovers.isNotEmpty() && result.isNotEmpty()) {
        result.last().addAll(leftovers)
    }
    return result
}

private fun highlightSection(
    section: String,
    notes: List<Note>,
    textColor: Color,
): AnnotatedString {
    val ranges = ArrayList<IntRange>()
    notes.forEach { note ->
        val key = note.phrase.ifBlank { note.pos }
        if (key.isNotEmpty()) {
            val start = section.indexOf(key)
            if (start >= 0) ranges.add(start until start + key.length)
        }
    }
    ranges.sortBy { it.first }

    val builder = AnnotatedString.Builder(section)
    var lastEnd = 0
    ranges.forEach { range ->
        if (range.first >= lastEnd) {
            builder.addStyle(
                SpanStyle(color = textColor, fontWeight = FontWeight.Medium),
                range.first,
                range.last + 1,
            )
            lastEnd = range.last + 1
        }
    }
    return builder.toAnnotatedString()
}

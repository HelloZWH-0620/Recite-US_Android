package com.reciteus2.beishuwa

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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

private fun audioAssetExists(context: Context, articleId: String): Boolean =
    try {
        context.assets.list("audio")?.contains("$articleId.mp3") == true
    } catch (e: Exception) {
        false
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

    val highlightColor = MaterialTheme.colorScheme.primaryContainer

    val sections = remember(article) { splitSections(article.text) }
    val translationParts = remember(article, sections) { splitTranslation(article.translation, sections.size) }
    val pairedTranslation = remember(sections, translationParts) {
        pairTranslation(sections, translationParts)
    }
    val sectionNotes = remember(article, sections) { assignNotes(sections, article.notes) }

    // 说明：原有一套从未被触发的 TTS 代码（每打开一篇文章都会初始化一个
    // TextToSpeech 引擎但没有任何入口把 speaking 置为 true）已移除；
    // 正文朗读后续可通过「朗读」按钮在无音频篇目上作为兜底重新接入。

    // 实验性朗读：播放内置音频 + 系统媒体中心控制
    val experimental = SettingsRepository.experimental.value
    var audioPlaying by remember { mutableStateOf(false) }
    val audioPlayer = remember { AudioPlayback(context) { audioPlaying = false } }
    val audioAvailable = remember(article.id) { audioAssetExists(context, article.id) }
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
        when {
            !audioAvailable ->
                Toast.makeText(context, "正在开发中", Toast.LENGTH_SHORT).show()

            audioPlaying -> {
                audioPlayer.stop()
                audioPlaying = false
            }

            else -> {
                if (Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // 音频缺失/损坏时 start() 可能抛异常，避免点击直接崩溃
                runCatching {
                    audioPlayer.start("audio/${article.id}.mp3", article.title, article.author)
                    audioPlaying = true
                }.onFailure {
                    audioPlaying = false
                    Toast.makeText(context, "朗读播放失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
            highlightColor = highlightColor,
            experimental = experimental,
            audioPlaying = audioPlaying,
            onToggleAudio = toggleAudio,
            onBack = onBack,
            onOpenAuthor = onOpenAuthor,
        )
        } }
        return
    }

    AppFontScaled { Box(Modifier.fillMaxSize().pinchToFontScale(pinchEnabled, context)) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                // 平板上限制正文宽度，保持舒适的阅读行宽
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

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
                    TextButton(onClick = { onOpenAuthor(author) }) {
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
                            highlightSection(section, article.notes, highlightColor)
                        } else {
                            AnnotatedString(section)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            lineHeight = 32.sp,
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
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp,
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
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                }

                if (article.appreciation.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "作品简介",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = article.appreciation,
                        fontSize = 15.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(120.dp))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background,
                        ),
                    )
                )
                .navigationBarsPadding()
                .padding(top = 24.dp, bottom = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ActionToggle(
                        label = "注释",
                        icon = Icons.Rounded.Notes,
                        active = notesOn,
                        onClick = { notesOn = !notesOn },
                    )
                    if (experimental) {
                        ActionToggle(
                            label = "朗读",
                            icon = Icons.Rounded.VolumeUp,
                            active = audioPlaying,
                            onClick = toggleAudio,
                        )
                    }
                    ActionToggle(
                        label = "翻译",
                        icon = Icons.Rounded.Translate,
                        active = translationOn,
                        onClick = { translationOn = !translationOn },
                    )
                }
            }
        }
    }
    } }
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
    highlightColor: Color,
    experimental: Boolean,
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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
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
                Box {
                    TextButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.width(120.dp),
                    ) {
                        Text(tabLabel, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Icon(
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = "切换显示内容",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.width(120.dp),
                    ) {
                        listOf("赏析", "注释", "翻译").forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    rightTab = index
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
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
                                    highlightSection(section, article.notes, highlightColor)
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
            if (experimental) {
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

@Composable
private fun ActionToggle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    val foreground = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RectangleShape,
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        contentColor = foreground,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 72.dp, minHeight = 44.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.size(5.dp))
            Text(text = label, color = foreground, fontSize = 15.sp)
        }
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

private fun highlightSection(section: String, notes: List<Note>, color: Color): AnnotatedString {
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
                SpanStyle(color = color, fontWeight = FontWeight.Medium),
                range.first,
                range.last + 1,
            )
            lastEnd = range.last + 1
        }
    }
    return builder.toAnnotatedString()
}

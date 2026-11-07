package com.reciteus2.beishuwa

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import android.widget.Toast

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

    val dark = isSystemInDarkTheme()
    val highlightColor = if (dark) Color(0xFFD9A521) else Color(0xFF6E9FD8)

    val sections = remember(article) { splitSections(article.text) }
    val translationParts = remember(article) { splitSections(article.translation) }
    val pairedTranslation = remember(sections, translationParts) {
        pairTranslation(sections.size, translationParts)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                                    color = Color.White,
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
                                        color = Color.White,
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
                shape = CircleShape,
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
                            onClick = {
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
                            },
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
}

@Composable
private fun GrayCard(content: @Composable ColumnScope.() -> Unit) {
    val background = if (isSystemInDarkTheme()) Color(0xFF3F3F3F) else Color(0xFF787878)
    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp),
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
        shape = RoundedCornerShape(50),
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

private fun pairTranslation(bodyCount: Int, translation: List<String>): List<String?> {
    if (bodyCount == 0 || translation.isEmpty()) return List(bodyCount) { null }
    if (translation.size == bodyCount) return translation
    if (bodyCount == 1) return listOf(translation.joinToString("\n\n"))

    val result = MutableList<String?>(bodyCount) { null }
    for (index in 0 until minOf(bodyCount, translation.size)) {
        result[index] = translation[index]
    }
    if (translation.size > bodyCount) {
        val tail = translation.drop(bodyCount).joinToString("\n\n")
        result[bodyCount - 1] = listOfNotNull(result[bodyCount - 1], tail)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }
    return result
}

private fun assignNotes(sections: List<String>, notes: List<Note>): List<List<Note>> {
    val result = MutableList(sections.size) { mutableListOf<Note>() }
    val used = mutableSetOf<Int>()
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

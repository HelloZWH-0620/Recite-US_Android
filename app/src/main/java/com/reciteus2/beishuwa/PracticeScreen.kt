package com.reciteus2.beishuwa

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiDialogActionRole
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.list.MochiIndexBar
import ui.feice.mochi.theme.MochiTheme

private sealed interface PracticeRoute {
    data object Home : PracticeRoute
    data object Picker : PracticeRoute
    data class Session(val articles: List<Article>) : PracticeRoute
}

/** 练习板块：与「字词」同级的根页面，卡片式入口。 */
@Composable
fun PracticePane(
    data: ReciteData,
    landscape: Boolean = false,
    onActiveChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var route by remember { mutableStateOf<PracticeRoute>(PracticeRoute.Home) }
    var showRangeDialog by remember { mutableStateOf(false) }
    var showNotice by rememberSaveable { mutableStateOf(true) }
    val backProgress = remember { Animatable(0f) }

    // 进入选题/答题时通知外层隐藏底部 dock
    LaunchedEffect(route) {
        onActiveChange(route != PracticeRoute.Home)
    }
    DisposableEffect(Unit) {
        onDispose { onActiveChange(false) }
    }

    // 系统侧滑返回：跟手位移 + 松手回弹
    PredictiveBackHandler(enabled = route != PracticeRoute.Home) { progress ->
        try {
            progress.collect { backProgress.snapTo(it.progress) }
            route = when (route) {
                is PracticeRoute.Session -> PracticeRoute.Picker
                else -> PracticeRoute.Home
            }
        } finally {
            backProgress.animateTo(0f)
        }
    }

    AppFontScaled {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = backProgress.value * size.width * 0.35f
                },
        ) {
            when (val current = route) {
                PracticeRoute.Home -> PracticeHome(
                    landscape = landscape,
                    onMemorization = { showRangeDialog = true },
                    onWordMatch = {
                        Toast.makeText(context, "词义匹配正在开发中", Toast.LENGTH_SHORT).show()
                    },
                )

                PracticeRoute.Picker -> ArticlePicker(
                    data = data,
                    onBack = { route = PracticeRoute.Home },
                    onConfirm = { route = PracticeRoute.Session(it) },
                )

                is PracticeRoute.Session -> MemorizationSession(
                    articles = current.articles,
                    onBack = { route = PracticeRoute.Picker },
                )
            }
        }
    }

    MochiDialog(
        visible = showRangeDialog,
        onDismissRequest = { showRangeDialog = false },
        title = "是否自定义范围出题？",
        actions = persistentListOf(
            MochiDialogAction("好的") {
                showRangeDialog = false
                route = PracticeRoute.Picker
            },
            MochiDialogAction(
                label = "全部范围",
                role = MochiDialogActionRole.Primary,
            ) {
                showRangeDialog = false
                // 全部范围：从不同篇目中随机抽 3 篇一起出题
                val picks = data.articles.shuffled().take(3)
                if (picks.isNotEmpty()) route = PracticeRoute.Session(picks)
            },
        ),
    )

    MochiDialog(
        visible = showNotice,
        onDismissRequest = { showNotice = false },
        title = "完善中",
        message = "可能存在学科性漏洞。",
        actions = persistentListOf(
            MochiDialogAction("好") { showNotice = false },
        ),
    )
}

@Composable
private fun PracticeHome(
    landscape: Boolean,
    onMemorization: () -> Unit,
    onWordMatch: () -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        if (landscape) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PracticeEntryCard(
                    modifier = Modifier.weight(1f),
                    title = "理解性默写",
                    enabled = true,
                    onClick = onMemorization,
                    leading = {
                        Icon(
                            Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
                PracticeEntryCard(
                    modifier = Modifier.weight(1f),
                    title = "词义匹配",
                    enabled = false,
                    onClick = onWordMatch,
                    leading = {
                        Icon(
                            Icons.Rounded.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        } else {
            PracticeEntryCard(
                modifier = Modifier.fillMaxWidth(),
                title = "理解性默写",
                enabled = true,
                onClick = onMemorization,
                leading = {
                    Icon(
                        Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            PracticeEntryCard(
                modifier = Modifier.fillMaxWidth(),
                title = "词义匹配",
                enabled = false,
                onClick = onWordMatch,
                leading = {
                    Icon(
                        Icons.Rounded.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun PracticeEntryCard(
    modifier: Modifier,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = MochiTheme.shapes.extraLarge,
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
            if (!enabled) {
                Text(
                    text = "即将上线",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class ArticleGroup(val letter: String, val articles: List<Article>)

private fun groupArticles(articles: List<Article>): List<ArticleGroup> =
    articles.groupBy { pinyinInitial(it.title) }
        .entries
        .sortedWith(compareBy({ it.key == "#" }, { it.key }))
        .map { ArticleGroup(it.key, it.value) }

/** 用 ICU 取标题首字的拼音首字母，低版本系统归入 #。 */
private fun pinyinInitial(title: String): String {
    val ch = title.firstOrNull() ?: return "#"
    if (ch.code < 128) return ch.uppercaseChar().toString()
    if (Build.VERSION.SDK_INT < 29) return "#"
    return runCatching {
        val transliterator = android.icu.text.Transliterator.getInstance("Han-Latin; Latin-ASCII")
        transliterator.transliterate(ch.toString()).firstOrNull()?.uppercaseChar()?.toString()
    }.getOrNull()?.takeIf { it.isNotBlank() && it[0] in 'A'..'Z' } ?: "#"
}

@Composable
private fun ArticlePicker(
    data: ReciteData,
    onBack: () -> Unit,
    onConfirm: (List<Article>) -> Unit,
) {
    val groups = remember(data.articles) { groupArticles(data.articles) }
    val labels = remember(groups) { persistentListOf(*groups.map { it.letter }.toTypedArray()) }
    val starts = remember(groups) {
        var index = 0
        groups.map { group ->
            index.also { index += 1 + group.articles.size }
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val selected = remember { mutableStateListOf<Article>() }
    val canConfirm = selected.isNotEmpty()

    Column(Modifier.fillMaxSize()) {
        PracticeTopBar(
            title = "选择篇目（${selected.size}）",
            onBack = onBack,
        ) {
            MochiButton(
                onClick = { if (canConfirm) onConfirm(selected.toList()) },
                modifier = Modifier.size(40.dp),
                enabled = canConfirm,
                style = MochiButtonStyle.ProminentGlass,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(0.dp),
                minHeight = 40.dp,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "开始出题",
                    tint = ui.feice.mochi.theme.LocalContentColor.current,
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 28.dp, top = 4.dp, bottom = 24.dp),
            ) {
                groups.forEach { group ->
                    item(key = "header-${group.letter}") {
                        Text(
                            text = group.letter,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
                        )
                    }
                    items(group.articles, key = { it.id }) { article ->
                        val isSelected = selected.any { it.id == article.id }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable {
                                    if (isSelected) {
                                        selected.removeAll { it.id == article.id }
                                    } else {
                                        selected.add(article)
                                    }
                                },
                            shape = MochiTheme.shapes.large,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            MochiIndexBar(
                labels = labels,
                onSelect = { index ->
                    starts.getOrNull(index)?.let { target ->
                        scope.launch { listState.scrollToItem(target) }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun PracticeTopBar(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MochiGlassIconButton(
            icon = Icons.Rounded.ArrowBack,
            contentDescription = "返回",
            onClick = onBack,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun MemorizationSession(articles: List<Article>, onBack: () -> Unit) {
    val context = LocalContext.current
    var questions by remember { mutableStateOf<List<PracticeQuestion>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, String>() }
    val articleIds = remember(articles) { articles.map { it.id } }

    LaunchedEffect(articleIds, reloadKey) {
        loading = true
        error = null
        submitted = false
        answers.clear()
        AiQuestionRepository.generateMemorization(
            context = context,
            articles = articles,
        ).onSuccess {
            questions = it
        }.onFailure {
            error = it.message ?: "生成失败，请稍后重试"
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MochiGlassIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (articles.size == 1) {
                        articles.first().title
                    } else {
                        "已选 ${articles.size} 篇"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (articles.size == 1) {
                        articles.first().author
                    } else {
                        articles.joinToString("、") { it.title }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "正在出题…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            error != null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(60.dp))
                Text(
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MochiButton(onClick = { reloadKey++ }) {
                        Text("重试")
                    }
                    MochiButton(
                        onClick = onBack,
                        style = MochiButtonStyle.Tonal,
                    ) {
                        Text("返回")
                    }
                }
            }

            else -> {
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                var focusedIndex by remember { mutableStateOf<Int?>(null) }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (focusedIndex != null) 260.dp else 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(questions) { index, question ->
                        val userAnswer = answers[index].orEmpty()
                        val correct = submitted && isAnswerCorrect(userAnswer, question)
                        val focused = focusedIndex == index
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            shape = MochiTheme.shapes.large,
                            color = if (focused) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                        ) {
                            Column(
                                Modifier
                                    .padding(16.dp)
                                    .then(if (focused) Modifier.heightIn(min = 180.dp) else Modifier),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "第 ${index + 1} 题",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (submitted) {
                                        Text(
                                            text = if (correct) "正确" else "待订正",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (correct) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = question.stem,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(10.dp))
                                MochiTextField(
                                    value = userAnswer,
                                    onValueChange = { answers[index] = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { state ->
                                            if (state.isFocused) {
                                                focusedIndex = index
                                                scope.launch {
                                                    delay(120)
                                                    listState.animateScrollToItem(index)
                                                }
                                            } else if (focusedIndex == index) {
                                                focusedIndex = null
                                            }
                                        },
                                    placeholder = "填写答案",
                                    singleLine = false,
                                    maxLines = 3,
                                    enabled = !submitted,
                                )
                                if (submitted) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "标准答案：${question.standardAnswer}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MochiButton(
                        onClick = { submitted = true },
                        modifier = Modifier.weight(1f),
                        enabled = !submitted,
                    ) {
                        Text("提交答案")
                    }
                    MochiButton(
                        onClick = { reloadKey++ },
                        style = MochiButtonStyle.Tonal,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("重新生成")
                    }
                }
            }
        }
    }
}

private fun isAnswerCorrect(input: String, question: PracticeQuestion): Boolean {
    val normalized = normalizeAnswer(input)
    if (normalized.isEmpty()) return false
    return question.answers.any { normalizeAnswer(it) == normalized }
}

private fun normalizeAnswer(text: String): String = text
    .replace("\\s".toRegex(), "")
    .replace(Regex("[，。、；：！？“”‘’（）《》〈〉,.;:!?\"'()<>\\[\\]{}—…·]"), "")
    .trim()

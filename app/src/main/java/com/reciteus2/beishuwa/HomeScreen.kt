package com.reciteus2.beishuwa

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.effect.glass.LocalMochiBackdrop
import ui.feice.mochi.effect.glass.mochiBackdropSource
import ui.feice.mochi.effect.glass.rememberMochiBackdrop
import ui.feice.mochi.basic.MochiSearchBar
import ui.feice.mochi.basic.MochiSegmentedControl
import ui.feice.mochi.basic.MochiTabBar
import ui.feice.mochi.basic.MochiTabBarDefaults
import ui.feice.mochi.basic.MochiTabItem
import ui.feice.mochi.theme.MochiTheme
import ui.feice.mochi.basic.MochiPageControl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    data: ReciteData,
    onOpenArticle: (Article) -> Unit,
    onOpenAuthor: (Author) -> Unit,
) {
    var rootTab by rememberSaveable { mutableIntStateOf(0) }
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var wordTab by rememberSaveable { mutableIntStateOf(1) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    // 进入练习会话（答题/选题）时隐藏底部 dock，让内容占满
    var practiceActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val shiCiWords = remember { WordRepository.load(context, "shici.json") }
    val xuCiWords = remember { WordRepository.load(context, "words.json") }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 输入法收起后主动清除搜索框焦点，避免输入框保持高亮、光标闪烁
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (!imeVisible) {
            focusManager.clearFocus()
            searchActive = false
        }
    }

    if (isLandscape) {
        LandscapeHome(
            data = data,
            shiCiWords = shiCiWords,
            xuCiWords = xuCiWords,
            rootTab = rootTab,
            subTab = subTab,
            wordTab = wordTab,
            query = query,
            searchActive = searchActive,
            onSearchActiveChange = { searchActive = it },
            onRootTabChange = { rootTab = it },
            onSubTabChange = { subTab = it },
            onWordTabChange = { wordTab = it },
            onQueryChange = { query = it },
            onOpenArticle = onOpenArticle,
            onOpenAuthor = onOpenAuthor,
            practiceActive = practiceActive,
            onPracticeActiveChange = { practiceActive = it },
        )
    } else {
        PortraitHome(
            data = data,
            shiCiWords = shiCiWords,
            xuCiWords = xuCiWords,
            rootTab = rootTab,
            subTab = subTab,
            wordTab = wordTab,
            query = query,
            searchActive = searchActive,
            onSearchActiveChange = { searchActive = it },
            onRootTabChange = { rootTab = it },
            onSubTabChange = { subTab = it },
            onWordTabChange = { wordTab = it },
            onQueryChange = { query = it },
            onOpenArticle = onOpenArticle,
            onOpenAuthor = onOpenAuthor,
            practiceActive = practiceActive,
            onPracticeActiveChange = { practiceActive = it },
        )
    }
}

@Composable
private fun PortraitHome(
    data: ReciteData,
    shiCiWords: List<Word>,
    xuCiWords: List<Word>,
    rootTab: Int,
    subTab: Int,
    wordTab: Int,
    query: String,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onRootTabChange: (Int) -> Unit,
    onSubTabChange: (Int) -> Unit,
    onWordTabChange: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenArticle: (Article) -> Unit,
    onOpenAuthor: (Author) -> Unit,
    practiceActive: Boolean,
    onPracticeActiveChange: (Boolean) -> Unit,
) {
    val backdrop = rememberMochiBackdrop()
    CompositionLocalProvider(LocalMochiBackdrop provides backdrop) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // 练习答题时自动收起 dock；dock 整体再抬高一点
            AnimatedVisibility(
                visible = !practiceActive,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            ) {
                MochiTabBar(
                    selectedIndex = rootTab,
                    onSelect = onRootTabChange,
                    modifier = Modifier.padding(bottom = 6.dp),
                    items = persistentListOf(
                    MochiTabItem(
                        label = "背书",
                        icon = { tint -> Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = tint) },
                    ),
                    MochiTabItem(
                        label = "字词",
                        icon = { tint -> Icon(Icons.Rounded.TextFields, contentDescription = null, tint = tint) },
                    ),
                    MochiTabItem(
                        label = "练习",
                        icon = { tint -> Icon(Icons.Rounded.EditNote, contentDescription = null, tint = tint) },
                    ),
                    MochiTabItem(
                        label = "我的",
                        icon = { tint -> Icon(Icons.Rounded.Person, contentDescription = null, tint = tint) },
                    ),
                ),
                behavior = MochiTabBarDefaults.behavior(
                    height = 56.dp,
                    itemWidth = 76.dp,
                ),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 尊重 Scaffold 的底部内边距：否则列表最后一项会被 56dp 标签栏压住
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    // 平板上限制正文宽度，避免单行过长
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                if (rootTab == 0) {
                    val focus = LocalFocusManager.current
                    val keyboard = LocalSoftwareKeyboardController.current
                    val deactivate = {
                        focus.clearFocus()
                        keyboard?.hide()
                        onSearchActiveChange(false)
                    }
                    val density = LocalDensity.current
                    var headerHeight by remember { mutableStateOf(0.dp) }
                    Box(Modifier.fillMaxSize()) {
                        // 目录：作为玻璃取样来源，内容从悬浮栏与导航栏下方滚过
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .mochiBackdropSource(backdrop)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        waitForUpOrCancellation()
                                        deactivate()
                                    }
                                },
                        ) {
                            if (subTab == 0) {
                                ArticlePane(
                                    data = data,
                                    query = query,
                                    onOpenArticle = onOpenArticle,
                                    topPadding = headerHeight,
                                    bottomPadding = 96.dp,
                                )
                            } else {
                                AuthorPane(
                                    data = data,
                                    query = query,
                                    onOpenAuthor = onOpenAuthor,
                                    topPadding = headerHeight,
                                    bottomPadding = 96.dp,
                                )
                            }
                        }
                        // 顶栏：背景覆盖到状态栏区域，内容整体避让安全区
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged {
                                    headerHeight = with(density) { it.height.toDp() }
                                }
                                .background(MaterialTheme.colorScheme.background)
                                .statusBarsPadding(),
                        ) {
                            AnimatedVisibility(
                                visible = !searchActive,
                                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                            ) {
                                CenteredTabs(
                                    selected = subTab,
                                    onSelect = onSubTabChange,
                                )
                            }
                            SearchField(
                                query = query,
                                placeholder = if (subTab == 0) "搜索篇目" else "搜索作者",
                                onQueryChange = onQueryChange,
                                onActivate = { onSearchActiveChange(true) },
                                onCancel = {
                                    onQueryChange("")
                                    deactivate()
                                },
                            )
                        }
                    }
                } else if (rootTab == 1) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .mochiBackdropSource(backdrop),
                    ) {
                        WordPane(
                            words = if (wordTab == 0) shiCiWords else xuCiWords,
                            selectedTab = wordTab,
                            onSelectTab = onWordTabChange,
                        )
                    }
                } else if (rootTab == 2) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .mochiBackdropSource(backdrop),
                    ) {
                        PracticePane(data = data, onActiveChange = onPracticeActiveChange)
                    }
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .mochiBackdropSource(backdrop),
                    ) {
                        ProfileScreen()
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun LandscapeHome(
    data: ReciteData,
    shiCiWords: List<Word>,
    xuCiWords: List<Word>,
    rootTab: Int,
    subTab: Int,
    wordTab: Int,
    query: String,
    searchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    onRootTabChange: (Int) -> Unit,
    onSubTabChange: (Int) -> Unit,
    onWordTabChange: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenArticle: (Article) -> Unit,
    onOpenAuthor: (Author) -> Unit,
    practiceActive: Boolean,
    onPracticeActiveChange: (Boolean) -> Unit,
) {
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val searchWidth = minOf(
        (LocalConfiguration.current.screenWidthDp.dp - 32.dp) / when {
            !isTablet -> 2.5f
            LocalConfiguration.current.screenWidthDp >= 1100 -> 4.5f
            else -> 3.5f
        },
        if (isTablet) 360.dp else 320.dp,
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(Modifier.fillMaxSize()) {
                if (rootTab == 0) {
                    val focus = LocalFocusManager.current
                    val keyboard = LocalSoftwareKeyboardController.current
                    val deactivate = {
                        focus.clearFocus()
                        keyboard?.hide()
                        onSearchActiveChange(false)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedVisibility(
                            visible = !searchActive,
                            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
                        ) {
                            // 左上角：文章 / 作者
                            MochiSegmentedControl(
                                selectedIndex = subTab,
                                onSelect = onSubTabChange,
                                items = persistentListOf("文章", "作者"),
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // 右上角：搜索栏
                        MochiSearchBar(
                            query = query,
                            onQueryChange = onQueryChange,
                            modifier = Modifier
                                .width(searchWidth)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        onSearchActiveChange(true)
                                    }
                                },
                            placeholder = if (subTab == 0) "搜索篇目" else "搜索作者",
                            onCancel = {
                                onQueryChange("")
                                deactivate()
                            },
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    waitForUpOrCancellation()
                                    deactivate()
                                }
                            },
                    ) {
                        if (subTab == 0) {
                            LandscapeArticlePane(data = data, query = query, onOpenArticle = onOpenArticle)
                        } else {
                            LandscapeAuthorPane(data = data, query = query, onOpenAuthor = onOpenAuthor)
                        }
                    }
                } else if (rootTab == 1) {
                    WordPane(
                        words = if (wordTab == 0) shiCiWords else xuCiWords,
                        selectedTab = wordTab,
                        onSelectTab = onWordTabChange,
                        landscape = true,
                    )
                } else if (rootTab == 2) {
                    PracticePane(
                        data = data,
                        landscape = true,
                        onActiveChange = onPracticeActiveChange,
                    )
                } else {
                    ProfileScreen()
                }
            }

            // 搜索建议浮层：覆盖正文，避免撑开顶部栏
            // 右下角：悬浮胶囊导航（练习答题时自动收起）
            AnimatedVisibility(
                visible = !practiceActive,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                FloatingCapsule(
                    selected = rootTab,
                    onSelect = onRootTabChange,
                )
            }
        }
    }
}

/**
 * 字词模块：实词 / 虚词两个 tab。手机端卡片竖向排列，平板横屏时一列一张卡横向滑动。
 */
@Composable
private fun WordPane(
    words: List<Word>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    landscape: Boolean = false,
) {
    val context = LocalContext.current
    val pinchEnabled = SettingsRepository.pinchZoomEnabled.value
    AppFontScaled {
    Box(Modifier.fillMaxSize().pinchToFontScale(pinchEnabled, context)) {
    Column(Modifier.fillMaxSize()) {
        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MochiSegmentedControl(
                    selectedIndex = selectedTab,
                    onSelect = onSelectTab,
                    items = persistentListOf("实词", "虚词"),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                MochiSegmentedControl(
                    selectedIndex = selectedTab,
                    onSelect = onSelectTab,
                    items = persistentListOf("实词", "虚词"),
                    modifier = Modifier.width(220.dp),
                )
            }
        }

        when {
            words.isEmpty() -> EmptyState(if (selectedTab == 0) "暂无实词数据" else "暂无虚词数据")

            landscape -> BoxWithConstraints(Modifier.fillMaxSize()) {
                // 横屏按屏宽铺 2~3 张卡，避免单张卡过宽产生大片空白
                val cardWidth = ((maxWidth - 32.dp) / 2.8f).coerceIn(300.dp, 420.dp)
                LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 220.dp, bottom = 16.dp),
                ) {
                    itemsIndexed(words) { _, word ->
                        WordCard(
                            word = word,
                            modifier = Modifier
                                .width(cardWidth)
                                .fillMaxHeight(),
                            scrollable = true,
                        )
                    }
                }
            }

            else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                // 手机端：mochi 左右轮播，卡片高度随当前页内容自适应
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                val cardWidth = maxWidth - 32.dp
                val pitchPx = with(LocalDensity.current) { (cardWidth + 12.dp).toPx() }
                Column(Modifier.fillMaxSize()) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .animateContentSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    ) {
                        itemsIndexed(words) { _, word ->
                            WordCard(
                                word = word,
                                modifier = Modifier.width(cardWidth),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    MochiPageControl(
                        pageCount = words.size,
                        position = {
                            val info = listState.layoutInfo
                            val first = info.visibleItemsInfo.firstOrNull()
                            if (first == null) {
                                0f
                            } else {
                                val scrolled = (info.viewportStartOffset - first.offset).toFloat()
                                (first.index + scrolled / pitchPx)
                                    .coerceIn(0f, (words.size - 1).toFloat())
                            }
                        },
                        onPageSelect = { scope.launch { listState.animateScrollToItem(it) } },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 88.dp),
                    )
                }
            }
        }
    }
    }
    }
}

@Composable
private fun WordCard(word: Word, modifier: Modifier = Modifier, scrollable: Boolean = false) {
    val base = modifier
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        .background(MaterialTheme.colorScheme.surface)
    Column(
        modifier = if (scrollable) {
            base.verticalScroll(rememberScrollState()).padding(20.dp)
        } else {
            base.padding(20.dp)
        },
    ) {
        Text(
            text = word.word,
            fontSize = 56.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        word.defs.forEachIndexed { defIndex, def ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "${defIndex + 1}. ${def.meaning}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (def.example.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = def.example,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (defIndex != word.defs.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FloatingCapsule(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CapsuleItem(Icons.Rounded.MenuBook, "背书", selected == 0) { onSelect(0) }
            CapsuleItem(Icons.Rounded.TextFields, "字词", selected == 1) { onSelect(1) }
            CapsuleItem(Icons.Rounded.EditNote, "练习", selected == 2) { onSelect(2) }
            CapsuleItem(Icons.Rounded.Person, "我的", selected == 3) { onSelect(3) }
        }
    }
}

@Composable
private fun CapsuleItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(RectangleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CenteredTabs(selected: Int, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        MochiSegmentedControl(
            selectedIndex = selected,
            onSelect = onSelect,
            items = persistentListOf("文章", "作者"),
            modifier = Modifier.width(220.dp),
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onActivate: () -> Unit,
    onCancel: () -> Unit,
) {
    MochiSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onActivate()
                }
            },
        placeholder = placeholder,
        onCancel = onCancel,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArticlePane(
    data: ReciteData,
    query: String,
    onOpenArticle: (Article) -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
) {
    val trimmed = query.trim()
    val filtered = remember(data, trimmed) {
        data.articles.filter {
            trimmed.isEmpty() || it.title.contains(trimmed, ignoreCase = true) ||
                it.author.contains(trimmed, ignoreCase = true)
        }
    }
    val groups = remember(filtered) {
        listOf(
            "文言文" to filtered.filter { !it.isPoem },
            "古诗词" to filtered.filter { it.isPoem },
        ).filter { it.second.isNotEmpty() }
    }

    if (filtered.isEmpty()) {
        EmptyState("未找到篇目")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding + 24.dp),
    ) {
        groups.forEach { (label, list) ->
            item(key = "header_$label") {
                SectionHeader(label)
            }
            items(list, key = { it.id }) { article ->
                ArticleRow(article) { onOpenArticle(article) }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    // 按 mochi 分组小标题规范：与行文字左边距 16dp 对齐，字号同正文、半粗，下方留 10dp
    Text(
        text = label,
        style = MochiTheme.textStyles.navTitle,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 10.dp),
    )
}

@Composable
private fun ArticleRow(article: Article, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MochiTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = article.author,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

/**
 * 横屏文章列表：整体横向滚动，每一列纵向排列 3 个按钮；
 * 文言文排完后古诗词继续向右另起新列，参考 Windows 8 起始页布局。
 */
@Composable
private fun LandscapeArticlePane(data: ReciteData, query: String, onOpenArticle: (Article) -> Unit) {
    val trimmed = query.trim()
    val filtered = remember(data, trimmed) {
        data.articles.filter {
            trimmed.isEmpty() || it.title.contains(trimmed, ignoreCase = true) ||
                it.author.contains(trimmed, ignoreCase = true)
        }
    }
    val groups = remember(filtered) {
        listOf(
            "文言文" to filtered.filter { !it.isPoem },
            "古诗词" to filtered.filter { it.isPoem },
        ).filter { it.second.isNotEmpty() }
    }

    if (filtered.isEmpty()) {
        EmptyState("未找到篇目")
        return
    }

    // 系统大字体下隐藏作者行，给标题留出空间
    val largeFont = LocalDensity.current.fontScale >= 1.3f

    // 展平为「列」列表，同时记录每列所属分组，用于悬浮提示
    val allColumns = remember(filtered) {
        groups.flatMap { (label, list) -> list.chunked(5).map { label to it } }
    }
    val listState = rememberLazyListState()

    // 悬浮分组提示：触摸后显示，5 秒无触摸自动隐藏
    var hintVisible by remember { mutableStateOf(true) }
    var touchTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(touchTick) {
        hintVisible = true
        delay(5000)
        hintVisible = false
    }
    val currentLabel = allColumns.getOrNull(listState.firstVisibleItemIndex)?.first
        ?: groups.first().first

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 手机横屏约 2.5 列；平板按实际宽度展示 3.5 / 4.5 列，避免卡片被拉得过大
        val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
        val columnsVisible = when {
            !isTablet -> 2.5f
            maxWidth >= 1100.dp -> 4.5f
            else -> 3.5f
        }
        val columnWidth = minOf(
            (maxWidth - 32.dp) / columnsVisible,
            if (isTablet) 360.dp else 320.dp,
        )
        Box(Modifier.fillMaxSize()) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            touchTick++
                        }
                    },
                contentPadding = PaddingValues(start = 16.dp, end = 210.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                groups.forEach { (_, list) ->
                    val columns = list.chunked(5)
                    itemsIndexed(columns, key = { _, chunk -> "col_${chunk.first().id}" }) { _, chunk ->
                        ArticleColumn(
                            articles = chunk,
                            largeFont = largeFont,
                            onOpenArticle = onOpenArticle,
                            modifier = Modifier
                                .width(columnWidth)
                                .fillMaxHeight(),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = hintVisible,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 10.dp),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    shape = RectangleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                ) {
                    Text(
                        text = currentLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleColumn(
    articles: List<Article>,
    largeFont: Boolean,
    onOpenArticle: (Article) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // 固定 5 个槽位，空槽用占位补齐，保证不足 5 个的列卡片高度与其他列一致
        repeat(5) { slot ->
            if (slot > 0) {
                Spacer(Modifier.height(8.dp))
            }
            val article = articles.getOrNull(slot)
            if (article != null) {
                ArticleTile(
                    article = article,
                    largeFont = largeFont,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = { onOpenArticle(article) },
                )
            } else {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

private val LandscapeTileTitleStyle = TextStyle(
    fontSize = 17.sp,
    lineHeight = 26.sp,
    fontWeight = FontWeight.SemiBold,
    // 开启字体上下留白，避免大字体下标题下半部分被卡片圆角裁掉
    platformStyle = PlatformTextStyle(includeFontPadding = true),
)

@Composable
private fun ArticleTile(
    article: Article,
    largeFont: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RectangleShape,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = article.title,
                style = LandscapeTileTitleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!largeFont) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = article.author,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class AuthorSection(val dynasty: String, val authors: List<Author>)

/**
 * 平板作者页：取消左侧朝代 tab，改为横向滚动。
 * 按朝代分组，每组向右铺开多列，每列最多 7 位作者，组首列顶部保留朝代标签。
 */
@Composable
private fun LandscapeAuthorPane(
    data: ReciteData,
    query: String,
    onOpenAuthor: (Author) -> Unit,
) {
    val trimmed = query.trim()
    val groups = remember(data, trimmed) {
        val ordered = ReciteData.DYNASTY_ORDER +
            data.authors.map { it.dynasty }.filter { it !in ReciteData.DYNASTY_ORDER }.distinct()
        ordered.mapNotNull { dynasty ->
            val list = data.authorsOfDynasty(dynasty)
                .filter { !it.name.startsWith("《") }
                .filter { trimmed.isEmpty() || it.name.contains(trimmed, ignoreCase = true) }
            if (list.isEmpty()) null else dynasty to list.chunked(7)
        }
    }

    if (groups.isEmpty()) {
        EmptyState("未找到作者")
        return
    }

    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 220.dp, bottom = 16.dp),
    ) {
        groups.forEach { (dynasty, columns) ->
            items(columns.size, key = { "$dynasty-$it" }) { columnIndex ->
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight(),
                ) {
                    // 所有列都渲染同样高度的朝代标签，非首列仅隐藏文字，保证按钮上下对齐
                    Text(
                        text = dynasty,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .alpha(if (columnIndex == 0) 1f else 0f)
                            .padding(bottom = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RectangleShape,
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    columns[columnIndex].forEach { author ->
                        Surface(
                            onClick = { onOpenAuthor(author) },
                            shape = RectangleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(bottom = 8.dp),
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = author.name,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    repeat(7 - columns[columnIndex].size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AuthorPane(
    data: ReciteData,
    query: String,
    onOpenAuthor: (Author) -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
) {
    val trimmed = query.trim()
    val sections = remember(data, trimmed) {
        ReciteData.DYNASTY_ORDER.mapNotNull { dynasty ->
            val list = data.authorsOfDynasty(dynasty)
                // 隐藏《礼记》《论语》等以书名署名的条目
                .filter { !it.name.startsWith("《") }
                .filter { trimmed.isEmpty() || it.name.contains(trimmed, ignoreCase = true) }
            if (list.isEmpty()) null else AuthorSection(dynasty, list)
        }
    }

    if (sections.isEmpty()) {
        EmptyState("未找到作者")
        return
    }

    val headerIndexes = remember(sections) {
        val indexes = ArrayList<Int>(sections.size)
        var index = 0
        sections.forEach {
            indexes.add(index)
            index += 1 + it.authors.size
        }
        indexes
    }

    val rightState = rememberLazyListState()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // 搜索结果变化时，若当前朝代无作者则回退到第一个可用朝代
    LaunchedEffect(sections) {
        val currentDynasty = ReciteData.DYNASTY_ORDER.getOrNull(selected)
        if (currentDynasty == null || sections.none { it.dynasty == currentDynasty }) {
            selected = ReciteData.DYNASTY_ORDER.indexOf(sections.first().dynasty).coerceAtLeast(0)
            rightState.scrollToItem(0)
        }
    }

    // 右侧上下滑动 → 左侧朝代高亮联动；滑到底部时高亮最后一组
    LaunchedEffect(rightState, sections, headerIndexes) {
        snapshotFlow { rightState.layoutInfo }.collect { info ->
            if (info.totalItemsCount == 0) return@collect
            val firstIndex = info.visibleItemsInfo.firstOrNull()?.index ?: return@collect
            var section = 0
            for (i in headerIndexes.indices) {
                if (firstIndex >= headerIndexes[i]) section = i else break
            }
            val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return@collect
            // 只有「真正滚到底」（不能继续向前，但还能往回滚）才强制高亮最后一组；
            // 否则点击左侧朝代后联动滚动途中会被强制拽回最后一组，
            // 内容不满一屏（根本不能滚）时也不会覆盖点击结果
            if (!rightState.canScrollForward && rightState.canScrollBackward &&
                lastVisible.index >= info.totalItemsCount - 1
            ) {
                section = sections.lastIndex
            }
            val dynastyIndex = ReciteData.DYNASTY_ORDER.indexOf(sections[section].dynasty)
            if (dynastyIndex >= 0 && dynastyIndex != selected) {
                selected = dynastyIndex
            }
        }
    }

    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .width(92.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            contentPadding = PaddingValues(top = topPadding + 4.dp, bottom = bottomPadding + 4.dp),
        ) {
            itemsIndexed(ReciteData.DYNASTY_ORDER) { index, dynasty ->
                DynastyNavItem(
                    label = dynasty,
                    selected = selected == index,
                    enabled = sections.any { it.dynasty == dynasty },
                    onClick = {
                        selected = index
                        val target = sections.indexOfFirst { it.dynasty == dynasty }
                        if (target >= 0) {
                            coroutineScope.launch {
                                rightState.animateScrollToItem(headerIndexes[target])
                            }
                        }
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        )

        LazyColumn(
            state = rightState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 14.dp,
                top = topPadding,
                bottom = bottomPadding + 24.dp,
            ),
        ) {
            sections.forEach { section ->
                item(key = "authors_${section.dynasty}") {
                    SectionHeader(section.dynasty)
                }
                items(section.authors, key = { it.id }) { author ->
                    AuthorRow(author) { onOpenAuthor(author) }
                }
            }
        }
    }
}

@Composable
private fun DynastyNavItem(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.primary
        enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RectangleShape,
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
        )
    }
}

@Composable
private fun AuthorRow(author: Author, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MochiTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = author.name,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
        )
    }
}



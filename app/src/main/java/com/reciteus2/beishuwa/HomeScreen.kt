package com.reciteus2.beishuwa

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    data: ReciteData,
    onOpenArticle: (Article) -> Unit,
    onOpenAuthor: (Author) -> Unit,
) {
    var rootTab by rememberSaveable { mutableIntStateOf(0) }
    var subTab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                NavigationBarItem(
                    selected = rootTab == 0,
                    onClick = { rootTab = 0 },
                    icon = { Icon(Icons.Rounded.MenuBook, contentDescription = null) },
                    label = { Text("背书") },
                )
                NavigationBarItem(
                    selected = rootTab == 1,
                    onClick = { rootTab = 1 },
                    icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    label = { Text("我的") },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (rootTab == 0) {
                CenteredTabs(
                    selected = subTab,
                    onSelect = { subTab = it },
                )
                SearchField(
                    query = query,
                    placeholder = if (subTab == 0) "搜索篇目" else "搜索作者",
                    onQueryChange = { query = it },
                    suggestions = searchSuggestions(data, query.trim(), subTab),
                    onSuggestionClick = { suggestion ->
                        query = ""
                        when {
                            suggestion.article != null -> onOpenArticle(suggestion.article)
                            suggestion.author != null -> onOpenAuthor(suggestion.author)
                        }
                    },
                )
                if (subTab == 0) {
                    ArticlePane(data = data, query = query, onOpenArticle = onOpenArticle)
                } else {
                    AuthorPane(data = data, query = query, onOpenAuthor = onOpenAuthor)
                }
            } else {
                ProfileScreen()
            }
        }
    }
}

@Composable
private fun CenteredTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        TabItem("文章", selected == 0) { onSelect(0) }
        Spacer(Modifier.width(8.dp))
        TabItem("作者", selected == 1) { onSelect(1) }
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 17.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(3.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(2.dp),
                ),
        )
    }
}

private data class SearchSuggestion(
    val title: String,
    val subtitle: String,
    val article: Article? = null,
    val author: Author? = null,
)

private fun searchSuggestions(
    data: ReciteData,
    query: String,
    subTab: Int,
): List<SearchSuggestion> {
    if (query.isEmpty()) return emptyList()
    return if (subTab == 0) {
        data.articles
            .filter { it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true) }
            .take(6)
            .map { SearchSuggestion(it.title, it.author, article = it) }
    } else {
        data.authors
            .filter { !it.name.startsWith("《") }
            .filter { it.name.contains(query, ignoreCase = true) }
            .take(6)
            .map { SearchSuggestion(it.name, it.dynasty, author = it) }
    }
}

@Composable
private fun SearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<SearchSuggestion>,
    onSuggestionClick: (SearchSuggestion) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 15.sp) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "清空搜索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
        )

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    suggestions.forEachIndexed { index, suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestionClick(suggestion) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = suggestion.title,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = suggestion.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        if (index < suggestions.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArticlePane(data: ReciteData, query: String, onOpenArticle: (Article) -> Unit) {
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
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        groups.forEach { (label, list) ->
            stickyHeader(key = "header_$label") {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun ArticleRow(article: Article, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
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

private data class AuthorSection(val dynasty: String, val authors: List<Author>)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AuthorPane(data: ReciteData, query: String, onOpenAuthor: (Author) -> Unit) {
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
            if (lastVisible.index >= info.totalItemsCount - 1) {
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentPadding = PaddingValues(vertical = 4.dp),
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
            contentPadding = PaddingValues(start = 10.dp, end = 14.dp, bottom = 24.dp),
        ) {
            sections.forEach { section ->
                stickyHeader(key = "authors_${section.dynasty}") {
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
                    RoundedCornerShape(2.dp),
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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



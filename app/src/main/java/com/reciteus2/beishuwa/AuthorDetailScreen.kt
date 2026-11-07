package com.reciteus2.beishuwa

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private sealed interface AuthorUiState {
    data object Loading : AuthorUiState
    data class Ready(val markdown: String, val fromCache: Boolean) : AuthorUiState
    data class Failed(val message: String) : AuthorUiState
}

@Composable
fun AuthorDetailScreen(author: Author, onBack: () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val fontScale = LocalDensity.current.fontScale
    val url = remember(author) { AuthorContentRepository.urlFor(author) }

    var attempt by remember { mutableIntStateOf(0) }
    var state by remember(author) { mutableStateOf<AuthorUiState>(AuthorUiState.Loading) }

    LaunchedEffect(author.id, attempt) {
        state = AuthorUiState.Loading
        state = try {
            val content = AuthorContentRepository.load(context, author)
            AuthorUiState.Ready(content.markdown, content.fromCache)
        } catch (error: Exception) {
            AuthorUiState.Failed(
                when (error) {
                    is UnknownHostException, is ConnectException, is SocketTimeoutException ->
                        "网络连接失败，请检查网络后重试"
                    else -> error.message ?: "加载失败"
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AuthorTopBar(
            title = url,
            onBack = onBack,
            onCopy = { copyLink(context, url) },
            onOpenBrowser = { openInBrowser(context, url) },
            onShare = { shareLink(context, author, url) },
        )

        Box(Modifier.weight(1f)) {
            when (val current = state) {
                AuthorUiState.Loading -> LoadingState()
                is AuthorUiState.Failed -> ErrorState(
                    message = current.message,
                    onRetry = { attempt++ },
                )
                is AuthorUiState.Ready -> {
                    val html = remember(current.markdown, dark) {
                        MarkdownRenderer.render(current.markdown, dark)
                    }
                    key(html, fontScale) {
                        AuthorWebView(
                            html = html,
                            baseUrl = url,
                            dark = dark,
                            fontScale = fontScale,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorTopBar(
    title: String,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onOpenBrowser: () -> Unit,
    onShare: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(start = 4.dp, end = 4.dp),
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
            text = title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text("复制页面链接") },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onCopy()
                    },
                )
                DropdownMenuItem(
                    text = { Text("用系统浏览器打开") },
                    leadingIcon = { Icon(Icons.Rounded.OpenInBrowser, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onOpenBrowser()
                    },
                )
                DropdownMenuItem(
                    text = { Text("分享") },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onShare()
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "正在加载作者资料…",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "加载失败",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun AuthorWebView(
    html: String,
    baseUrl: String,
    dark: Boolean,
    fontScale: Float,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(webColor(if (dark) "#12100C" else "#F7F3EA"))
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.loadsImagesAutomatically = false
                settings.textZoom = (fontScale * 100f).toInt().coerceIn(80, 200)
                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
            }
        },
    )
}

private fun webColor(value: String): Int = android.graphics.Color.parseColor(value)

private fun copyLink(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("作者页面链接", url))
    Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
}

private fun openInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "未找到可用的浏览器", Toast.LENGTH_SHORT).show() }
}

private fun shareLink(context: Context, author: Author, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${author.name}\n$url")
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "分享作者"))
    }.onFailure {
        Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
    }
}

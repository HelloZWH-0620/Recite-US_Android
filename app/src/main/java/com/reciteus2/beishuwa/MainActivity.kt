package com.reciteus2.beishuwa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsRepository.init(this)
        enableEdgeToEdge()
        setContent {
            BeishuwaTheme {
                BeishuwaApp()
            }
        }
    }
}

private sealed interface Screen {
    val key: String

    data object Home : Screen {
        override val key = "home"
    }

    data class ArticleDetail(val article: Article) : Screen {
        override val key = "article_${article.id}"
    }

    data class AuthorDetail(val author: Author) : Screen {
        override val key = "author_${author.id}"
    }
}

@Composable
private fun BeishuwaApp() {
    val context = LocalContext.current
    val data = remember(context) { ReciteRepository.load(context) }
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val scope = rememberCoroutineScope()
    val stateHolder = rememberSaveableStateHolder()

    // 0f：当前页面完全就位；1f：当前页面完全滑出到屏幕右侧
    var progress by remember { mutableFloatStateOf(0f) }
    var animJob by remember { mutableStateOf<Job?>(null) }
    var lastPopAt by remember { mutableStateOf(0L) }

    // 防抖：一次返回手势/点击只弹一级，避免系统重复派发导致连续弹栈
    fun tryPop(): Boolean {
        if (stack.size <= 1) return false
        val now = System.currentTimeMillis()
        if (now - lastPopAt < 400) return false
        lastPopAt = now
        return true
    }

    suspend fun animateProgress(target: Float) {
        animate(
            initialValue = progress,
            targetValue = target,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        ) { value, _ -> progress = value }
    }

    fun push(screen: Screen) {
        animJob?.cancel()
        progress = 1f
        stack.add(screen)
        animJob = scope.launch { animateProgress(0f) }
    }

    fun pop() {
        if (!tryPop()) return
        animJob?.cancel()
        animJob = scope.launch {
            animateProgress(1f)
            stack.removeAt(stack.lastIndex)
            progress = 0f
        }
    }

    fun popImmediate() {
        if (!tryPop()) return
        animJob?.cancel()
        stack.removeAt(stack.lastIndex)
        progress = 0f
    }

    // Android 16 预测性返回：系统边缘手势的进度直接驱动页面位移，与系统导航动画同步
    PredictiveBackHandler(enabled = stack.size > 1) { backFlow ->
        try {
            backFlow.collect { event ->
                animJob?.cancel()
                progress = event.progress
            }
            popImmediate()
        } catch (e: CancellationException) {
            // 手势取消，回弹到原位
            animJob = scope.launch { animateProgress(0f) }
        }
    }

    // 启动时申请通知权限（媒体中心控制需要）
    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 横竖屏由实验性功能开关控制：关闭时锁定竖屏
    val experimental by SettingsRepository.experimental
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(experimental) {
        activity?.requestedOrientation = if (experimental) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose { }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val widthPx = constraints.maxWidth.toFloat()

        // 每个层级占用固定的组合槽位，推入/弹出时页面不会离开组合，
        // 从而完整保留 rememberSaveable 状态（滚动位置等）
        stack.forEachIndexed { index, screen ->
            val isTop = index == stack.lastIndex

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            if (isTop) {
                                (widthPx * progress).roundToInt()
                            } else {
                                (-widthPx * 0.22f * (1f - progress)).roundToInt()
                            },
                            0,
                        )
                    },
            ) {
                key(screen.key) {
                    stateHolder.SaveableStateProvider(screen.key) {
                        RenderScreen(screen, data, push = { push(it) }, pop = { pop() })
                    }
                }
            }

            // 顶层与下层之间的深色遮罩，随滑出程度减弱
            if (index == stack.lastIndex - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f * progress)),
                )
            }
        }
    }
}

@Composable
private fun RenderScreen(
    screen: Screen,
    data: ReciteData,
    push: (Screen) -> Unit,
    pop: () -> Unit,
) {
    when (screen) {
        Screen.Home -> HomeScreen(
            data = data,
            onOpenArticle = { push(Screen.ArticleDetail(it)) },
            onOpenAuthor = { push(Screen.AuthorDetail(it)) },
        )

        is Screen.ArticleDetail -> ArticleDetailScreen(
            article = screen.article,
            onBack = pop,
            onOpenAuthor = { author -> author?.let { push(Screen.AuthorDetail(it)) } },
        )

        is Screen.AuthorDetail -> AuthorDetailScreen(
            author = screen.author,
            onBack = pop,
        )
    }
}

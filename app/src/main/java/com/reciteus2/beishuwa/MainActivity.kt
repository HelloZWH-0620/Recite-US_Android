package com.reciteus2.beishuwa

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import kotlinx.collections.immutable.persistentListOf
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiToastHost
import ui.feice.mochi.basic.rememberMochiToastState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private var screenCaptureCallback: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsRepository.init(this)
        requestNotificationPermission()
        registerScreenshotDetection()
        enableEdgeToEdge()
        setContent {
            BeishuwaTheme {
                BeishuwaApp()
            }
        }
    }

    /** 监听系统截屏（Android 14+），用于截图纠错提示。 */
    private fun registerScreenshotDetection() {
        if (Build.VERSION.SDK_INT < 34) return
        val callback = object : android.app.Activity.ScreenCaptureCallback {
            override fun onScreenCaptured() {
                ScreenshotFeedback.onScreenCaptured()
            }
        }
        screenCaptureCallback = callback
        runCatching { registerScreenCaptureCallback(mainExecutor, callback) }
            .onFailure { screenCaptureCallback = null }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= 34) {
            (screenCaptureCallback as? android.app.Activity.ScreenCaptureCallback)
                ?.let { runCatching { unregisterScreenCaptureCallback(it) } }
        }
        screenCaptureCallback = null
        super.onDestroy()
    }

    /** Android 13+ 通知权限，用于朗读通知栏与锁屏媒体控制。 */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
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

    // 平板暂时限制进入：尚未完成适配，仅弹提示并允许退出
    val isTabletDevice = LocalConfiguration.current.smallestScreenWidthDp >= 600
    if (isTabletDevice) {
        val host = context as? android.app.Activity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        MochiDialog(
            visible = true,
            onDismissRequest = { },
            title = "设备适配中",
            message = "当前版本尚未完成平板适配，请使用手机进入。",
            actions = persistentListOf(
                MochiDialogAction("好") { host?.finish() },
            ),
        )
        return
    }

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

    // 手机：主界面锁定竖屏，文章/作者页默认竖屏并支持旋转。
    // 平板：仍由实验性功能开关控制是否允许横竖屏切换。
    val tabletRotation by SettingsRepository.tabletRotation
    val activity = LocalContext.current as? android.app.Activity
    val isPhone = LocalConfiguration.current.smallestScreenWidthDp < 600
    val atHome = stack.lastOrNull() is Screen.Home
    DisposableEffect(tabletRotation, isPhone, atHome) {
        activity?.requestedOrientation = when {
            !isPhone -> if (tabletRotation) {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            } else {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            atHome -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
        onDispose { }
    }

    // 截图纠错：系统截屏回调触发计数，这里弹出可点击的 mochi 提示
    val toast = rememberMochiToastState()
    val captureCount by ScreenshotFeedback.captureCount
    LaunchedEffect(captureCount) {
        if (captureCount > 0) {
            toast.show(
                title = "发现问题？单击此处反馈",
                subtitle = "点击后跳转至QQ用户群",
                icon = { tint ->
                    Icon(Icons.Rounded.BugReport, contentDescription = null, tint = tint)
                },
                durationMillis = 5000,
                onClick = { ScreenshotFeedback.reportProblem(context) },
            )
        }
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

        MochiToastHost(toast)
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

package com.reciteus2.beishuwa

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ui.feice.mochi.basic.MochiAlign
import ui.feice.mochi.basic.MochiActivityIndicator
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiButtonStyle
import ui.feice.mochi.basic.MochiDialog
import ui.feice.mochi.basic.MochiDialogAction
import ui.feice.mochi.basic.MochiIconButton
import ui.feice.mochi.basic.MochiPopoverBox
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.theme.LocalContentColor
import ui.feice.mochi.theme.MochiTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val experimental by remember { SettingsRepository.experimental }
    val audioEnabled by remember { SettingsRepository.audioEnabled }
    val audioSource by remember { SettingsRepository.audioSource }
    var sourceMenuOpen by remember { mutableStateOf(false) }
    var importingAudio by remember { mutableStateOf(false) }
    var importedAudioCount by remember { mutableStateOf(AudioLibrary.importedCount(context)) }
    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                importingAudio = true
                val result = withContext(Dispatchers.IO) { AudioLibrary.importZip(context, uri) }
                importingAudio = false
                importedAudioCount = AudioLibrary.importedCount(context)
                val message = when {
                    result.error != null -> "导入失败：${result.error}"
                    result.imported > 0 && result.skipped > 0 ->
                        "已导入 ${result.imported} 篇，${result.skipped} 个文件未匹配"
                    result.imported > 0 -> "已导入 ${result.imported} 篇录音"
                    else -> "未找到可匹配的录音文件"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val screenshotFeedback by remember { SettingsRepository.screenshotFeedbackEnabled }
    val tabletRotation by remember { SettingsRepository.tabletRotation }
    val useAaasSpec by remember { SettingsRepository.useAaasSpec }
    var showExperimentalPanel by remember { mutableStateOf(false) }
    // 右侧浮动面板：用 MutableTransitionState 驱动进出场动画，退出动画播完再移除 Popup
    val experimentalPanelTransition = remember { MutableTransitionState(false) }
    var checkingConnection by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    val customFont by remember { SettingsRepository.useCustomFont }
    val fontScale by remember { SettingsRepository.fontScale }
    val pinchZoom by remember { SettingsRepository.pinchZoomEnabled }
    val advancedSheet = rememberMochiSheetState()
    val aiModel by remember { SettingsRepository.aiModel }
    val aiBaseUrl by remember { SettingsRepository.aiBaseUrl }
    val aiApiKey by remember { SettingsRepository.aiApiKey }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "设置",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
        )

        Surface(
            shape = MochiTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExperimentalPanel = true }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Text(
                        text = "实验性功能",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(1.dp),
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAbout = !showAbout }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "关于",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "背书哇 1.6.5",
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

                AboutDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openUrl(context, UPDATE_URL) }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "检查更新",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "用浏览器打开 GitHub 发布页，查看最新版本。",
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

                AboutDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "截图纠错",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "截图后弹出反馈入口，一键加入 QQ 群反馈问题。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MochiSwitch(
                        checked = screenshotFeedback,
                        onCheckedChange = { SettingsRepository.setScreenshotFeedback(context, it) },
                    )
                }
            }
        }

        MochiDialog(
            visible = showAbout,
            onDismissRequest = { showAbout = false },
            title = "软件信息",
            content = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    AboutRow("软件名称", "背书哇")
                    AboutDivider()
                    AboutRow("版本号", "1.6.5 · 2026-11-23")
                    AboutDivider()
                    AboutRow("作者", "@喔糖圆鼠")
                    AboutDivider()
                    AboutRow("共创作者", "@F15EX @落星尘 @Motziki @やわらかな光")
                    AboutDivider()
                    AboutRow("鸣谢", "@2friend @Clover-Yan 《重庆日报》 南昌雷式学校")
                    AboutDivider()
                    AboutRow("录音资源", "人民教育出版社")
                    AboutDivider()
                    AboutLinkRow(
                        label = "UI",
                        value = "https://github.com/feice11/mochi",
                        onClick = { openUrl(context, "https://github.com/feice11/mochi") },
                    )
                    AboutDivider()
                    AboutLinkRow(
                        label = "交流群",
                        value = ScreenshotFeedback.QQ_GROUP_NUMBER,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(ScreenshotFeedback.QQ_GROUP_NUMBER))
                            Toast.makeText(context, "群号已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        },
                    )
                    AboutDivider()
                    AboutLinkRow(
                        label = "正文资源",
                        value = "https://github.com/clover-yan/gaokao-poetry",
                        onClick = { openUrl(context, "https://github.com/clover-yan/gaokao-poetry") },
                    )
                    AboutDivider()
                    AboutLinkRow(
                        label = "源代码地址",
                        value = "https://github.com/HelloZWH-0620/Reciting-us",
                        onClick = { openUrl(context, "https://github.com/HelloZWH-0620/Reciting-us") },
                    )
                }
            },
            actions = persistentListOf(
                MochiDialogAction("关闭") { showAbout = false },
            ),
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "字体",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            shape = MochiTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                var fontMenuOpen by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatSize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "字体",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "选择阅读界面使用的字体。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MochiPopoverBox(
                        expanded = fontMenuOpen,
                        onExpandedChange = { fontMenuOpen = it },
                        width = 200.dp,
                        align = MochiAlign.End,
                        anchor = {
                            Row(
                                modifier = Modifier
                                    .clickable { fontMenuOpen = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (customFont) "自定义字体" else "系统默认字体",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        popover = {
                            ProfileOptionRow(
                                label = "系统默认字体",
                                selected = !customFont,
                                onClick = {
                                    SettingsRepository.setCustomFont(context, false)
                                    fontMenuOpen = false
                                },
                            )
                            ProfileOptionRow(
                                label = "自定义字体",
                                selected = customFont,
                                onClick = {
                                    SettingsRepository.setCustomFont(context, true)
                                    fontMenuOpen = false
                                },
                            )
                        },
                    )
                }

                AboutDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FormatSize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "字体大小",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "调整正文、作者详情与字词的文字大小。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            SettingsRepository.setFontScale(context, fontScale - 0.05f)
                        },
                        enabled = fontScale > SettingsRepository.MIN_FONT_SCALE,
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "减小字体")
                    }
                    Text(
                        text = "${(fontScale * 100).roundToInt()}%",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = {
                            SettingsRepository.setFontScale(context, fontScale + 0.05f)
                        },
                        enabled = fontScale < SettingsRepository.MAX_FONT_SCALE,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "增大字体")
                    }
                }

                AboutDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "音频朗读",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "在正文页朗读文章，并按所选音源播放。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MochiSwitch(
                        checked = audioEnabled,
                        onCheckedChange = { SettingsRepository.setAudioEnabled(context, it) },
                    )
                }

                AboutDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.padding(horizontal = 7.dp))
                        Text(
                            text = "朗读音源",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    MochiPopoverBox(
                        expanded = sourceMenuOpen,
                        onExpandedChange = { sourceMenuOpen = it },
                        width = 260.dp,
                        align = MochiAlign.Start,
                        cover = true,
                        anchor = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sourceMenuOpen = true }
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = audioSourceLabel(audioSource),
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        popover = {
                            ProfileOptionRow(
                                label = "系统 TTS",
                                selected = audioSource == SettingsRepository.AUDIO_SOURCE_SYSTEM,
                                onClick = {
                                    SettingsRepository.setAudioSource(
                                        context,
                                        SettingsRepository.AUDIO_SOURCE_SYSTEM,
                                    )
                                    sourceMenuOpen = false
                                },
                            )
                            ProfileOptionRow(
                                label = "Edge 在线朗读",
                                selected = audioSource == SettingsRepository.AUDIO_SOURCE_EDGE,
                                onClick = {
                                    SettingsRepository.setAudioSource(
                                        context,
                                        SettingsRepository.AUDIO_SOURCE_EDGE,
                                    )
                                    sourceMenuOpen = false
                                },
                            )
                            ProfileOptionRow(
                                label = "内置录音",
                                selected = audioSource == SettingsRepository.AUDIO_SOURCE_MEDIA,
                                onClick = {
                                    SettingsRepository.setAudioSource(
                                        context,
                                        SettingsRepository.AUDIO_SOURCE_MEDIA,
                                    )
                                    sourceMenuOpen = false
                                },
                            )
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = audioSourceHint(audioSource),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !importingAudio) {
                                zipPicker.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "application/octet-stream",
                                    )
                                )
                            }
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "导入音频压缩包",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (importingAudio) {
                                    "正在解析压缩包…"
                                } else {
                                    "选择 zip 自动解析；已导入 $importedAudioCount 篇"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (importingAudio) {
                            MochiActivityIndicator(
                                size = 18.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "AI 相关",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            shape = MochiTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                var modelMenuOpen by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.padding(horizontal = 7.dp))
                        Text(
                            text = "出题模型",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    MochiPopoverBox(
                        expanded = modelMenuOpen,
                        onExpandedChange = { modelMenuOpen = it },
                        width = 260.dp,
                        align = MochiAlign.Start,
                        cover = true,
                        anchor = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { modelMenuOpen = true }
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = aiModelLabel(aiModel),
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        popover = {
                            ProfileOptionRow(
                                label = "DeepSeek V4.1 Flash 1017",
                                selected = aiModel == SettingsRepository.AI_MODEL_FLASH,
                                onClick = {
                                    SettingsRepository.setAiModel(context, SettingsRepository.AI_MODEL_FLASH)
                                    modelMenuOpen = false
                                },
                            )
                            ProfileOptionRow(
                                label = "DeepSeek V4.1 Pro 1017",
                                selected = aiModel == SettingsRepository.AI_MODEL_PRO,
                                onClick = {
                                    SettingsRepository.setAiModel(context, SettingsRepository.AI_MODEL_PRO)
                                    modelMenuOpen = false
                                },
                            )
                        },
                    )
                }

                AboutDivider()

                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Text(
                        text = "API Key",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "仅保存在本机，用于调用模型接口。",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    MochiTextField(
                        value = aiApiKey,
                        onValueChange = { SettingsRepository.setAiApiKey(context, it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "sk-…",
                        secure = true,
                    )
                }

                AboutDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { advancedSheet.show() }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Text(
                        text = "高级选项",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "存储",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            onClick = { showDialog = true },
            shape = MochiTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.padding(horizontal = 7.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "清除缓存",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "删除已缓存的作者资料",
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
        Spacer(Modifier.height(88.dp))
    }

    LaunchedEffect(showExperimentalPanel) {
        experimentalPanelTransition.targetState = showExperimentalPanel
    }

    if (experimentalPanelTransition.currentState || experimentalPanelTransition.targetState) {
        Popup(
            alignment = Alignment.TopStart,
            onDismissRequest = { showExperimentalPanel = false },
            properties = PopupProperties(focusable = true),
        ) {
            // 遮罩跟随开关淡入淡出
            val scrim by animateFloatAsState(
                targetValue = if (showExperimentalPanel) 1f else 0f,
                label = "experimentalScrim",
            )
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f * scrim))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showExperimentalPanel = false },
                )
                // 浮动面板：参考 mochi 浮动侧边栏，从右侧向左滑入，四周留 8dp 悬浮
                AnimatedVisibility(
                    visibleState = experimentalPanelTransition,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .fillMaxHeight()
                            .width(300.dp),
                        shape = MochiTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shadowElevation = 16.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 18.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "实验性功能",
                                    style = MochiTheme.textStyles.navTitle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                MochiIconButton(
                                    onClick = { showExperimentalPanel = false },
                                    style = MochiButtonStyle.Tonal,
                                    size = 32.dp,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "关闭",
                                        tint = MochiTheme.colorScheme.onSurface,
                                        modifier = Modifier.width(16.dp),
                                    )
                                }
                            }
                            ExperimentalToggleRow(
                                title = "平板横竖屏切换",
                                description = "允许平板随设备方向旋转。",
                                checked = tabletRotation,
                                onCheckedChange = { SettingsRepository.setTabletRotation(context, it) },
                            )
                            ExperimentalToggleRow(
                                title = "AAAS 题库规范",
                                description = "按 AAAS 格式出题，默认关闭。",
                                checked = useAaasSpec,
                                onCheckedChange = { SettingsRepository.setUseAaasSpec(context, it) },
                            )
                            ExperimentalToggleRow(
                                title = "双指缩放调整字体大小",
                                description = "在正文、作者详情与字词页面双指捏合即可缩放。",
                                checked = pinchZoom,
                                onCheckedChange = { SettingsRepository.setPinchZoom(context, it) },
                            )
                        }
                    }
                }
            }
        }
    }

    MochiSheet(
        state = advancedSheet,
        onDismiss = { advancedSheet.hide() },
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            // 右上角圆形对勾：验证连通性，成功后自动收起面板
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                MochiIconButton(
                    onClick = {
                        if (checkingConnection) return@MochiIconButton
                        checkingConnection = true
                        scope.launch {
                            val result = AiQuestionRepository.testConnection()
                            checkingConnection = false
                            result
                                .onSuccess { advancedSheet.hide() }
                                .onFailure { connectionError = it.message ?: "连接失败" }
                        }
                    },
                    enabled = !checkingConnection,
                    style = MochiButtonStyle.Filled,
                    size = 38.dp,
                ) {
                    if (checkingConnection) {
                        MochiActivityIndicator(
                            size = 18.dp,
                            color = LocalContentColor.current,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "验证并保存",
                            tint = LocalContentColor.current,
                            modifier = Modifier.width(18.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "接口地址",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "OpenAI 兼容接口，默认 DeepSeek。",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            MochiTextField(
                value = aiBaseUrl,
                onValueChange = { SettingsRepository.setAiBaseUrl(context, it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = SettingsRepository.DEFAULT_AI_BASE_URL,
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    MochiDialog(
        visible = connectionError != null,
        onDismissRequest = { connectionError = null },
        title = "连接失败",
        message = connectionError.orEmpty(),
        actions = persistentListOf(
            MochiDialogAction("好") { connectionError = null },
        ),
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("清除缓存") },
            text = { Text("确定要删除所有已缓存的作者资料吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        scope.launch {
                            AuthorContentRepository.clearCache(context)
                            Toast.makeText(context, "清除成功", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

private const val UPDATE_URL = "https://github.com/HelloZWH-0620/Recite-US_Android/releases"

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "未找到可用的浏览器", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp),
        )
        VerticalDivider()
        Text(
            text = value,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
    }
}

@Composable
private fun AboutLinkRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp),
        )
        VerticalDivider()
        Text(
            text = value,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    )
}

@Composable
private fun ExperimentalToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        MochiSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun aiModelLabel(model: String): String = when (model) {
    SettingsRepository.AI_MODEL_PRO -> "DeepSeek V4.1 Pro 1017"
    else -> "DeepSeek V4.1 Flash 1017"
}

private fun audioSourceLabel(source: String): String = when (source) {
    SettingsRepository.AUDIO_SOURCE_EDGE -> "Edge 在线朗读"
    SettingsRepository.AUDIO_SOURCE_MEDIA -> "内置录音"
    else -> "系统 TTS"
}

private fun audioSourceHint(source: String): String = when (source) {
    SettingsRepository.AUDIO_SOURCE_EDGE -> "使用微软 Edge 在线合成，音色云健（zh-CN-YunjianNeural），需要联网。"
    SettingsRepository.AUDIO_SOURCE_MEDIA -> "播放内置或导入的真人录音；篇目暂无录音时自动改用 Edge 在线朗读。"
    else -> "使用设备内置 TTS 引擎逐句朗读，无需联网；媒体卡片会显示当前句子。"
}

/** 弹出层里的单个选项行：文字 + 选中勾。 */
@Composable
private fun ProfileOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MochiTheme.textStyles.body,
            color = MochiTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MochiTheme.colorScheme.primary,
                modifier = Modifier.width(18.dp),
            )
        }
    }
}

@Composable
private fun AboutDivider() {
    Surface(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {}
}

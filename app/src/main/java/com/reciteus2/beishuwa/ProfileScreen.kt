package com.reciteus2.beishuwa

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ui.feice.mochi.basic.MochiButton
import ui.feice.mochi.basic.MochiSheet
import ui.feice.mochi.basic.MochiSwitch
import ui.feice.mochi.basic.MochiTextField
import ui.feice.mochi.basic.rememberMochiSheetState
import ui.feice.mochi.theme.MochiTheme
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val experimental by remember { SettingsRepository.experimental }
    val tabletRotation by remember { SettingsRepository.tabletRotation }
    val useAaasSpec by remember { SettingsRepository.useAaasSpec }
    var showExperimentalPanel by remember { mutableStateOf(false) }
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
                            text = "背书哇 v1.6.6111",
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

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("软件信息") },
                text = {
                    Column {
                        AboutRow("软件名称", "背书哇")
                        AboutDivider()
                        AboutRow("版本号", "v1.6.6111 · 2026-11-11")
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
                            label = "交流群",
                            value = "950739151",
                            onClick = {
                                clipboardManager.setText(AnnotatedString("950739151"))
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
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) {
                        Text("关闭")
                    }
                },
            )
        }

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
                    Box {
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
                        DropdownMenu(
                            expanded = fontMenuOpen,
                            onDismissRequest = { fontMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("系统默认字体") },
                                onClick = {
                                    SettingsRepository.setCustomFont(context, false)
                                    fontMenuOpen = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("自定义字体") },
                                onClick = {
                                    SettingsRepository.setCustomFont(context, true)
                                    fontMenuOpen = false
                                },
                            )
                        }
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
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(horizontal = 7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "双指缩放调整字体大小",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "在正文、作者详情与字词页面双指捏合即可缩放。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    MochiSwitch(
                        checked = pinchZoom,
                        onCheckedChange = { SettingsRepository.setPinchZoom(context, it) },
                    )
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
                    Box {
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
                        DropdownMenu(
                            expanded = modelMenuOpen,
                            onDismissRequest = { modelMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("DeepSeek V4.1 Flash 1017") },
                                onClick = {
                                    SettingsRepository.setAiModel(context, SettingsRepository.AI_MODEL_FLASH)
                                    modelMenuOpen = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("DeepSeek V4.1 Pro 1017") },
                                onClick = {
                                    SettingsRepository.setAiModel(context, SettingsRepository.AI_MODEL_PRO)
                                    modelMenuOpen = false
                                },
                            )
                        }
                    }
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

    if (showExperimentalPanel) {
        Popup(
            alignment = Alignment.CenterEnd,
            onDismissRequest = { showExperimentalPanel = false },
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .width(300.dp),
                shape = MochiTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 16.dp,
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "实验性功能",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showExperimentalPanel = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭")
                        }
                    }
                    ExperimentalToggleRow(
                        title = "音频朗读",
                        description = "正文页播放音频，并使用媒体通知控制。",
                        checked = experimental,
                        onCheckedChange = { SettingsRepository.setExperimental(context, it) },
                    )
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
                }
            }
        }
    }

    MochiSheet(
        state = advancedSheet,
        onDismiss = { advancedSheet.hide() },
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = "高级选项",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))
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
            Spacer(Modifier.height(24.dp))
            MochiButton(
                onClick = { advancedSheet.hide() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("完成")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

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

@Composable
private fun AboutDivider() {
    Surface(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {}
}

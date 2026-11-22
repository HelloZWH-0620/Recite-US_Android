package com.reciteus2.beishuwa

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/**
 * 逐字拼音字典（与 WebView 版同源：web/js/pinyin-dict.js，pypinyin 生成）。
 *
 * 词典是「单字 → 拼音」的静态映射，不做多音字上下文消歧——与 WebView 版行为一致，
 * 保证两个版本的注音结果不会互相打脸。
 */
object PinyinDict {
    private val cache = AtomicReference<Map<Char, String>?>(null)

    fun get(context: Context): Map<Char, String> {
        cache.get()?.let { return it }
        synchronized(this) {
            cache.get()?.let { return it }
            val json = context.assets.open("pinyin.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val obj = JSONObject(json)
            val map = HashMap<Char, String>(obj.length() * 2)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.length == 1) map[key[0]] = obj.getString(key)
            }
            cache.set(map)
            return map
        }
    }
}

/**
 * 逐字注音段落：每个汉字上方一行小号拼音，标点/空格按原样占位。
 *
 * 用 FlowRow 而不是单条 Text：Compose 没有原生 ruby，逐字单元既能保持汉字对齐，
 * 又能按字换行；配合 LazyColumn 只组合可见段落，长文也不会一次性布局全文。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RubyParagraph(
    text: String,
    dict: Map<Char, String>?,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    color: Color,
    pinyinColor: Color,
    highlight: Set<Int> = emptySet(),
    highlightColor: Color = color,
    firstLineIndent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val pinyinSize = (fontSize.value * 0.46f).sp
    val pinyinLine = (pinyinSize.value * 1.1f).sp
    val indentWidth = with(density) { (fontSize * 2).toDp() }
    val spaceWidth = with(density) { (fontSize * 0.5f).toDp() }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        // 行间留白：上一行汉字与下一行拼音之间需要呼吸感，否则拼音看起来像挂在上一行下面
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (firstLineIndent) Spacer(Modifier.width(indentWidth))

        text.forEachIndexed { index, ch ->
            when {
                ch == '\n' -> Unit
                ch == ' ' || ch == '\u3000' -> Spacer(Modifier.width(spaceWidth))
                else -> {
                    val pinyin = dict?.get(ch)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pinyin ?: "",
                            fontSize = pinyinSize,
                            lineHeight = pinyinLine,
                            color = pinyinColor,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = ch.toString(),
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = if (index in highlight) highlightColor else color,
                        )
                    }
                }
            }
        }
    }
}

package com.reciteus2.beishuwa

import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/**
 * 用户导入的录音库：把 zip 中的 mp3 解析并保存到应用私有目录，
 * 播放时优先使用导入录音，其次使用安装包内置录音。
 */
object AudioLibrary {
    private const val DIR_NAME = "imported_audio"

    data class ImportResult(val imported: Int, val skipped: Int, val error: String? = null)

    fun importedDir(context: Context): File = File(context.filesDir, DIR_NAME)

    fun importedFile(context: Context, articleId: String): File? =
        File(importedDir(context), "$articleId.mp3").takeIf { it.isFile && it.length() > 0L }

    fun hasAudio(context: Context, articleId: String): Boolean =
        importedFile(context, articleId) != null || assetExists(context, articleId)

    fun importedCount(context: Context): Int =
        importedDir(context).listFiles()?.count { it.isFile && it.extension.equals("mp3", true) } ?: 0

    fun clearImported(context: Context) {
        importedDir(context).listFiles()?.forEach { it.delete() }
    }

    private fun assetExists(context: Context, articleId: String): Boolean =
        runCatching { context.assets.list("audio")?.contains("$articleId.mp3") == true }
            .getOrDefault(false)

    /**
     * 解析 zip 压缩包。文件名可以是篇目 id 或作品标题，允许任意层目录；
     * UTF-8 未匹配到任何文件时再用 GBK 重试一次，兼容 Windows 生成的压缩包。
     */
    fun importZip(context: Context, uri: Uri): ImportResult {
        val utf8 = importWithCharset(context, uri, Charsets.UTF_8)
        if (utf8.error != null || utf8.imported > 0) return utf8
        return importWithCharset(context, uri, Charset.forName("GBK"))
    }

    private fun importWithCharset(context: Context, uri: Uri, charset: Charset): ImportResult {
        val data = ReciteRepository.load(context)
        val keyToId = HashMap<String, String>(data.articles.size * 2)
        data.articles.forEach { article ->
            keyToId[normalize(article.id)] = article.id
            keyToId[normalize(article.title)] = article.id
        }

        val dir = importedDir(context).apply { mkdirs() }
        var imported = 0
        var skipped = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { raw ->
                ZipInputStream(raw.buffered(), charset).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val fileName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                        if (!entry.isDirectory && fileName.endsWith(".mp3", ignoreCase = true)) {
                            val id = keyToId[normalize(fileName.substringBeforeLast('.'))]
                            if (id == null) {
                                skipped++
                            } else {
                                val target = File(dir, "$id.mp3")
                                val temp = File(dir, "$id.mp3.tmp")
                                temp.outputStream().use { out -> zip.copyTo(out, 64 * 1024) }
                                if (temp.length() > 0L) {
                                    if (target.exists()) target.delete()
                                    temp.renameTo(target)
                                    imported++
                                } else {
                                    temp.delete()
                                    skipped++
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: return ImportResult(0, 0, "无法读取所选文件")
        } catch (e: Exception) {
            return ImportResult(imported, skipped, e.message ?: "压缩包解析失败")
        }

        return ImportResult(imported, skipped)
    }

    private val normalizeRegex =
        Regex("[\\s《》〈〉“”\"'’‘·,，.。!！?？:：;；()（）\\[\\]【】<>/\\\\_-]")

    private fun normalize(value: String): String = value.lowercase().replace(normalizeRegex, "")
}

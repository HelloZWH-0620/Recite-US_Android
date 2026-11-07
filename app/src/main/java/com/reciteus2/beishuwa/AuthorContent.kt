package com.reciteus2.beishuwa

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AuthorContent(
    val markdown: String,
    val fromCache: Boolean,
)

object AuthorContentRepository {

    private const val BASE_URL = "https://m.guwendao.net/authorv.aspx?name="

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 16; Pixel 9) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    fun urlFor(author: Author): String =
        BASE_URL + URLEncoder.encode(author.queryName.ifBlank { author.name }, "UTF-8")

    suspend fun load(context: Context, author: Author): AuthorContent = withContext(Dispatchers.IO) {
        val directory = cacheDirectory(context)
        val cacheFile = File(directory, "${author.id}.md")
        if (cacheFile.isFile && cacheFile.length() > 0L) {
            return@withContext AuthorContent(cacheFile.readText(), fromCache = true)
        }

        val markdown = fetchMarkdown(author, urlFor(author))
        cacheFile.writeText(markdown)
        AuthorContent(markdown, fromCache = false)
    }

    suspend fun clearCache(context: Context): Int = withContext(Dispatchers.IO) {
        val files = cacheDirectory(context).listFiles { file ->
            file.isFile && file.name.endsWith(".md", ignoreCase = true)
        } ?: return@withContext 0
        var count = 0
        files.forEach { file ->
            if (file.delete()) count++
        }
        count
    }

    private fun cacheDirectory(context: Context): File =
        File(context.filesDir, "authors").apply { if (!exists()) mkdirs() }

    private fun fetchMarkdown(author: Author, url: String): String {
        val html = httpGet(url)
        val document = Jsoup.parse(html, url)

        val title = document.selectFirst("h1")
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: author.name

        val bioElement = document.selectFirst("p.author-bio")
            ?: document.selectFirst("h1 ~ p")
        val bio = bioElement?.clone()?.apply { select("a").remove() }
            ?.text()
            ?.trim()
            .orEmpty()

        val sections = document.select("div.contyishang").mapNotNull { container ->
            val heading = container.selectFirst("h2")?.text()?.trim().orEmpty()
            val paragraphs = container.select("p")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
            if (heading.isBlank() || paragraphs.isEmpty()) null else heading to paragraphs
        }.distinctBy { it.first }

        if (bio.isBlank() && sections.isEmpty()) {
            throw IOException("未能解析作者页面内容")
        }

        return buildString {
            append("# ").append(title).append("\n\n")
            if (bio.isNotBlank()) {
                append("> ").append(bio).append("\n\n")
            }
            sections.forEach { (heading, paragraphs) ->
                append("## ").append(heading).append("\n\n")
                paragraphs.forEach { paragraph ->
                    append(paragraph).append("\n\n")
                }
            }
        }.trimEnd()
    }

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", MOBILE_UA)
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("服务器返回 $code")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

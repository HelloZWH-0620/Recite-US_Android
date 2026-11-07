package com.reciteus2.beishuwa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Note(
    val phrase: String,
    val pos: String,
    val explain: String,
)

data class Article(
    val id: String,
    val title: String,
    val author: String,
    val authorId: String,
    val category: String,
    val text: String,
    val translation: String,
    val notes: List<Note>,
    val appreciation: String,
) {
    val isPoem: Boolean get() = category == "poem"

    // 词、曲及戏曲标题首行保留缩进；普通诗歌不需要首行空两格
    val keepFirstLineIndent: Boolean
        get() = CI_TITLE_PREFIXES.any { title.startsWith(it) }

    private companion object {
        val CI_TITLE_PREFIXES = listOf(
            "菩萨蛮", "虞美人", "望海潮", "桂枝香", "江城子", "念奴娇", "鹊桥仙",
            "苏幕遮", "声声慢", "永遇乐", "青玉案", "贺新郎", "扬州慢", "长亭送别", "朝天子",
        )
    }
}

data class Author(
    val id: String,
    val name: String,
    val dynasty: String,
    val dynastyRaw: String,
    val bio: String,
    val queryName: String = "",
)

class ReciteData(
    val articles: List<Article>,
    val authors: List<Author>,
) {
    val authorsById: Map<String, Author> = authors.associateBy { it.id }

    fun authorsOfDynasty(dynasty: String): List<Author> =
        authors.filter { it.dynasty == dynasty }

    companion object {
        val DYNASTY_ORDER = listOf("先秦", "汉", "魏晋南北朝", "唐", "五代", "宋", "元", "明", "清")
    }
}

object ReciteRepository {
    @Volatile
    private var cached: ReciteData? = null

    fun load(context: Context): ReciteData =
        cached ?: synchronized(this) {
            cached ?: parse(
                context.assets.open("recite.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            ).also { cached = it }
        }

    private fun parse(json: String): ReciteData {
        val root = JSONObject(json)

        val authorsJson = root.getJSONArray("authors")
        val authors = ArrayList<Author>(authorsJson.length())
        for (i in 0 until authorsJson.length()) {
            val o = authorsJson.getJSONObject(i)
            authors += Author(
                id = o.getString("id"),
                name = o.getString("name"),
                dynasty = o.getString("dynasty"),
                dynastyRaw = o.optString("dynastyRaw"),
                bio = o.optString("bio"),
                queryName = o.optString("queryName"),
            )
        }

        val articlesJson = root.getJSONArray("articles")
        val articles = ArrayList<Article>(articlesJson.length())
        for (i in 0 until articlesJson.length()) {
            val o = articlesJson.getJSONObject(i)
            articles += Article(
                id = o.getString("id"),
                title = o.getString("title"),
                author = o.getString("author"),
                authorId = o.getString("authorId"),
                category = o.getString("category"),
                text = o.optString("text"),
                translation = o.optString("translation"),
                notes = o.optJSONArray("notes").toNotes(),
                appreciation = o.optString("appreciation"),
            )
        }
        return ReciteData(articles, authors)
    }

    private fun JSONArray?.toNotes(): List<Note> {
        if (this == null) return emptyList()
        val list = ArrayList<Note>(length())
        for (i in 0 until length()) {
            val o = getJSONObject(i)
            list += Note(
                phrase = o.optString("phrase"),
                pos = o.optString("pos"),
                explain = o.optString("explain"),
            )
        }
        return list
    }
}

package com.reciteus2.beishuwa

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/** 一道理解性默写题。 */
data class PracticeQuestion(
    val stem: String,
    val answers: List<String>,
    val standardAnswer: String,
    val knowledge: String? = null,
)

object AiQuestionRepository {

    /** 部分服务不接受自定义模型名时的兜底模型。 */
    private const val FALLBACK_MODEL = "deepseek-chat"

    /**
     * 调用自定义 AI 模型，为指定篇目生成理解性默写题。
     * 开启「AAAS 题库规范」实验项时按 AAAS 格式出题，否则使用简化 JSON。
     */
    suspend fun generateMemorization(
        context: Context,
        article: Article,
        count: Int = 5,
    ): Result<List<PracticeQuestion>> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = SettingsRepository.aiApiKey.value.trim()
            if (apiKey.isEmpty()) {
                throw IllegalStateException("请先在「我的 - AI 相关」填写 API Key")
            }
            val baseUrl = SettingsRepository.aiBaseUrl.value.trim().trimEnd('/')
            if (baseUrl.isEmpty()) {
                throw IllegalStateException("请先在「我的 - AI 相关」填写接口地址")
            }
            val useAaas = SettingsRepository.useAaasSpec.value
            val endpoint = if (baseUrl.endsWith("/chat/completions")) {
                baseUrl
            } else {
                "$baseUrl/chat/completions"
            }

            val first = post(
                endpoint = endpoint,
                apiKey = apiKey,
                body = buildBody(SettingsRepository.aiModel.value, article, count, useAaas, jsonMode = true),
            )
            // 自定义模型名 / response_format 不被支持时，退回标准模型再试一次
            val result = if (first.first == 400 || first.first == 404 || first.first == 422) {
                post(
                    endpoint = endpoint,
                    apiKey = apiKey,
                    body = buildBody(FALLBACK_MODEL, article, count, useAaas, jsonMode = false),
                )
            } else {
                first
            }
            if (result.first !in 200..299) {
                throw IllegalStateException("请求失败（${result.first}）：${result.second.take(300)}")
            }
            parseQuestions(result.second)
        }
    }

    private fun buildBody(
        model: String,
        article: Article,
        count: Int,
        useAaas: Boolean,
        jsonMode: Boolean,
    ): JSONObject = JSONObject().apply {
        put("model", model)
        put("temperature", 0.7)
        if (jsonMode) put("response_format", JSONObject().put("type", "json_object"))
        put("messages", JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put(
                    "content",
                    if (useAaas) {
                        "你是一位高中语文老师，擅长依据古诗文原文出理解性默写题，并严格按 AAAS 题库格式输出 JSON。"
                    } else {
                        "你是一位高中语文老师，擅长依据古诗文原文出理解性默写题，并按要求输出 JSON。"
                    },
                )
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", buildUserPrompt(article, count, useAaas))
            })
        })
    }

    private fun post(endpoint: String, apiKey: String, body: JSONObject): Pair<Int, String> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return code to text
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUserPrompt(article: Article, count: Int, useAaas: Boolean): String = buildString {
        appendLine("请为下面的高中古诗文出 $count 道理解性默写题（填空）：")
        appendLine("篇目：《${article.title}》")
        appendLine("作者：${article.author}")
        appendLine("正文：")
        appendLine(article.text.take(4000))
        appendLine()
        appendLine("要求：")
        appendLine("1. 每道题给出一个语境式的理解性描述，用 ______ 表示一个空缺，每道题只挖 1 处。")
        appendLine("2. 只考正文原句，答案能被原文精确命中，不要自己改写诗句。")
        appendLine("3. 答案可能存在的同义写法（如异体字、标点差异）都放进 answer 数组。")
        if (useAaas) {
            appendLine("4. 严格输出 AAAS 题库格式的 JSON，不要输出 Markdown 代码块或解释文字。")
            appendLine(AAAS_EXAMPLE)
        } else {
            appendLine("4. 只输出一个 JSON 对象，不要输出 Markdown 代码块或解释文字，格式如下：")
            appendLine(SIMPLE_EXAMPLE)
        }
    }

    private fun parseQuestions(raw: String): List<PracticeQuestion> {
        val json = extractJson(raw)
        val root = JSONObject(json)
        val questions = root.optJSONArray("questions")
            ?: throw IllegalStateException("模型未返回 questions 数组")
        val result = ArrayList<PracticeQuestion>()
        for (i in 0 until questions.length()) {
            val item = questions.optJSONObject(i) ?: continue
            val stem = item.optString("stem").trim()
            if (stem.isEmpty()) continue
            val answers = ArrayList<String>()
            when (val answer = item.opt("answer")) {
                is JSONArray -> for (j in 0 until answer.length()) {
                    answer.optString(j).takeIf { it.isNotBlank() }?.let(answers::add)
                }
                is String -> if (answer.isNotBlank()) answers.add(answer)
            }
            val standard = item.optString("standard_answer").trim()
                .ifEmpty { answers.firstOrNull().orEmpty() }
            if (answers.isEmpty() && standard.isBlank()) continue
            val knowledge = item.optJSONArray("knowledge")?.optString(0)?.takeIf { it.isNotBlank() }
            result.add(
                PracticeQuestion(
                    stem = stem.replace("\\n", "\n"),
                    answers = if (answers.isNotEmpty()) answers else listOf(standard),
                    standardAnswer = standard,
                    knowledge = knowledge,
                )
            )
        }
        if (result.isEmpty()) throw IllegalStateException("模型没有生成有效题目，请重试")
        return result
    }

    /** 兼容模型把 JSON 包在 ```json 代码块里的情况。 */
    private fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) throw IllegalStateException("模型返回内容无法解析")
        return raw.substring(start, end + 1)
    }

    private const val SIMPLE_EXAMPLE = """
{"questions":[
  {"stem":"《劝学》中以蚯蚓为例，说明用心专一才能有所成就的句子是“______”。",
   "answer":["蚓无爪牙之利，筋骨之强"],
   "standard_answer":"蚓无爪牙之利，筋骨之强"}
]}
"""

    private const val AAAS_EXAMPLE = """
{"version":1,"questions":[
  {"subject":"语文","authors":["AI"],"date":"2026.11.22","revision":10,"points":2.0,
   "instruction":"补写出下列句子中的空缺部分。","source":"背书哇",
   "type":"blank","knowledge":["理解性默写"],
   "stem":"《劝学》中以蚯蚓为例，说明用心专一才能有所成就的句子是“______”。",
   "answer":["蚓无爪牙之利，筋骨之强"],"standard_answer":"蚓无爪牙之利，筋骨之强","scoring":"auto"}
]}
"""
}

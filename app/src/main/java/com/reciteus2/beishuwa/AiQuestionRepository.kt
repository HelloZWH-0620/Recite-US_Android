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
     * 调用自定义 AI 模型，为一篇或多篇篇目生成理解性默写题。
     * 开启「AAAS 题库规范」实验项时按 AAAS 格式出题，否则使用简化 JSON。
     */
    suspend fun generateMemorization(
        context: Context,
        articles: List<Article>,
        count: Int = 5,
    ): Result<List<PracticeQuestion>> = withContext(Dispatchers.IO) {
        runCatching {
            if (articles.isEmpty()) throw IllegalStateException("请至少选择一篇篇目")
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

            var result = post(
                endpoint = endpoint,
                apiKey = apiKey,
                body = buildBody(SettingsRepository.aiModel.value, articles, count, useAaas, jsonMode = true),
            )
            // ① response_format 不被支持：保留原模型名、关闭 json_mode 再试
            if (result.first == 400 || result.first == 404 || result.first == 422) {
                result = post(
                    endpoint = endpoint,
                    apiKey = apiKey,
                    body = buildBody(SettingsRepository.aiModel.value, articles, count, useAaas, jsonMode = false),
                )
            }
            // ② 仍然报模型不存在：换标准模型兜底
            if ((result.first == 400 || result.first == 404 || result.first == 422) &&
                (result.second.contains("model", ignoreCase = true) || result.second.contains("模型"))
            ) {
                result = post(
                    endpoint = endpoint,
                    apiKey = apiKey,
                    body = buildBody(FALLBACK_MODEL, articles, count, useAaas, jsonMode = false),
                )
            }
            if (result.first !in 200..299) {
                throw IllegalStateException("请求失败（${result.first}）：${result.second.take(300)}")
            }
            parseQuestions(result.second)
        }
    }

    /**
     * 连通性验证：用当前接口地址 / API Key / 模型发一个最小请求。
     * 成功返回 Unit；失败抛出带错误信息的异常（HTTP 状态码 + 响应片段）。
     */
    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = SettingsRepository.aiApiKey.value.trim()
            if (apiKey.isEmpty()) {
                throw IllegalStateException("请先填写 API Key")
            }
            val baseUrl = SettingsRepository.aiBaseUrl.value.trim().trimEnd('/')
            if (baseUrl.isEmpty()) {
                throw IllegalStateException("请先填写接口地址")
            }
            val endpoint = if (baseUrl.endsWith("/chat/completions")) {
                baseUrl
            } else {
                "$baseUrl/chat/completions"
            }
            val model = SettingsRepository.aiModel.value
            fun testBody(modelName: String): JSONObject = JSONObject().apply {
                put("model", modelName)
                put("max_tokens", 1)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
            }
            var (code, text) = post(endpoint, apiKey, testBody(model))
            // 自定义模型名不被接口支持时，用标准模型再验证一次（避免误报「连接失败」）
            if (code == 400 || code == 404 || code == 422) {
                val retry = post(endpoint, apiKey, testBody(FALLBACK_MODEL))
                if (retry.first in 200..299) return@runCatching
                if (retry.second.contains("model", ignoreCase = true) ||
                    retry.second.contains("模型")
                ) {
                    throw IllegalStateException(
                        "模型名不被接口支持（已尝试 $model / $FALLBACK_MODEL）：${retry.second.take(300)}"
                    )
                }
                code = retry.first
                text = retry.second
            }
            if (code !in 200..299) {
                throw IllegalStateException("请求失败（$code）：${text.take(300)}")
            }
        }
    }

    private fun buildBody(
        model: String,
        articles: List<Article>,
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
                put("content", buildUserPrompt(articles, count, useAaas))
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

    private fun buildUserPrompt(articles: List<Article>, count: Int, useAaas: Boolean): String = buildString {
        appendLine("请为下面的高中古诗文出 $count 道理解性默写题（填空），可跨篇目组合：")
        articles.forEachIndexed { index, article ->
            appendLine("篇目 ${index + 1}：《${article.title}》")
            appendLine("作者：${article.author}")
            appendLine("正文：")
            appendLine(article.text.take(3000))
            appendLine()
        }
        appendLine("要求：")
        appendLine("1. 每道题给出一个语境式的理解性描述，用 {{1}} 表示一个空缺，每道题只挖 1 处。")
        appendLine("2. 只考正文原句，答案能被原文精确命中，不要自己改写诗句。")
        appendLine("3. 答案可能存在的同义写法（如异体字、标点差异）都放进 answer 数组。")
        appendLine("4. 输出必须是合法的 json，根字段为 questions 数组；每道题的题干写在 stem 字段，答案写在 answer 字段。")
        if (useAaas) {
            appendLine("5. 严格按 AAAS 题库格式输出，questions 里的每道题直接写 type/stem/answer 等字段，不要用 items 嵌套；不要输出 Markdown 代码块或解释文字。")
            appendLine(AAAS_EXAMPLE)
        } else {
            appendLine("5. 只输出一个 JSON 对象，不要输出 Markdown 代码块或解释文字，格式如下：")
            appendLine(SIMPLE_EXAMPLE)
        }
    }

    /** 把一道 question（可能含 items）展开成可直接展示的题目列表。 */
    private fun parseQuestions(raw: String, depth: Int = 0): List<PracticeQuestion> {
        if (depth > 2) throw IllegalStateException("模型返回内容嵌套过深，无法解析")
        val json = extractJson(raw)
        val result = ArrayList<PracticeQuestion>()

        // 兼容模型直接把题目数组作为根节点
        val rootArray = runCatching { JSONArray(json) }.getOrNull()
        if (rootArray != null) {
            for (i in 0 until rootArray.length()) {
                rootArray.optJSONObject(i)?.let { addFromQuestion(it, result) }
            }
            if (result.isEmpty()) {
                throw IllegalStateException("模型返回内容无法解析：${raw.take(200)}")
            }
            return result
        }

        val root = runCatching { JSONObject(json) }.getOrElse {
            throw IllegalStateException("模型返回内容无法解析：${raw.take(200)}")
        }

        // 标准 OpenAI 响应：真正的内容在 choices[0].message.content 里
        val messageContent = root.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
        if (!messageContent.isNullOrBlank()) {
            return parseQuestions(messageContent, depth + 1)
        }

        val questions = root.optJSONArray("questions")
            ?: root.optJSONArray("items")
            ?: root.optJSONObject("data")?.optJSONArray("questions")
            ?: if (root.has("stem")) JSONArray().put(root) else null
            ?: throw IllegalStateException("模型未返回 questions 数组：${raw.take(300)}")
        for (i in 0 until questions.length()) {
            questions.optJSONObject(i)?.let { addFromQuestion(it, result) }
        }
        if (result.isEmpty()) throw IllegalStateException("模型没有生成有效题目，请重试")
        return result
    }

    private fun addFromQuestion(question: JSONObject, result: MutableList<PracticeQuestion>) {
        val items = question.optJSONArray("items")
        if (items != null) {
            for (j in 0 until items.length()) {
                items.optJSONObject(j)?.let { parseDetail(it)?.let(result::add) }
            }
        } else {
            parseDetail(question)?.let(result::add)
        }
    }

    private fun parseDetail(item: JSONObject): PracticeQuestion? {
        val rawStem = item.optString("stem").trim()
        if (rawStem.isEmpty()) return null
        // AAAS 的 {{1}} 空位统一显示为 ______
        val stem = rawStem.replace(Regex("\\{\\{\\s*\\d+\\s*\\}\\}"), "______").replace("\\n", "\n")
        val answers = ArrayList<String>()
        when (val answer = item.opt("answer")) {
            is JSONArray -> for (j in 0 until answer.length()) {
                when (val value = answer.opt(j)) {
                    is JSONObject -> value.optString("value")
                        .takeIf { it.isNotBlank() }?.let(answers::add)
                    else -> answer.optString(j).takeIf { it.isNotBlank() }?.let(answers::add)
                }
            }
            is String -> if (answer.isNotBlank()) answers.add(answer)
        }
        val standard = item.optString("standard_answer").trim()
            .ifEmpty { answers.joinToString("，") }
        if (answers.isEmpty() && standard.isBlank()) return null
        val knowledge = item.optJSONArray("knowledge")?.optString(0)?.takeIf { it.isNotBlank() }
        return PracticeQuestion(
            stem = stem,
            answers = if (answers.isNotEmpty()) answers else listOf(standard),
            standardAnswer = standard,
            knowledge = knowledge,
        )
    }

    /** 兼容模型把 JSON 包在 ```json 代码块里、或直接返回数组的情况。 */
    private fun extractJson(raw: String): String {
        val objectStart = raw.indexOf('{')
        val arrayStart = raw.indexOf('[')
        val start = listOf(objectStart, arrayStart).filter { it >= 0 }.minOrNull() ?: -1
        val end = maxOf(raw.lastIndexOf('}'), raw.lastIndexOf(']'))
        if (start < 0 || end <= start) {
            throw IllegalStateException("模型返回内容无法解析：${raw.take(200)}")
        }
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

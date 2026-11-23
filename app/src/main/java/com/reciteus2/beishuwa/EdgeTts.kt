package com.reciteus2.beishuwa

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * Edge 在线朗读：把一段文本合成为 mp3 缓存文件。
 *
 * 走 Edge 阅读器的非官方 WebSocket 接口，逐句合成，失败时返回 false，
 * 由调用方决定降级策略。所有方法都是阻塞的，必须放在工作线程调用。
 *
 * 接口鉴权在 2026 年升级为 Sec-MS-GEC v2（CS64 + 修改版 SHA-256），
 * 算法与 rany2/edge-tts 的 drm.py 保持一致。
 */
object EdgeTts {

    const val DEFAULT_VOICE = "zh-CN-YunjianNeural"

    private const val TAG = "EdgeTts"
    private const val TRUSTED_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
    private const val SEC_MS_GEC_VERSION = "2-$CHROMIUM_FULL_VERSION"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"
    private const val ORIGIN = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** 是否还有可以联网的合成请求 */
    fun isAvailable(): Boolean = true

    /**
     * 合成 [text] 到 [outFile]。若缓存文件已存在则直接复用。
     * @return 合成成功返回 true。
     */
    fun synthesize(text: String, outFile: File, voice: String = DEFAULT_VOICE): Boolean {
        if (outFile.exists() && outFile.length() > 0L) return true
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return false

        val audio = ByteArrayOutputStream()
        val done = CountDownLatch(1)
        val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
        var finished = false

        val request = Request.Builder()
            .url(buildUrl())
            .header("Origin", ORIGIN)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Cookie", "muid=${randomHex(32)};")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "websocket open")
                webSocket.send(configMessage())
                webSocket.send(ssmlMessage(cleaned, voice))
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // 二进制帧：前 2 字节为大端 header 长度，其后是 header，再之后是音频数据
                val data = bytes.toByteArray()
                if (data.size < 2) return
                val headerLength = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                val headerEnd = 2 + headerLength
                if (headerEnd > data.size) return
                val header = String(data, 2, headerLength, Charsets.UTF_8)
                if (header.contains("Path:audio", ignoreCase = true)) {
                    audio.write(data, headerEnd, data.size - headerEnd)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end", ignoreCase = true)) {
                    finished = true
                    done.countDown()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!cancelled.get()) {
                    Log.w(TAG, "websocket failure: code=${response?.code} ${t.message}", t)
                }
                done.countDown()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                done.countDown()
            }
        }

        val webSocket = client.newWebSocket(request, listener)
        return try {
            done.await(25, TimeUnit.SECONDS)
            val data = audio.toByteArray()
            if (!finished || data.isEmpty()) {
                Log.w(TAG, "synthesize failed finished=$finished bytes=${data.size}")
                false
            } else {
                outFile.parentFile?.mkdirs()
                val tmp = File(outFile.parentFile, outFile.name + ".tmp")
                tmp.writeBytes(data)
                if (outFile.exists()) outFile.delete()
                tmp.renameTo(outFile)
                Log.d(TAG, "synthesize ok bytes=${data.size} -> ${outFile.name}")
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "synthesize exception", e)
            false
        } finally {
            cancelled.set(true)
            runCatching { webSocket.cancel() }
        }
    }

    private fun buildUrl(): String {
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        return "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
            "?TrustedClientToken=$TRUSTED_TOKEN" +
            "&Sec-MS-GEC=${secMsGec()}" +
            "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION" +
            "&ConnectionId=$connectionId"
    }

    private fun randomHex(length: Int): String {
        val chars = "0123456789ABCDEF"
        val random = java.security.SecureRandom()
        return buildString(length) {
            repeat(length) { append(chars[random.nextInt(16)]) }
        }
    }

    private fun timestamp(): String {
        val format = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }

    private fun configMessage(): String = buildString {
        append("X-Timestamp:${timestamp()}\r\n")
        append("Content-Type:application/json; charset=utf-8\r\n")
        append("Path:speech.config\r\n\r\n")
        append(
            """{"context":{"synthesis":{"audio":{"metadataoptions":""" +
                """{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},""" +
                """"outputFormat":"audio-24khz-48kbitrate-mono-mp3"}}}}"""
        )
    }

    private fun ssmlMessage(text: String, voice: String): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
        return buildString {
            append("X-RequestId:${UUID.randomUUID().toString().replace("-", "")}\r\n")
            append("Content-Type:application/ssml+xml\r\n")
            append("X-Timestamp:${timestamp()}\r\n")
            append("Path:ssml\r\n\r\n")
            append(
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-CN'>" +
                    "<voice name='$voice'>" +
                    "<prosody rate='0%' pitch='0Hz'>$escaped</prosody>" +
                    "</voice></speak>"
            )
        }
    }

    // ---------------- Sec-MS-GEC v2 ----------------

    /**
     * Sec-MS-GEC v2：格式 `<CS64 16位>-<SHA256 64位>`，签名输入是
     * 「对齐到 5 分钟窗口的 Windows 文件时间（100ns）+ 固定令牌」。
     */
    private fun secMsGec(): String {
        var seconds = System.currentTimeMillis() / 1000L + 11_644_473_600L
        seconds -= seconds % 300L
        val ticks = seconds * 10_000_000L
        val data = "$ticks$TRUSTED_TOKEN".toByteArray(Charsets.US_ASCII)
        val digest = Drm.sha256Digest(data)
        val cs64 = Drm.cs64Digest(data, digest)
        val digestHex = digest.joinToString("") { "%02X".format(it) }
        return "$cs64-$digestHex"
    }

    private object Drm {

        private const val MASK = 0xFFFFFFFFL

        private val SHA256_INITIAL = longArrayOf(
            0x6A09E667L, 0xBB67AE85L, 0x3C6EF372L, 0xA54FF53AL,
            0x510E527FL, 0x9B05688CL, 0x1F83D9ABL, 0x5BE0CD19L,
        )
        private val SHA256_STATE = longArrayOf(
            0xFD9FBEA2L, 0x91243CE7L, 0xBE499094L, 0xCD2E1E0AL,
            0x6C006F49L, 0x04BD35ABL, 0xFEB0A7E4L, 0x03EFFBD2L,
        )
        private val SHA256_SCHEDULE = longArrayOf(
            0xEB24BF9EL, 0x1558691FL, 0xA0C09EE2L, 0x0CFA32BAL, 0xA622EE8CL,
            0xD1DD9491L, 0xC0DD25A0L, 0xF9E70AB6L, 0xF0C31573L, 0x7058CE4EL,
            0x7188E606L, 0x00E5AB33L, 0xD258B060L, 0x72766564L,
        )
        private val SHA256_ROUND_CONSTANTS = longArrayOf(
            0x428A2F98L, 0x71374491L, 0xB5C0FBCFL, 0xE9B5DBA5L, 0x3956C25BL,
            0x59F111F1L, 0x923F82A4L, 0xAB1C5ED5L, 0xD807AA98L, 0x12835B01L,
            0x243185BEL, 0x550C7DC3L, 0x72BE5D74L, 0x80DEB1FEL, 0x9BDC06A7L,
            0xC19BF174L, 0xE49B69C1L, 0xEFBE4786L, 0x0FC19DC6L, 0x240CA1CCL,
            0x2DE92C6FL, 0x4A7484AAL, 0x5CB0A9DCL, 0x76F988DAL, 0x983E5152L,
            0xA831C66DL, 0xB00327C8L, 0xBF597FC7L, 0xC6E00BF3L, 0xD5A79147L,
            0x06CA6351L, 0x14292967L, 0x27B70A85L, 0x2E1B2138L, 0x4D2C6DFCL,
            0x53380D13L, 0x650A7354L, 0x766A0ABBL, 0x81C2C92EL, 0x92722C85L,
            0xA2BFE8A1L, 0xA81A664BL, 0xC24B8B70L, 0xC76C51A3L, 0xD192E819L,
            0xD6990624L, 0xF40E3585L, 0x106AA070L, 0x19A4C116L, 0x1E376C08L,
            0x2748774CL, 0x34B0BCB5L, 0x391C0CB3L, 0x4ED8AA4AL, 0x5B9CCA4FL,
            0x682E6FF3L, 0x748F82EEL, 0x78A5636FL, 0x84C87814L, 0x8CC70208L,
            0x90BEFFFAL, 0xA4506CEBL, 0xBEF9A3F7L, 0xC67178F2L,
        )
        private const val SHA256_ADJUSTMENT = 0xC9DC06A7L
        private const val SHA256_SIGMA0_ADJUSTMENT = 0x059C0B80L
        private const val SHA256_SIGMA1_ADJUSTMENT = 0x000B92C0L

        private val SHA256_BLOCK_CONSTANTS = LongArray(64) { index ->
            if (index == 14) SHA256_ADJUSTMENT else SHA256_ROUND_CONSTANTS[index]
        }

        private val CS64_COEFFICIENTS = arrayOf(
            longArrayOf(0xE82E91B1L, 0xB32700D0L),
            longArrayOf(0x44DE24C6L, 0x88DFF51DL),
            longArrayOf(0x3BEB65BCL, 0x29514038L),
            longArrayOf(0xC89C5933L, 0xB328CBDFL),
            longArrayOf(0x8D1EC211L, 0xD405B4D5L),
            longArrayOf(0x314EF5D5L, 0x26E55F3CL),
            longArrayOf(0xE92491B1L, 0x607801C6L),
            longArrayOf(0xF0A5B9D5L, 0x82B2C1FBL),
            longArrayOf(0x06064CF3L, 0xFA51A3DAL),
            longArrayOf(0xDA79FA60L, 0x207CC464L),
            longArrayOf(0xBED20560L, 0x3C3C74F3L),
            longArrayOf(0x89F9C9F8L, 0xD2D14B61L),
            longArrayOf(0x35FA38C6L, 0xBB04A2C1L),
            longArrayOf(0xCB49E3C6L, 0xC5626167L),
        )
        private const val CS64_FACTOR = 0x82893933L
        private const val CS64_ADJUSTMENT0 = 0xF2F90B1AL
        private const val CS64_ADJUSTMENT1 = 0x0B1A0000L

        private fun u(value: Long): Long = value and MASK

        private fun rol16(value: Long): Long {
            val x = value and MASK
            return ((x shl 16) or (x ushr 16)) and MASK
        }

        private fun sigma0(value: Long): Long =
            (u(value) ushr 7) xor (u(value) shl 25) xor (u(value) ushr 18) xor
                (u(value) shl 14) xor (u(value) ushr 3) and MASK

        private fun sigma1(value: Long): Long =
            (u(value) ushr 17) xor (u(value) shl 15) xor (u(value) ushr 19) xor
                (u(value) shl 13) xor (u(value) ushr 10) and MASK

        private fun sha256Rounds(state: LongArray, words: LongArray, firstRound: Int): LongArray {
            var a = state[0]
            var b = state[1]
            var c = state[2]
            var d = state[3]
            var e = state[4]
            var f = state[5]
            var g = state[6]
            var h = state[7]
            val constants = if (firstRound == 14) SHA256_BLOCK_CONSTANTS else SHA256_ROUND_CONSTANTS
            for (index in firstRound until 64) {
                val rotE = e * 0x100000001L
                val rotA = a * 0x100000001L
                val t1 = h +
                    ((rotE ushr 6) xor (rotE ushr 11) xor (rotE ushr 25)) +
                    (g xor (e and (f xor g))) +
                    constants[index] + words[index]
                val t2 = ((rotA ushr 2) xor (rotA ushr 13) xor (rotA ushr 22)) +
                    ((a and b) or (c and (a or b)))
                h = g
                g = f
                f = e
                e = (d + t1) and MASK
                d = c
                c = b
                b = a
                a = (t1 + t2) and MASK
            }
            return longArrayOf(a, b, c, d, e, f, g, h)
        }

        private fun expandSchedule(words: LongArray, firstWord: Int) {
            for (index in firstWord until 64) {
                val x = words[index - 2]
                val y = words[index - 15]
                val s1 = (x ushr 17) xor (x shl 15) xor (x ushr 19) xor (x shl 13) xor (x ushr 10)
                val s0 = (y ushr 7) xor (y shl 25) xor (y ushr 18) xor (y shl 14) xor (y ushr 3)
                words[index] = (s1 + words[index - 7] + s0 + words[index - 16]) and MASK
            }
        }

        private fun readBigEndian(data: ByteArray, from: Int, count: Int): Long {
            var value = 0L
            for (i in 0 until count) {
                value = (value shl 8) or (data[from + i].toLong() and 0xFF)
            }
            return value
        }

        private fun sha256Init(data: ByteArray): LongArray {
            val words = LongArray(64)
            words[14] = readBigEndian(data, 0, 3)
            words[15] = readBigEndian(data, 3, 4)
            for (index in 16 until 30) {
                var value = SHA256_SCHEDULE[index - 16]
                if (index - 2 >= 14) {
                    val s1 = sigma1(words[index - 2])
                    value += if (index == 16) s1 xor SHA256_SIGMA1_ADJUSTMENT else s1
                }
                if (index - 7 >= 14) value += words[index - 7]
                if (index - 15 >= 14) {
                    val s0 = sigma0(words[index - 15])
                    value += if (index == 29) s0 xor SHA256_SIGMA0_ADJUSTMENT else s0
                }
                words[index] = value and MASK
            }
            words[30] = (
                sigma1(words[28]) + words[23] + sigma0(words[15]) + words[14] +
                    SHA256_ADJUSTMENT - SHA256_ROUND_CONSTANTS[14]
                ) and MASK
            expandSchedule(words, 31)
            val working = sha256Rounds(SHA256_STATE, words, 14)
            return LongArray(8) { (SHA256_INITIAL[it] + working[it]) and MASK }
        }

        fun sha256Digest(data: ByteArray): ByteArray {
            require(data.size >= 7) { "Token salt must contain at least seven bytes" }
            val length = 57 + data.size
            val padCount = ((55 - length) % 64 + 64) % 64
            val tail = data.copyOfRange(7, data.size)
            val padded = ByteArray(tail.size + 1 + padCount + 8)
            System.arraycopy(tail, 0, padded, 0, tail.size)
            padded[tail.size] = 0x80.toByte()
            val bitLength = length.toLong() * 8
            for (i in 0 until 8) {
                padded[padded.size - 1 - i] = ((bitLength ushr (8 * i)) and 0xFF).toByte()
            }

            var state = sha256Init(data.copyOfRange(0, 7))
            val words = LongArray(64)
            var offset = 0
            while (offset < padded.size) {
                for (i in 0 until 16) {
                    words[i] = readBigEndian(padded, offset + i * 4, 4)
                }
                expandSchedule(words, 16)
                val working = sha256Rounds(state, words, 0)
                state = LongArray(8) { (state[it] + working[it]) and MASK }
                offset += 64
            }

            val digest = ByteArray(32)
            for (i in 0 until 8) {
                val value = state[i]
                for (j in 0 until 4) {
                    digest[i * 4 + j] = ((value ushr (24 - 8 * j)) and 0xFF).toByte()
                }
            }
            return digest
        }

        private fun wordsLe4Even(data: ByteArray): LongArray {
            val padCount = (4 - data.size % 4) % 4
            val padded = ByteArray(data.size + padCount)
            System.arraycopy(data, 0, padded, 0, data.size)
            if (padded.isEmpty()) return LongArray(0)
            var count = padded.size / 4
            val odd = count % 2 != 0
            if (odd) count++
            return LongArray(count) { index ->
                if (index * 4 >= padded.size) {
                    0L
                } else {
                    (padded[index * 4].toLong() and 0xFF) or
                        ((padded[index * 4 + 1].toLong() and 0xFF) shl 8) or
                        ((padded[index * 4 + 2].toLong() and 0xFF) shl 16) or
                        ((padded[index * 4 + 3].toLong() and 0xFF) shl 24)
                }
            }
        }

        fun cs64Digest(data: ByteArray, digest: ByteArray): String {
            var keyA = (digest[0].toLong() and 0xFF) or
                ((digest[1].toLong() and 0xFF) shl 8) or
                ((digest[2].toLong() and 0xFF) shl 16) or
                ((digest[3].toLong() and 0xFF) shl 24)
            var keyB = (digest[4].toLong() and 0xFF) or
                ((digest[5].toLong() and 0xFF) shl 8) or
                ((digest[6].toLong() and 0xFF) shl 16) or
                ((digest[7].toLong() and 0xFF) shl 24)
            keyA = keyA or 1L
            keyB = keyB or 1L

            val factors = longArrayOf(
                (keyA * CS64_FACTOR) and MASK,
                (keyB * CS64_FACTOR) and MASK,
            )

            val terms = ArrayList<Long>(64)
            val offsets = ArrayList<Long>(64)
            CS64_COEFFICIENTS.forEachIndexed { index, pair ->
                terms.add((pair[0] * factors[index % 2]) and MASK)
                offsets.add(pair[1])
            }

            val payload = ByteArray(data.size + 1)
            System.arraycopy(data, 0, payload, 1, data.size)
            val dataWords = wordsLe4Even(payload)
            dataWords.forEachIndexed { index, word ->
                var term = word * if (index % 2 == 0) keyA else keyB
                var off = rol16(word) * if (index % 2 == 0) 0xEF0569FBL else 0xC31713DBL
                if (index == 0) {
                    term += CS64_ADJUSTMENT0 * factors[0]
                    off += CS64_ADJUSTMENT1
                }
                terms.add(term and MASK)
                offsets.add(off and MASK)
            }

            var swapAcc = 0L
            var swapLast = 0L
            var state = 0L
            var acc = 0L
            var index = 0
            while (index < terms.size) {
                val x = terms[index]
                val y = terms[index + 1]

                var t = rol16((offsets[index] + x) and MASK)
                var q = (t * 0x76F50F34L) and MASK
                swapAcc += (rol16(q) * 0xC3EFEA97L + q) and MASK

                t = rol16((offsets[index + 1] + y) and MASK)
                q = (t * 0x1881CE3CL) and MASK
                swapLast = (rol16(q) * 0x35BD1EC9L + q) and MASK
                swapAcc = (swapAcc + swapLast) and MASK

                state = (state * keyA + x) and MASK
                state = (rol16(state) * 0xCF98B111L) and MASK
                state = (rol16(state) * 0x87085B9FL) and MASK
                state = (rol16(state) * 0x12CEB96DL) and MASK
                state = (rol16(state) * 0x257E1D83L) and MASK
                acc += state

                t = (state * keyB + y) and MASK
                t = (rol16(t) * 0xA27416F5L) and MASK
                t = (rol16(t) * 0xD38396FFL) and MASK
                state = (rol16((rol16(t) * 0x7C932B89L) and MASK) * 0xBFA49F69L) and MASK
                acc = (acc + state) and MASK

                index += 2
            }

            val first = swapLast xor state
            val second = swapAcc xor acc
            val bytes = ByteArray(8)
            for (i in 0 until 4) {
                bytes[i] = ((first ushr (8 * i)) and 0xFF).toByte()
                bytes[4 + i] = ((second ushr (8 * i)) and 0xFF).toByte()
            }
            return bytes.joinToString("") { "%02X".format(it) }
        }
    }
}

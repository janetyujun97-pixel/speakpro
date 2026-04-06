package com.speakpro.core.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

// ============================================================
// 客户端 -> 服务端 消息
// ============================================================

/**
 * 客户端发送的 WebSocket 消息
 *
 * @param type 消息类型，如 session_init / audio_chunk / audio_complete / text / pong
 * @param data 消息体（根据 type 不同传入不同的 data class）
 */
data class WSClientMessage(
    val type: String,
    val data: Any? = null,
) {
    companion object {
        const val TYPE_SESSION_INIT = "session_init"
        const val TYPE_AUDIO_CHUNK = "audio_chunk"
        const val TYPE_AUDIO_COMPLETE = "audio_complete"
        const val TYPE_TEXT = "text"
        const val TYPE_PONG = "pong"
    }
}

/** session_init 消息体 */
data class SessionInitData(
    @SerializedName("session_id")  val sessionId: String,
    @SerializedName("exam_type")   val examType: String,   // IELTS, TOEFL
    val section: String,                                    // Part1, Part2, Part3, Independent, Integrated
    val mode: String,                                       // conversation, read_aloud, follow_read, mock_exam
)

/** audio_chunk 消息体 */
data class AudioChunkData(
    val sequence: Int,
    @SerializedName("audio_b64") val audioB64: String,
    @SerializedName("is_final")  val isFinal: Boolean,
)

/** audio_complete 消息体 */
data class AudioCompleteData(
    @SerializedName("session_id")     val sessionId: String,
    @SerializedName("reference_text") val referenceText: String? = null,
)

/** text 消息体 */
data class TextMessageData(
    val content: String,
)

// ============================================================
// 服务端 -> 客户端 消息
// ============================================================

/**
 * 服务端消息 — 密封类
 *
 * 每种类型对应 Go 服务的 ws_messages 中的一个消息结构。
 */
sealed class WSServerMessage {

    /** 会话已就绪，包含考官开场白和时间限制 */
    data class SessionReady(
        val sessionId: String,
        val examinerGreeting: String,
        val timeLimitSec: Int,
        val greetingTtsB64: String? = null,
    ) : WSServerMessage()

    /** 语音识别实时转写 */
    data class Transcript(
        val text: String,
        val isFinal: Boolean,
    ) : WSServerMessage()

    /** 考官回复文本（可选带 TTS 音频） */
    data class Examiner(
        val text: String,
        val ttsAudioB64: String? = null,
    ) : WSServerMessage()

    /** 评分更新 */
    data class ScoreUpdate(
        val pronunciation: PronunciationScore? = null,
        val grammar: GrammarScore? = null,
        val content: ContentScore? = null,
        val overall: Double,
        val aiFeedback: String? = null,
    ) : WSServerMessage()

    /** 处理中状态 */
    data class Processing(
        val step: String,
        val message: String,
    ) : WSServerMessage()

    /** 服务端错误 */
    data class Error(
        val code: String,
        val message: String,
    ) : WSServerMessage()

    /** 心跳 Ping */
    data object Ping : WSServerMessage()

    /** 无法识别的消息 */
    data class Unknown(val raw: String) : WSServerMessage()
}

// ---------- 评分子模型 ----------

data class PronunciationScore(
    val overall: Double? = null,
    val fluency: Double? = null,
    val stress: Double? = null,
    val intonation: Double? = null,
    val integrity: Double? = null,
)

data class GrammarScore(
    val score: Double? = null,
    val errors: List<GrammarError>? = null,
    val corrections: List<String>? = null,
)

data class GrammarError(
    val text: String? = null,
    val type: String? = null,
    val suggestion: String? = null,
)

data class ContentScore(
    val score: Double? = null,
    val relevance: Double? = null,
    val vocabulary: Double? = null,
    val coherence: Double? = null,
)

// ============================================================
// 消息解析器
// ============================================================

/**
 * 将服务端 JSON 字符串解析为 [WSServerMessage] 类型
 *
 * JSON 信封格式：{ "type": "xxx", "data": { ... } }
 */
object WsMessageParser {

    private val gson = Gson()

    fun parse(jsonString: String): WSServerMessage {
        return try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            val type = root.get("type")?.asString ?: return WSServerMessage.Unknown(jsonString)
            val data: JsonObject? = root.getAsJsonObject("data")

            when (type) {
                "session_ready" -> {
                    requireNotNull(data) { "session_ready 缺少 data 字段" }
                    WSServerMessage.SessionReady(
                        sessionId = data.getString("session_id"),
                        examinerGreeting = data.getString("examiner_greeting"),
                        timeLimitSec = data.getInt("time_limit_sec"),
                        greetingTtsB64 = data.getStringOrNull("greeting_tts_b64"),
                    )
                }

                "transcript" -> {
                    requireNotNull(data) { "transcript 缺少 data 字段" }
                    WSServerMessage.Transcript(
                        text = data.getString("text"),
                        isFinal = data.get("is_final")?.asBoolean ?: false,
                    )
                }

                "examiner" -> {
                    requireNotNull(data) { "examiner 缺少 data 字段" }
                    WSServerMessage.Examiner(
                        text = data.getString("text"),
                        ttsAudioB64 = data.getStringOrNull("tts_audio_b64"),
                    )
                }

                "score_update" -> {
                    requireNotNull(data) { "score_update 缺少 data 字段" }
                    WSServerMessage.ScoreUpdate(
                        pronunciation = data.getAsJsonObject("pronunciation")?.let { p ->
                            PronunciationScore(
                                overall = p.getDoubleOrNull("overall"),
                                fluency = p.getDoubleOrNull("fluency"),
                                stress = p.getDoubleOrNull("stress"),
                                intonation = p.getDoubleOrNull("intonation"),
                                integrity = p.getDoubleOrNull("integrity"),
                            )
                        },
                        grammar = data.getAsJsonObject("grammar")?.let { g ->
                            GrammarScore(
                                score = g.getDoubleOrNull("score"),
                                errors = g.getAsJsonArray("errors")?.map { elem ->
                                    val obj = elem.asJsonObject
                                    GrammarError(
                                        text = obj.getStringOrNull("text"),
                                        type = obj.getStringOrNull("type"),
                                        suggestion = obj.getStringOrNull("suggestion"),
                                    )
                                },
                                corrections = g.getAsJsonArray("corrections")?.map { it.asString },
                            )
                        },
                        content = data.getAsJsonObject("content")?.let { c ->
                            ContentScore(
                                score = c.getDoubleOrNull("score"),
                                relevance = c.getDoubleOrNull("relevance"),
                                vocabulary = c.getDoubleOrNull("vocabulary"),
                                coherence = c.getDoubleOrNull("coherence"),
                            )
                        },
                        overall = data.get("overall")?.asDouble ?: 0.0,
                        aiFeedback = data.getStringOrNull("ai_feedback"),
                    )
                }

                "processing" -> {
                    requireNotNull(data) { "processing 缺少 data 字段" }
                    WSServerMessage.Processing(
                        step = data.getString("step"),
                        message = data.getString("message"),
                    )
                }

                "error" -> {
                    requireNotNull(data) { "error 缺少 data 字段" }
                    WSServerMessage.Error(
                        code = data.getString("code"),
                        message = data.getString("message"),
                    )
                }

                "ping" -> WSServerMessage.Ping

                else -> WSServerMessage.Unknown(jsonString)
            }
        } catch (e: Exception) {
            WSServerMessage.Unknown(jsonString)
        }
    }

    // ---------- JsonObject 辅助扩展 ----------

    private fun JsonObject.getString(key: String): String =
        get(key)?.asString ?: ""

    private fun JsonObject.getStringOrNull(key: String): String? =
        if (has(key) && !get(key).isJsonNull) get(key).asString else null

    private fun JsonObject.getInt(key: String): Int =
        get(key)?.asInt ?: 0

    private fun JsonObject.getDoubleOrNull(key: String): Double? =
        if (has(key) && !get(key).isJsonNull) get(key).asDouble else null
}

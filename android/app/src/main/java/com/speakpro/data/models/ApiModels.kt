package com.speakpro.data.models

import com.google.gson.annotations.SerializedName

// =============================================================================
// SpeakPro 共享 API 数据模型
// 与 iOS 端和后端接口保持一致
//
// 命名约定：
//   - NestJS REST 端点 —— 全 camelCase，字段名直接匹配，不需要 @SerializedName
//   - Go REST 端点     —— 也全 camelCase（json tags 用 camelCase）
//   - Go WebSocket 消息 —— 使用 snake_case（历史遗留），数据类上保留 @SerializedName
//
// 曾经的 bug（PR5 修复）：REST 相关的 data class 之前带着 @SerializedName("snake_case")
// 但后端 JSON 的 key 是 camelCase，导致字段全为 null。现在移除这些错误标注。
// =============================================================================

// MARK: - 通用响应包装

/**
 * 后端统一响应格式
 *
 * ```json
 * { "code": 0, "message": "ok", "data": {...} }
 * ```
 */
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
)

// MARK: - 认证

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserInfo,
)

data class UserInfo(
    val id: String,
    val name: String,
    val email: String,
    val role: String,   // "student" | "teacher"
)

// MARK: - 练习统计

data class PracticeStats(
    val totalSessions: Int = 0,
    val averageScore: Double? = null,
    val totalDurationMin: Int? = null,
    val streakDays: Int? = null,
    val todaySessionCount: Int? = null,
    val dimensions: DimensionScores? = null,
    val recent: RecentStats = RecentStats(),
)

data class DimensionScores(
    val pronunciation: Double = 0.0,
    val fluency: Double = 0.0,
    val grammar: Double = 0.0,
    val content: Double = 0.0,
)

data class RecentStats(
    val last7Days: Int = 0,
    val last30Days: Int = 0,
)

// MARK: - 题库

data class QuestionItem(
    val id: String,
    val title: String,
    val content: String = "",
    val examType: String = "",          // "IELTS" | "TOEFL"
    val section: String? = null,        // "Part1" | "Part2" | "Part3" | "Independent" | "Integrated"
    val difficulty: String? = null,     // "easy" | "medium" | "hard"
    val tags: List<String>? = null,
    val createdAt: String? = null,
)

data class QuestionListResponse(
    val items: List<QuestionItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
)

// MARK: - 作业 (Homework)

data class HomeworkAssignment(
    val id: String,
    val title: String,
    val description: String? = null,
    val dueDate: String? = null,
    val createdAt: String? = null,
    val teacher: TeacherInfo? = null,
    val submissions: List<SubmissionInfo>? = null,
    val questionIds: List<String>? = null,
    val questions: List<QuestionItem>? = null,
) {
    /** 当前用户是否已完成（有 submitted 或 graded 状态的提交） */
    val isCompleted: Boolean
        get() = submissions?.any { it.status == "submitted" || it.status == "graded" } == true
}

data class TeacherInfo(
    val id: String,
    val name: String,
    val email: String? = null,
)

data class SubmissionInfo(
    val id: String? = null,
    val studentId: String? = null,
    val status: String = "pending",     // "pending" | "submitted" | "graded"
    val audioUrl: String? = null,
    val score: Double? = null,
    val feedback: String? = null,
    val submittedAt: String? = null,
    val gradedAt: String? = null,
)

// MARK: - AI 评测 (Assessment)

/**
 * 完整评测请求体（full-evaluate）
 */
data class FullEvaluateBody(
    val sessionId: String,
    val examType: String,
    val section: String,
    val questionText: String,
    val studentAudioB64: String? = null,
    val studentText: String? = null,
    val referenceText: String? = null,
)

/**
 * 完整评测结果（full-evaluate 响应）
 */
data class FullEvaluateResult(
    val sessionId: String? = null,       // Go 目前未回传；留字段以容纳后续补充
    val overallScore: Double = 0.0,
    val pronunciationScore: PronScore? = null,    // Go 侧字段名：pronunciationScore
    val grammarScore: GramScore? = null,
    val contentScore: ContentScore? = null,
    val fluencyScore: FluencyScore? = null,
    val aiFeedback: String? = null,
    val transcript: String? = null,
    val suggestions: List<String>? = null,
)

/** 发音评分 */
data class PronScore(
    val overall: Double? = null,
    val fluency: Double? = null,
    val stress: Double? = null,
    val intonation: Double? = null,
    val integrity: Double? = null,
    val words: List<WordScore>? = null,
)

/** 单词级发音评分 */
data class WordScore(
    val word: String,
    val score: Double = 0.0,
    val isCorrect: Boolean? = null,
)

/** 语法评分 */
data class GramScore(
    val score: Double? = null,
    val errors: List<GramError>? = null,
    val corrections: List<String>? = null,
)

/** 语法错误详情 */
data class GramError(
    val text: String? = null,
    val type: String? = null,
    val suggestion: String? = null,
)

/** 内容评分 */
data class ContentScore(
    val score: Double? = null,
    val relevance: Double? = null,
    val vocabulary: Double? = null,
    val coherence: Double? = null,
)

/** 流利度评分 */
data class FluencyScore(
    val score: Double? = null,
    val wordsPerMinute: Int? = null,
    val pauseCount: Int? = null,
    val fillerCount: Int? = null,
)

// MARK: - TTS 合成

data class TtsSynthesizeRequest(
    val text: String,
    val voice: String? = null,
    val speed: Float? = null,
)

data class TtsSynthesizeResponse(
    val audioB64: String,
    val duration: Double? = null,
)

// MARK: - WebSocket 消息模型

/** 客户端 → 服务端消息类型 */
enum class WSClientMessageType(val value: String) {
    SESSION_INIT("session_init"),
    AUDIO_CHUNK("audio_chunk"),
    AUDIO_COMPLETE("audio_complete"),
    TEXT("text"),
    PONG("pong"),
}

/** 会话初始化数据 */
data class SessionInitData(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("exam_type")
    val examType: String,       // "IELTS" | "TOEFL"
    val section: String,        // "Part1" | "Part2" | etc.
    val mode: String,           // "conversation" | "read_aloud" | "follow_read" | "mock_exam"
)

/** 音频分片数据 */
data class AudioChunkData(
    val sequence: Int,
    @SerializedName("audio_b64")
    val audioB64: String,
    @SerializedName("is_final")
    val isFinal: Boolean,
)

/** 音频完成信号 */
data class AudioCompleteData(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("reference_text")
    val referenceText: String? = null,
)

/** 文本消息数据 */
data class TextMessageData(
    val content: String,
)

/** 服务端 → 客户端消息类型 */
enum class WSServerMessageType(val value: String) {
    SESSION_READY("session_ready"),
    TRANSCRIPT("transcript"),
    EXAMINER("examiner"),
    SCORE_UPDATE("score_update"),
    ERROR("error"),
    PING("ping"),
    PROCESSING("processing"),
}

/** 会话就绪 */
data class SessionReadyPayload(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("examiner_greeting")
    val examinerGreeting: String,
    @SerializedName("time_limit_sec")
    val timeLimitSec: Int,
    @SerializedName("greeting_tts_b64")
    val greetingTtsB64: String? = null,
)

/** 语音识别文本 */
data class TranscriptPayload(
    val text: String,
    @SerializedName("is_final")
    val isFinal: Boolean,
)

/** 考官消息 */
data class ExaminerPayload(
    val text: String,
    @SerializedName("tts_audio_b64")
    val ttsAudioB64: String? = null,
)

/** 实时评分更新 */
data class ScoreUpdatePayload(
    val pronunciation: PronunciationScoreData? = null,
    val grammar: GrammarScoreData? = null,
    val content: ContentScoreData? = null,
    val overall: Double = 0.0,
    @SerializedName("ai_feedback")
    val aiFeedback: String? = null,
)

data class PronunciationScoreData(
    val overall: Double? = null,
    val fluency: Double? = null,
    val stress: Double? = null,
    val intonation: Double? = null,
    val integrity: Double? = null,
)

data class GrammarScoreData(
    val score: Double? = null,
    val errors: List<GrammarErrorData>? = null,
    val corrections: List<String>? = null,
)

data class GrammarErrorData(
    val text: String? = null,
    val type: String? = null,
    val suggestion: String? = null,
)

data class ContentScoreData(
    val score: Double? = null,
    val relevance: Double? = null,
    val vocabulary: Double? = null,
    val coherence: Double? = null,
)

/** 错误消息 */
data class ErrorPayload(
    val code: String = "",
    val message: String = "",
)

/** 处理进度消息 */
data class ProcessingPayload(
    val step: String = "",
    val message: String = "",
)

// MARK: - 练习枚举

/** 练习模式 */
enum class PracticeMode(val value: String) {
    CONVERSATION("conversation"),
    READ_ALOUD("read_aloud"),
    FOLLOW_READ("follow_read"),
    MOCK_EXAM("mock_exam"),
}

/** 考试类型 */
enum class ExamType(val value: String) {
    IELTS("IELTS"),
    TOEFL("TOEFL"),
}

/** 练习会话信息 */
data class PracticeSession(
    val id: String,
    val mode: String,
    val examType: String = "",
    val section: String? = null,
    val score: Double? = null,
    val durationSec: Int? = null,
    val createdAt: String? = null,
    val completedAt: String? = null,
)

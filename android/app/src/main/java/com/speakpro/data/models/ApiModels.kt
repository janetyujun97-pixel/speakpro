package com.speakpro.data.models

import com.google.gson.annotations.SerializedName

// =============================================================================
// SpeakPro 共享 API 数据模型
// 与 iOS 端和后端接口保持一致
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
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
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
    @SerializedName("total_sessions")
    val totalSessions: Int = 0,
    @SerializedName("average_score")
    val averageScore: Double? = null,
    @SerializedName("total_duration_min")
    val totalDurationMin: Int? = null,
    @SerializedName("streak_days")
    val streakDays: Int? = null,
    @SerializedName("today_session_count")
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
    @SerializedName("last_7_days")
    val last7Days: Int = 0,
    @SerializedName("last_30_days")
    val last30Days: Int = 0,
)

// MARK: - 题库

data class QuestionItem(
    val id: String,
    val title: String,
    val content: String = "",
    @SerializedName("exam_type")
    val examType: String = "",          // "IELTS" | "TOEFL"
    val section: String? = null,        // "Part1" | "Part2" | "Part3" | "Independent" | "Integrated"
    val difficulty: String? = null,     // "easy" | "medium" | "hard"
    val tags: List<String>? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
)

data class QuestionListResponse(
    val items: List<QuestionItem> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerializedName("page_size")
    val pageSize: Int = 20,
)

// MARK: - 作业 (Homework)

data class HomeworkAssignment(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    val teacher: TeacherInfo? = null,
    val submissions: List<SubmissionInfo>? = null,
    @SerializedName("question_ids")
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
    @SerializedName("student_id")
    val studentId: String? = null,
    val status: String = "pending",     // "pending" | "submitted" | "graded"
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    val score: Double? = null,
    val feedback: String? = null,
    @SerializedName("submitted_at")
    val submittedAt: String? = null,
    @SerializedName("graded_at")
    val gradedAt: String? = null,
)

// MARK: - AI 评测 (Assessment)

/**
 * 完整评测请求体（full-evaluate）
 */
data class FullEvaluateBody(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("exam_type")
    val examType: String,
    val section: String,
    @SerializedName("question_text")
    val questionText: String,
    @SerializedName("student_audio_b64")
    val studentAudioB64: String? = null,
    @SerializedName("student_text")
    val studentText: String? = null,
    @SerializedName("reference_text")
    val referenceText: String? = null,
)

/**
 * 完整评测结果（full-evaluate 响应）
 */
data class FullEvaluateResult(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("overall_score")
    val overallScore: Double = 0.0,
    val pronunciation: PronScore? = null,
    val grammar: GramScore? = null,
    val content: ContentScore? = null,
    val fluency: FluencyScore? = null,
    @SerializedName("ai_feedback")
    val aiFeedback: String? = null,
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
    @SerializedName("is_correct")
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
    @SerializedName("words_per_minute")
    val wordsPerMinute: Int? = null,
    @SerializedName("pause_count")
    val pauseCount: Int? = null,
    @SerializedName("filler_count")
    val fillerCount: Int? = null,
)

// MARK: - TTS 合成

data class TtsSynthesizeRequest(
    val text: String,
    val voice: String? = null,
    val speed: Float? = null,
)

data class TtsSynthesizeResponse(
    @SerializedName("audio_b64")
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
    @SerializedName("exam_type")
    val examType: String = "",
    val section: String? = null,
    val score: Double? = null,
    @SerializedName("duration_sec")
    val durationSec: Int? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("completed_at")
    val completedAt: String? = null,
)

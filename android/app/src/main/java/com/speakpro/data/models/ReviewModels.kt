package com.speakpro.data.models

// PR3c · Review 系统的请求 / 响应模型。字段全 camelCase，与 NestJS 输出对齐。

// ── 历史时间线 ──

data class SessionAudioResponse(
    val sessionId: String,
    val audioUrl: String? = null,
    /** 签名 URL 有效期；null 表示未签名，客户端直接用 audioUrl */
    val expiresInSec: Int? = null,
    val signed: Boolean = false,
)

data class PracticeSessionListItem(
    val id: String,
    val mode: String,
    val audioUrl: String? = null,
    val transcript: String? = null,
    val durationSec: Int? = null,
    val overallScore: Double? = null,
    val createdAt: String,
    val question: SessionQuestion? = null,
) {
    data class SessionQuestion(
        val id: String,
        val promptText: String? = null,
        val examType: String? = null,
        val section: String? = null,
    )
}

// ── Notebook ──

enum class NotebookFilter(val value: String) {
    DUE("due"), MASTERED("mastered"), ALL("all");
}

data class NotebookWord(
    val id: String,
    val userId: String,
    val word: String,
    val ipa: String? = null,
    val note: String? = null,
    val sourceSessionId: String? = null,
    val missCount: Int = 1,
    val lastSeenAt: String? = null,
    val masteredAt: String? = null,
    val nextReviewAt: String? = null,
    val ef: Double = 2.5,
    val intervalDays: Int = 0,
    val createdAt: String? = null,
)

data class NotebookPhrase(
    val id: String,
    val userId: String,
    val phrase: String,
    val note: String? = null,
    val useCount: Int = 1,
    val lastSeenAt: String? = null,
    val createdAt: String? = null,
)

data class ReviewWordRequest(val quality: Int)

data class OkResponse(val ok: Boolean = false)

// ── Notifications ──

enum class NotificationKind(val value: String) {
    HOMEWORK("homework"),
    FEEDBACK("feedback"),
    STREAK("streak"),
    REMINDER("reminder"),
    SYSTEM("system"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(v: String?): NotificationKind =
            entries.firstOrNull { it.value == v } ?: UNKNOWN
    }
}

data class NotificationItem(
    val id: String,
    val userId: String,
    /** 后端返字符串；UI 层用 [NotificationKind.fromValue] 转 enum */
    val kind: String,
    val title: String,
    val body: String,
    val payload: Map<String, Any?>? = null,
    val isRead: Boolean = false,
    val createdAt: String,
)

data class NotificationListResponse(
    val items: List<NotificationItem> = emptyList(),
    val unread: Int = 0,
)

data class NotificationPrefs(
    val userId: String,
    val quietStart: String = "22:30:00",
    val quietEnd: String = "07:30:00",
    val pushEnabled: Boolean = true,
    val updatedAt: String? = null,
)

data class UpdatePrefsRequest(
    val quietStart: String? = null,
    val quietEnd: String? = null,
    val pushEnabled: Boolean? = null,
)

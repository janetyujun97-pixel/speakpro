package com.speakpro.data.models

// Onboarding 8 步的模型。
// 跟 NestJS /onboarding/* 端点严格对齐，字段名 camelCase 即可。

enum class OnbExamType(val value: String) {
    IELTS("IELTS"),
    TOEFL("TOEFL"),
    GENERAL("GENERAL");

    companion object {
        fun fromValue(v: String?): OnbExamType? = entries.firstOrNull { it.value == v }
    }
}

data class OnboardingProfile(
    val userId: String,
    val examType: String? = null,         // "IELTS" / "TOEFL" / "GENERAL"
    val targetScore: Double? = null,
    val examDate: String? = null,         // ISO 日期 "2026-05-22"
    val selfLevel: Int? = null,           // 1..5
    val baselineSessionId: String? = null,
    val studyPlan: StudyPlan? = null,
    val completedAt: String? = null,
    val updatedAt: String? = null,
)

data class StudyPlan(
    val weeks: Int = 0,
    val dailyMinutes: Int = 0,
    val focusAreas: List<String> = emptyList(),
    val milestones: List<Milestone> = emptyList(),
) {
    data class Milestone(val week: Int, val goal: String)
}

data class OnboardingStatusResponse(
    val completed: Boolean = false,
    val profile: OnboardingProfile? = null,
)

data class UpdateProfileRequest(
    val examType: String? = null,
    val targetScore: Double? = null,
    val examDate: String? = null,
    val selfLevel: Int? = null,
)

data class BaselineRequest(
    val sessionId: String? = null,
    val audioUrl: String? = null,
    val transcript: String? = null,
)

data class BaselineResponse(val sessionId: String)

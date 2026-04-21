package com.speakpro.core.errors

import retrofit2.HttpException
import java.io.IOException

/**
 * §4.3 客户端错误码。覆盖 UI 文案 + 大号数字 glyph + 从 [Throwable] 映射。
 */
enum class SpErrorCode(
    val code: String,
    val displayNumber: String,
    val eyebrow: String,
    val headline: String,
    val headlineItalic: String,
    val body: String,
) {
    SCORE_ENGINE_503(
        "ERR-SCORE-503", "503",
        "SCORE ENGINE UNREACHABLE",
        "Our scoring engine",
        "is catching its breath.",
        "你的录音已经安全保存在本地 —— 等服务恢复会自动上传评分。不会丢。",
    ),
    TTS_TIMEOUT_504(
        "ERR-TTS-504", "504",
        "VOICE SYNTH SLOW",
        "The voice synthesis",
        "is catching up.",
        "示范发音生成较慢，已自动切换到备用语音。你可以继续练习。",
    ),
    OFFLINE(
        "ERR-NET", "—",
        "NO CONNECTION · 无网络",
        "We're offline,",
        "some rehearsal.",
        "检查下 WiFi / 蜂窝数据。已缓存的材料仍可继续练。",
    ),
    UNAUTHORIZED_401(
        "ERR-AUTH-401", "401",
        "SESSION EXPIRED · 会话过期",
        "Please log in again,",
        "for continuity.",
        "为保护账号安全，请重新登录一次。",
    ),
    UNKNOWN(
        "ERR-UNKNOWN", "?",
        "SOMETHING WRONG · 出错了",
        "Something odd,",
        "please retry.",
        "发生了预期之外的问题。请重试，或反馈给我们。",
    );

    companion object {
        /** 把 Retrofit / 网络层异常映射为 SpErrorCode */
        fun from(error: Throwable): SpErrorCode = when (error) {
            is HttpException -> when (error.code()) {
                401 -> UNAUTHORIZED_401
                503 -> SCORE_ENGINE_503
                504 -> TTS_TIMEOUT_504
                else -> UNKNOWN
            }
            is IOException -> OFFLINE
            else -> UNKNOWN
        }
    }
}

package com.speakpro.core.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// MARK: - 日期扩展函数

/**
 * 相对时间描述
 *
 * 示例: "刚刚", "5分钟前", "2小时前", "3天前"
 * 超过 7 天则返回格式化日期 "yyyy-MM-dd"
 */
fun Date.relativeTimeString(): String {
    val now = System.currentTimeMillis()
    val intervalMs = now - this.time

    if (intervalMs < 0) return "刚刚"

    val seconds = intervalMs / 1000
    return when {
        seconds < 60      -> "刚刚"
        seconds < 3600    -> "${seconds / 60}分钟前"
        seconds < 86400   -> "${seconds / 3600}小时前"
        seconds < 604800  -> "${seconds / 86400}天前"
        else              -> this.formattedDate()
    }
}

/**
 * 格式化日期
 *
 * 输出: "2026-04-05"
 */
fun Date.formattedDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    return sdf.format(this)
}

/**
 * 格式化日期时间
 *
 * 输出: "2026-04-05 14:30"
 */
fun Date.formattedDateTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    return sdf.format(this)
}

/**
 * 友好的截止日期描述
 *
 * 示例: "还剩2天", "还剩3小时", "已截止"
 */
fun Date.deadlineString(): String {
    val now = System.currentTimeMillis()
    val intervalMs = this.time - now

    if (intervalMs < 0) return "已截止"

    val seconds = intervalMs / 1000
    return when {
        seconds < 3600  -> "还剩${seconds / 60}分钟"
        seconds < 86400 -> "还剩${seconds / 3600}小时"
        else            -> "还剩${seconds / 86400}天"
    }
}

// MARK: - ISO 8601 日期解析

/**
 * 解析 ISO 8601 日期字符串
 *
 * 支持以下格式：
 * - "2026-04-05T14:30:00.000Z"  (带毫秒)
 * - "2026-04-05T14:30:00Z"      (不带毫秒)
 * - "2026-04-05T14:30:00+08:00" (带时区偏移)
 *
 * @return 解析后的 [Date]，解析失败返回 null
 */
fun String.parseISO8601(): Date? {
    // 尝试带毫秒的格式
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
    )

    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            if (pattern.endsWith("'Z'")) {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(this)
            if (date != null) return date
        } catch (_: Exception) {
            // 当前格式不匹配，尝试下一个
        }
    }

    // 最后兜底：截取前 19 个字符 + Z
    return try {
        val cleaned = this.take(19) + "Z"
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(cleaned)
    } catch (_: Exception) {
        null
    }
}

/**
 * 将毫秒时间戳转换为 Date
 */
fun Long.toDate(): Date = Date(this)

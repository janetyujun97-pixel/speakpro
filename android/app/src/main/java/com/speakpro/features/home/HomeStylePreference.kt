package com.speakpro.features.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * 首页风格偏好（dev 彩蛋：连点 masthead 的 No.XXX 3 下打开切换 sheet）。
 *
 * 对应设计 speakpro/components/{HomeEditorial,HomeVariants}.jsx：
 *   - Editorial 风格 × 3 种模考卡片（Full / Ticket / Diagram）
 *   - Minimal / Dashboard 目前占位（UI 未实现，敬请期待）
 */
enum class HomeStyle(val label: String, val subtitle: String, val available: Boolean) {
    EDITORIAL_FULL("编辑风 · 完整卡", "深底 hero + 3 项统计", true),
    EDITORIAL_TICKET("编辑风 · 票根卡", "左侧 sienna 票根 + 虚线分割", true),
    EDITORIAL_DIAGRAM("编辑风 · 示意图", "P1/P2/P3 schematic", true),
    MINIMAL("极简风", "大量留白 · 单列信息", false),
    DASHBOARD("仪表盘风", "圆环评分 + 多图表", false);

    companion object {
        val default = EDITORIAL_FULL
        fun fromName(name: String?): HomeStyle = entries.firstOrNull { it.name == name } ?: default
    }
}

object HomeStylePreference {

    private const val PREF = "speakpro_home_style"
    private const val KEY = "home_style"

    private lateinit var prefs: SharedPreferences

    /** 可在 Composable 里 collectAsState 的响应式状态 */
    val current = mutableStateOf(HomeStyle.default)

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        current.value = HomeStyle.fromName(prefs.getString(KEY, null))
    }

    fun set(style: HomeStyle) {
        current.value = style
        prefs.edit().putString(KEY, style.name).apply()
    }
}

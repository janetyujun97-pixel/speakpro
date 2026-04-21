package com.speakpro.designsystem.theme

import androidx.compose.ui.graphics.Color

// SpeakPro 设计系统颜色
// Editorial 风格：墨黑 + sienna + 暖米。source of truth = speakpro/components/HomeEditorial.jsx 的 PALETTE。
// 业务代码仍用既有名字（SpPrimary / SpAccent / ...），仅替换值；新增 token 用新名字。

// ——— 既有 token（改值） ———

/** Ink — 墨黑主色 */
val SpPrimary = Color(0xFF1C1B18)

/** Sienna — 强调色 */
val SpAccent = Color(0xFFB54A25)

/** Warm Parchment — 背景色 */
val SpBackground = Color(0xFFF4EEE3)

/** Moss — 成功色（克制的深苔绿，替代薄荷） */
val SpSuccess = Color(0xFF2F4A3A)

/** Amber — 警告色（保留） */
val SpWarning = Color(0xFFF59E0B)

/** Red — 错误色（保留） */
val SpError = Color(0xFFEF4444)

/** Text Primary — 主文本色（= ink） */
val SpTextPrimary = Color(0xFF1C1B18)

/** Text Secondary — 次要文本色（muted） */
val SpTextSecondary = Color(0xFF706A5E)

/** Ivory — 卡片/容器表面色 */
val SpSurface = Color(0xFFFBF8F2)

/** White — 纯白 */
val SpWhite = Color(0xFFFFFFFF)

/** Black — 纯黑 */
val SpBlack = Color(0xFF000000)

// ——— 新增 token ———

/** Ivory — 卡片面（与 SpSurface 同值，语义双份） */
val SpIvory = Color(0xFFFBF8F2)

/** Muted — 次级文字 */
val SpMuted = Color(0xFF706A5E)

/** Moss — 成功/强调的克制态 */
val SpMoss = Color(0xFF2F4A3A)

/** Line — 分隔线 rgba(28,27,24,0.12)；alpha 0.12 ≈ 0x1F */
val SpLine = Color(0x1F1C1B18)

/** Accent Warm — Splash / 暗底珊瑚 */
val SpAccentWarm = Color(0xFFD9734A)

/** BgSoft — 第二层背景 */
val SpBgSoft = Color(0xFFEDE5D6)

/** AccentSoft — 低饱和 accent 底 rgba(181,74,37,0.08)；alpha 0.08 ≈ 0x14 */
val SpAccentSoft = Color(0x14B54A25)

/** MossSoft — 低饱和 moss 底 rgba(47,74,58,0.08)；alpha 0.08 ≈ 0x14 */
val SpMossSoft = Color(0x142F4A3A)

import SwiftUI

// MARK: - SpeakPro 设计系统颜色
// Editorial 风格：墨黑 + sienna + 暖米。source of truth = speakpro/components/HomeEditorial.jsx 的 PALETTE。
// 业务代码仍用既有名字（spPrimary/spAccent/...），仅替换值；新增 token 用新名字。

extension Color {

    // MARK: 既有 token（改值）

    /// Ink — 墨黑主色 #1C1B18
    static let spPrimary = Color(red: 0x1C / 255.0, green: 0x1B / 255.0, blue: 0x18 / 255.0)

    /// Sienna — 强调色 #B54A25
    static let spAccent = Color(red: 0xB5 / 255.0, green: 0x4A / 255.0, blue: 0x25 / 255.0)

    /// Warm Parchment — 背景色 #F4EEE3
    static let spBackground = Color(red: 0xF4 / 255.0, green: 0xEE / 255.0, blue: 0xE3 / 255.0)

    /// Moss — 成功色 #2F4A3A（克制的深苔绿，替代薄荷）
    static let spSuccess = Color(red: 0x2F / 255.0, green: 0x4A / 255.0, blue: 0x3A / 255.0)

    /// Amber — 警告色 #F59E0B（保留）
    static let spWarning = Color(red: 0xF5 / 255.0, green: 0x9E / 255.0, blue: 0x0B / 255.0)

    /// Red — 错误色 #EF4444（保留）
    static let spError = Color(red: 0xEF / 255.0, green: 0x44 / 255.0, blue: 0x44 / 255.0)

    /// 主文本色（= ink）
    static let spTextPrimary = Color(red: 0x1C / 255.0, green: 0x1B / 255.0, blue: 0x18 / 255.0)

    /// 次要文本色 #706A5E（muted）
    static let spTextSecondary = Color(red: 0x70 / 255.0, green: 0x6A / 255.0, blue: 0x5E / 255.0)

    /// Ivory — 卡片/容器表面色 #FBF8F2
    static let spSurface = Color(red: 0xFB / 255.0, green: 0xF8 / 255.0, blue: 0xF2 / 255.0)

    // MARK: 新增 token

    /// Ivory — 卡片面（与 spSurface 同值，语义双份）
    static let spIvory = Color(red: 0xFB / 255.0, green: 0xF8 / 255.0, blue: 0xF2 / 255.0)

    /// Muted — 次级文字 #706A5E
    static let spMuted = Color(red: 0x70 / 255.0, green: 0x6A / 255.0, blue: 0x5E / 255.0)

    /// Moss — 成功/强调的克制态 #2F4A3A
    static let spMoss = Color(red: 0x2F / 255.0, green: 0x4A / 255.0, blue: 0x3A / 255.0)

    /// Line — 分隔线 rgba(28,27,24,0.12)
    static let spLine = Color(red: 0x1C / 255.0, green: 0x1B / 255.0, blue: 0x18 / 255.0).opacity(0.12)

    /// Accent Warm — Splash / 暗底珊瑚 #D9734A
    static let spAccentWarm = Color(red: 0xD9 / 255.0, green: 0x73 / 255.0, blue: 0x4A / 255.0)

    /// BgSoft — 第二层背景 #EDE5D6
    static let spBgSoft = Color(red: 0xED / 255.0, green: 0xE5 / 255.0, blue: 0xD6 / 255.0)

    /// AccentSoft — 低饱和 accent 底 rgba(181,74,37,0.08)
    static let spAccentSoft = Color(red: 0xB5 / 255.0, green: 0x4A / 255.0, blue: 0x25 / 255.0).opacity(0.08)

    /// MossSoft — 低饱和 moss 底 rgba(47,74,58,0.08)
    static let spMossSoft = Color(red: 0x2F / 255.0, green: 0x4A / 255.0, blue: 0x3A / 255.0).opacity(0.08)
}

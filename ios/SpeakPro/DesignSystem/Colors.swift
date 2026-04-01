import SwiftUI

// MARK: - SpeakPro 设计系统颜色

extension Color {

    /// Deep Indigo — 主色 #1B2A4A
    static let spPrimary = Color(red: 0x1B / 255.0, green: 0x2A / 255.0, blue: 0x4A / 255.0)

    /// Coral Orange — 强调色 #FF6B4A
    static let spAccent = Color(red: 0xFF / 255.0, green: 0x6B / 255.0, blue: 0x4A / 255.0)

    /// Warm Gray White — 背景色 #F8F7F4
    static let spBackground = Color(red: 0xF8 / 255.0, green: 0xF7 / 255.0, blue: 0xF4 / 255.0)

    /// Mint Green — 成功色 #2DD4A8
    static let spSuccess = Color(red: 0x2D / 255.0, green: 0xD4 / 255.0, blue: 0xA8 / 255.0)

    /// Warning — 警告色 #F59E0B
    static let spWarning = Color(red: 0xF5 / 255.0, green: 0x9E / 255.0, blue: 0x0B / 255.0)

    /// Error — 错误色 #EF4444
    static let spError = Color(red: 0xEF / 255.0, green: 0x44 / 255.0, blue: 0x44 / 255.0)

    /// Text Primary — 主文本色 (Deep Indigo)
    static let spTextPrimary = Color(red: 0x1B / 255.0, green: 0x2A / 255.0, blue: 0x4A / 255.0)

    /// Text Secondary — 次要文本色
    static let spTextSecondary = Color(red: 0x6B / 255.0, green: 0x72 / 255.0, blue: 0x80 / 255.0)

    /// Surface — 卡片/容器表面色 #F3F4F6
    static let spSurface = Color(red: 0xF3 / 255.0, green: 0xF4 / 255.0, blue: 0xF6 / 255.0)
}

import SwiftUI

// MARK: - SpeakPro 字体系统

/// 基于 SF Pro Display (系统字体) 的字体层级定义
/// 备注：设计文档中指定了 Nunito 作为辅助字体，可在需要时通过 .custom("Nunito", ...) 使用
extension Font {

    /// Title Large — 28pt Bold
    static let spTitleLarge = Font.system(size: 28, weight: .bold, design: .default)

    /// Title Medium — 22pt Semibold
    static let spTitleMedium = Font.system(size: 22, weight: .semibold, design: .default)

    /// Title Small — 18pt Semibold
    static let spTitleSmall = Font.system(size: 18, weight: .semibold, design: .default)

    /// Body Large — 17pt Regular
    static let spBodyLarge = Font.system(size: 17, weight: .regular, design: .default)

    /// Body Medium — 15pt Regular
    static let spBodyMedium = Font.system(size: 15, weight: .regular, design: .default)

    /// Body Small — 13pt Regular
    static let spBodySmall = Font.system(size: 13, weight: .regular, design: .default)

    /// Caption — 11pt Regular
    static let spCaption = Font.system(size: 11, weight: .regular, design: .default)
}

// MARK: - Nunito 辅助字体（需在项目中导入字体文件）

extension Font {

    static func nunito(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        // TODO: 添加 Nunito 字体文件到项目中后，替换为 .custom("Nunito", size: size)
        return .system(size: size, weight: weight, design: .rounded)
    }
}

import SwiftUI

// MARK: - SpeakPro 字体系统
// Editorial 风格：Fraunces（serif）承担标题与数字；Inter（sans）承担正文。
// 字体文件位于 Resources/Fonts/，通过 Info.plist 的 UIAppFonts 注册。
// Variable font PostScript 名：`Fraunces` / `Fraunces-Italic` / `Inter` / `Inter-Italic`。
// 业务代码仍用既有名字（spTitleLarge 等），仅替换底层字体族。

extension Font {

    // MARK: 既有 token — 标题层（Fraunces）

    /// Title Large — Fraunces 28pt
    static let spTitleLarge = Font.custom("Fraunces", size: 28)

    /// Title Medium — Fraunces 22pt
    static let spTitleMedium = Font.custom("Fraunces", size: 22)

    /// Title Small — Fraunces 18pt
    static let spTitleSmall = Font.custom("Fraunces", size: 18)

    // MARK: 既有 token — 正文层（Inter）

    /// Body Large — Inter 17pt
    static let spBodyLarge = Font.custom("Inter", size: 17)

    /// Body Medium — Inter 15pt
    static let spBodyMedium = Font.custom("Inter", size: 15)

    /// Body Small — Inter 13pt
    static let spBodySmall = Font.custom("Inter", size: 13)

    /// Caption — Inter 11pt
    static let spCaption = Font.custom("Inter", size: 11)

    // MARK: 新增 token

    /// Eyebrow — Inter 10pt / 600，使用时配合 `.tracking(2.2)` 与 `.textCase(.uppercase)`
    static let spEyebrow = Font.custom("Inter", size: 10).weight(.semibold)

    /// Numeric 数字 — Fraunces，opsz 144 的大数字用场景。调用方按 22–72 传 size
    static func spNumeric(_ size: CGFloat) -> Font {
        Font.custom("Fraunces", size: size)
    }

    /// Fraunces 通用构造（含 italic 变体）。italic 由 `Fraunces-Italic` 族提供
    static func spSerif(_ size: CGFloat, italic: Bool = false) -> Font {
        Font.custom(italic ? "Fraunces-Italic" : "Fraunces", size: size)
    }

    /// Inter 通用构造
    static func spSans(_ size: CGFloat, italic: Bool = false) -> Font {
        Font.custom(italic ? "Inter-Italic" : "Inter", size: size)
    }

    // MARK: 兼容保留 — Nunito 占位（既有 API 不删，防业务代码引用）

    static func nunito(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        // Nunito 已被 Inter 取代。保留该 API 以兼容既有调用。
        Font.custom("Inter", size: size).weight(weight)
    }
}

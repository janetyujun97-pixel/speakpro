import SwiftUI

/// Empty 态 —— 编辑式排版，大号斜体 `0` + 45° 斜划线。
/// 不要插画。eyebrow 用 accent 色，标题两行（正体 + italic 副句）。
struct EmptyStateView: View {
    let eyebrow: String            // "NO HOMEWORK · 清单空空"
    let headline: String           // "All caught up,"
    let headlineItalic: String     // "— for now."
    let message: String            // "老师暂时没有布置新的作业…"
    var primaryCTA: CTAConfig? = nil
    var secondaryCTA: CTAConfig? = nil
    var footer: String = "EMPTY STATE"
    var footerNumber: String = "N° 001"

    struct CTAConfig {
        let title: String
        let action: () -> Void
    }

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            zeroGlyph
                .padding(.bottom, 28)

            Text(eyebrow)
                .font(.spEyebrow)
                .foregroundColor(.spAccent)

            VStack(spacing: -2) {
                Text(headline)
                    .font(.spSerif(28))
                    .foregroundColor(.spPrimary)
                Text(headlineItalic)
                    .font(.spSerif(28, italic: true))
                    .foregroundColor(.spPrimary)
            }
            .padding(.top, 14)

            Text(message)
                .font(.spBodyMedium)
                .foregroundColor(.spMuted)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .frame(maxWidth: 240)
                .padding(.top, 16)

            if let cta = primaryCTA {
                Button(action: cta.action) {
                    HStack(spacing: 8) {
                        Text(cta.title)
                            .font(.system(size: 13, weight: .semibold))
                        Image(systemName: "arrow.right")
                            .font(.system(size: 12))
                    }
                    .foregroundColor(.spIvory)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 14)
                    .background(Color.spPrimary)
                    .clipShape(Capsule())
                }
                .padding(.top, 30)
            }
            if let cta = secondaryCTA {
                Button(action: cta.action) {
                    Text(cta.title)
                        .font(.system(size: 11))
                        .foregroundColor(.spAccent)
                }
                .padding(.top, 14)
            }

            Spacer()

            decorativeFooter
        }
        .padding(.horizontal, 36)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.spBackground)
    }

    // MARK: - Zero glyph

    private var zeroGlyph: some View {
        ZStack {
            Text("0")
                .font(.spSerif(140, italic: true))
                .foregroundColor(Color.spLine)
                .frame(width: 120, height: 140)

            Rectangle()
                .fill(Color.spAccent)
                .frame(width: 60, height: 1)
                .rotationEffect(.degrees(-15))
        }
    }

    // MARK: - Footer rule

    private var decorativeFooter: some View {
        VStack(spacing: 0) {
            HStack {
                Text(footer)
                    .font(.system(size: 9, weight: .semibold))
                    .tracking(2)
                Spacer()
                Text(footerNumber)
                    .font(.system(size: 9, weight: .semibold))
                    .tracking(2)
            }
            .foregroundColor(.spMuted)
            .padding(.bottom, 8)
            .padding(.horizontal, -8)

            Rectangle().fill(Color.spLine).frame(height: 1)
        }
        .padding(.bottom, 24)
    }
}

#Preview {
    EmptyStateView(
        eyebrow: "NO HOMEWORK · 清单空空",
        headline: "All caught up,",
        headlineItalic: "— for now.",
        message: "老师暂时没有布置新的作业。\n不如趁着这个空隙，自己练 15 分钟？",
        primaryCTA: .init(title: "去自由练习", action: {}),
        secondaryCTA: .init(title: "或 加入一个公开班级 接收作业", action: {}),
    )
}

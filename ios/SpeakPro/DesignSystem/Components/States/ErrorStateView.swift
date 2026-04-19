import SwiftUI

/// Error 态 —— 大号数字错误码 + 红斜划线 + "录音已本地保存"承诺。
/// 使用方：根据 `SpErrorCode` 选对应文案；可选传 `localBackupSize` 展示 chip。
struct ErrorStateView: View {
    let code: SpErrorCode
    var localBackupSize: String? = nil    // e.g. "2.4 MB"
    var onRetry: (() -> Void)? = nil
    var onFeedback: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            numberGlyph

            Text(code.eyebrow)
                .font(.spEyebrow)
                .foregroundColor(.spAccent)
                .padding(.top, 10)

            VStack(spacing: -2) {
                Text(code.headline)
                    .font(.spSerif(26))
                    .foregroundColor(.spPrimary)
                Text(code.headlineItalic)
                    .font(.spSerif(26, italic: true))
                    .foregroundColor(.spAccent)
            }
            .padding(.top, 20)

            Text(code.body)
                .font(.spBodyMedium)
                .foregroundColor(.spMuted)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .frame(maxWidth: 240)
                .padding(.top, 16)

            if let size = localBackupSize {
                backupChip(size: size)
                    .padding(.top, 18)
            }

            if let retry = onRetry {
                Button(action: retry) {
                    HStack(spacing: 8) {
                        Text("再试一次")
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
                .padding(.top, 28)
            }

            HStack(spacing: 0) {
                if let fb = onFeedback {
                    Button("反馈问题") { fb() }
                        .foregroundColor(.spAccent)
                        .font(.system(size: 11, weight: .semibold))
                    Text(" · ").foregroundColor(.spLine)
                }
                Text("错误码 \(code.rawValue)")
                    .foregroundColor(.spMuted)
                    .font(.system(size: 11))
            }
            .padding(.top, 12)

            Spacer()

            decorativeFooter
        }
        .padding(.horizontal, 32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.spBackground)
    }

    // MARK: - Number glyph with red slash

    private var numberGlyph: some View {
        ZStack {
            Text(code.displayNumber)
                .font(.spSerif(110, italic: true))
                .foregroundColor(.spPrimary)

            Rectangle()
                .fill(Color.spAccent)
                .frame(height: 2)
                .padding(.horizontal, -6)
                .rotationEffect(.degrees(-8))
                .padding(.vertical, 60)
        }
    }

    // MARK: - Saved locally chip

    private func backupChip(size: String) -> some View {
        HStack(spacing: 8) {
            Circle().fill(Color.spMoss).frame(width: 6, height: 6)
            Text("录音已本地保存 · \(size)")
                .font(.system(size: 10, weight: .semibold))
                .tracking(1)
                .foregroundColor(.spMoss)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.spIvory)
        .overlay(Capsule().stroke(Color.spLine, lineWidth: 1))
        .clipShape(Capsule())
    }

    // MARK: - Footer rule

    private var decorativeFooter: some View {
        VStack(spacing: 0) {
            HStack {
                Text("ERROR STATE")
                    .font(.system(size: 9, weight: .semibold))
                    .tracking(2)
                Spacer()
                Text("N° \(code.displayNumber)")
                    .font(.system(size: 9, weight: .semibold))
                    .tracking(2)
            }
            .foregroundColor(.spMuted)
            .padding(.bottom, 8)

            Rectangle().fill(Color.spLine).frame(height: 1)
        }
        .padding(.bottom, 24)
    }
}

#Preview {
    ErrorStateView(
        code: .scoreEngine503,
        localBackupSize: "2.4 MB",
        onRetry: {},
        onFeedback: {},
    )
}

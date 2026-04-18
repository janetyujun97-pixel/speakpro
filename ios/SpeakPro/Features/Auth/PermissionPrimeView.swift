import AVFoundation
import SwiftUI

/// 麦克风权限前置 —— 登录后首次进入 onboarding 的基线录音前展示。
/// 点 "同意并继续" 才触发 AVAudioApplication.requestRecordPermission。
struct PermissionPrimeView: View {

    /// 完成（granted / declined）或跳过
    var onComplete: (_ granted: Bool) -> Void

    @State private var isRequesting = false

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                // masthead
                HStack {
                    Text("PERMISSION · 1 / 2").font(.spEyebrow).foregroundColor(.spMuted)
                    Spacer()
                    Button("跳过") { onComplete(false) }
                        .font(.system(size: 11))
                        .foregroundColor(.spMuted)
                }
                .padding(.horizontal, 28)
                .padding(.top, 16)

                // illustrative mark
                ZStack {
                    Circle().stroke(Color.spAccentWarm.opacity(0.15), lineWidth: 1)
                        .frame(width: 174, height: 174)
                    Circle().stroke(Color.spAccentWarm.opacity(0.3), lineWidth: 1)
                        .frame(width: 150, height: 150)
                    Circle().fill(Color.spPrimary).frame(width: 130, height: 130)
                    Image(systemName: "mic.fill")
                        .font(.system(size: 38))
                        .foregroundColor(.spAccentWarm)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 28)

                VStack(alignment: .center, spacing: 0) {
                    Text("We need to")
                        .font(.spSerif(28)).foregroundColor(.spPrimary)
                    Text("hear you.")
                        .font(.spSerif(28, italic: true))
                        .foregroundColor(.spAccent)
                        .padding(.top, -4)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 36)

                (
                    Text("SpeakPro 只在你主动按下录音时才会采集麦克风音频 —— 所有分析都在云端完成，")
                    + Text("不用于训练第三方 AI 模型").foregroundColor(.spPrimary)
                    + Text("。")
                )
                .font(.system(size: 13))
                .foregroundColor(.spMuted)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .padding(.horizontal, 32)
                .padding(.top, 14)

                trustCard
                    .padding(.horizontal, 28)
                    .padding(.top, 24)

                Spacer()

                Button {
                    Task { await request() }
                } label: {
                    HStack(spacing: 8) {
                        if isRequesting {
                            SwiftUI.ProgressView().tint(.spIvory)
                        } else {
                            Text("同意并继续").font(.system(size: 14, weight: .semibold))
                            Image(systemName: "arrow.right")
                        }
                    }
                    .foregroundColor(.spIvory)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.spPrimary)
                    .clipShape(Capsule())
                }
                .disabled(isRequesting)
                .padding(.horizontal, 28)
                .padding(.bottom, 24)
            }
        }
        .navigationBarHidden(true)
    }

    private var trustCard: some View {
        VStack(spacing: 0) {
            ForEach(Array(trustPoints.enumerated()), id: \.offset) { idx, point in
                HStack(spacing: 10) {
                    Text(String(format: "%02d", idx + 1))
                        .font(.spSerif(11, italic: true))
                        .foregroundColor(.spAccent)
                        .frame(width: 20, alignment: .leading)
                    Text(point)
                        .font(.system(size: 12))
                        .foregroundColor(.spPrimary)
                    Spacer()
                }
                .padding(.vertical, 8)
                if idx < trustPoints.count - 1 {
                    Rectangle().fill(Color.spLine).frame(height: 1)
                }
            }
        }
        .padding(16)
        .background(Color.spIvory)
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var trustPoints: [String] {
        [
            "你可以随时在 iOS 设置里撤销",
            "录音文件默认保留 30 天后自动删除",
            "端到端加密传输 · AES-256",
        ]
    }

    private func request() async {
        isRequesting = true
        defer { isRequesting = false }
        let granted = await withCheckedContinuation { (cont: CheckedContinuation<Bool, Never>) in
            AVAudioApplication.requestRecordPermission { ok in
                cont.resume(returning: ok)
            }
        }
        onComplete(granted)
    }
}

#Preview {
    PermissionPrimeView { _ in }
}

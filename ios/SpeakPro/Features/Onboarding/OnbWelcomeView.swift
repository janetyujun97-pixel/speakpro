import SwiftUI

/// 01 · 欢迎页 — 深底页，介绍产品定位 + 3 条卖点
struct OnbWelcomeView: View {

    @ObservedObject var vm: OnboardingViewModel

    var body: some View {
        ZStack {
            Color.spPrimary.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text("SpeakPro")
                        .font(.spSerif(22, italic: true))
                        .foregroundColor(.spIvory)
                    Spacer()
                    Button("EN / 中") { }
                        .font(.system(size: 11))
                        .foregroundColor(.spIvory)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .overlay(
                            Capsule().stroke(Color.spIvory.opacity(0.3), lineWidth: 1)
                        )
                }
                .padding(.horizontal, 32)
                .padding(.top, 40)

                Spacer()

                VStack(alignment: .leading, spacing: 0) {
                    Text("SPEAKPRO · WELCOME")
                        .font(.spEyebrow).foregroundColor(Color.spIvory.opacity(0.55))
                    VStack(alignment: .leading, spacing: -8) {
                        Text("Speak, in")
                            .font(.spSerif(52)).foregroundColor(.spIvory)
                        Text("rhythm.")
                            .font(.spSerif(52, italic: true))
                            .foregroundColor(.spAccentWarm)
                    }
                    .padding(.top, 24)

                    Text("AI 考官陪你练习 11 分钟的 IELTS Speaking —— 和真考一样的节奏、评分、追问。")
                        .font(.system(size: 14))
                        .foregroundColor(Color.spIvory.opacity(0.75))
                        .lineSpacing(4)
                        .frame(maxWidth: 280, alignment: .leading)
                        .padding(.top, 28)

                    VStack(alignment: .leading, spacing: 14) {
                        proofRow("I", "真实考官口音 · 英美可选")
                        proofRow("II", "11 分钟完整模考 · 6 维度评分")
                        proofRow("III", "每日 15 分钟 · 逐句纠音")
                    }
                    .padding(.top, 40)
                }
                .padding(.horizontal, 32)

                Spacer()

                VStack(spacing: 14) {
                    Button {
                        Task {
                            await vm.patchCurrent()
                            vm.next()
                        }
                    } label: {
                        HStack(spacing: 8) {
                            Text("开始 · Get started")
                                .font(.system(size: 14, weight: .semibold))
                            Image(systemName: "arrow.right")
                        }
                        .foregroundColor(.spPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.spIvory)
                        .clipShape(Capsule())
                    }

                    Text("已有账号？ 登录")
                        .font(.system(size: 11))
                        .foregroundColor(Color.spIvory.opacity(0.55))
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 40)
            }
        }
    }

    private func proofRow(_ numeral: String, _ text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 14) {
            Text("\(numeral).")
                .font(.spSerif(16, italic: true))
                .foregroundColor(.spAccentWarm)
                .frame(width: 24, alignment: .leading)
            Text(text)
                .font(.system(size: 13))
                .foregroundColor(.spIvory)
            Spacer()
        }
    }
}

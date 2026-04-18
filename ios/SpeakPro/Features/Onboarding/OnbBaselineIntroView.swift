import SwiftUI

/// 06 · 基线录音前说明
struct OnbBaselineIntroView: View {

    @ObservedObject var vm: OnboardingViewModel

    private let prompt = "Tell me about a place you recently visited and enjoyed."

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 16) {
                Text("BASELINE · 05").font(.spEyebrow).foregroundColor(.spMuted)
                VStack(alignment: .leading, spacing: -4) {
                    Text("Let's hear you").font(.spSerif(36)).foregroundColor(.spPrimary)
                    Text("speak for 30s.")
                        .font(.spSerif(36, italic: true))
                        .foregroundColor(.spAccent)
                }
                Text("用 30 秒回答一个简单问题 —— 我们会快速分析你现在的 6 个维度，制定适合的计划。")
                    .font(.system(size: 14))
                    .foregroundColor(.spMuted)
                    .lineSpacing(4)
                    .frame(maxWidth: 280, alignment: .leading)
            }
            .padding(.horizontal, 24)
            .padding(.top, 28)

            promptCard
                .padding(.horizontal, 24)
                .padding(.top, 28)

            analyzeGrid
                .padding(.horizontal, 24)
                .padding(.top, 28)

            Spacer()

            VStack(spacing: 10) {
                Button {
                    vm.next()
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "mic.fill").font(.system(size: 14))
                        Text("开始录音 · 30 秒").font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundColor(.spIvory)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.spAccent)
                    .clipShape(Capsule())
                }
                Text("不紧张 · 说错也没关系")
                    .font(.system(size: 11))
                    .foregroundColor(.spMuted)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
    }

    private var promptCard: some View {
        ZStack(alignment: .topLeading) {
            VStack(alignment: .leading, spacing: 12) {
                Text(prompt)
                    .font(.spSerif(22, italic: true))
                    .foregroundColor(.spPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text("— IELTS Part 1 style · 一道 common 问题")
                    .font(.system(size: 11))
                    .foregroundColor(.spMuted)
            }
            .padding(24)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.spIvory)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color.spLine, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            Text("YOUR PROMPT")
                .font(.spEyebrow)
                .foregroundColor(.spAccent)
                .padding(.horizontal, 10)
                .background(Color.spBackground)
                .offset(x: 20, y: -8)
        }
    }

    private var analyzeGrid: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("WE'LL ANALYZE").font(.spEyebrow).foregroundColor(.spMuted)
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                analyzeCell("I", "Fluency · 流畅度")
                analyzeCell("II", "Pronunciation · 发音")
                analyzeCell("III", "Grammar · 语法")
                analyzeCell("IV", "Vocabulary · 词汇")
            }
        }
    }

    private func analyzeCell(_ numeral: String, _ label: String) -> some View {
        HStack(spacing: 8) {
            Text("\(numeral).")
                .font(.spSerif(13, italic: true))
                .foregroundColor(.spAccent)
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(.spPrimary)
            Spacer()
        }
    }
}

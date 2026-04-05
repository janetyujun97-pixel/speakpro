import SwiftUI

/// 跟读练习完整评分报告
struct FollowReadReportView: View {

    let scores: [FollowReadViewModel.SentenceScore]
    var onDismiss: (() -> Void)?

    // 各维度平均分
    private var avgPronunciation: Double {
        guard !scores.isEmpty else { return 0 }
        return scores.reduce(0) { $0 + $1.pronunciation } / Double(scores.count)
    }
    private var avgIntonation: Double {
        guard !scores.isEmpty else { return 0 }
        return scores.reduce(0) { $0 + $1.intonation } / Double(scores.count)
    }
    private var avgFluency: Double {
        guard !scores.isEmpty else { return 0 }
        return scores.reduce(0) { $0 + $1.fluency } / Double(scores.count)
    }
    private var overallScore: Double {
        (avgPronunciation + avgIntonation + avgFluency) / 3.0
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {

                    // MARK: - 总分
                    VStack(spacing: 8) {
                        Text("跟读练习报告")
                            .font(.spTitleLarge)
                            .foregroundColor(.spTextPrimary)

                        ScoreRing(
                            score: overallScore,
                            color: scoreColor(overallScore),
                            lineWidth: 12,
                            size: 120
                        )

                        Text("\(scores.count) 个句子 · 平均 \(String(format: "%.0f", overallScore)) 分")
                            .font(.spBodyMedium)
                            .foregroundColor(.spTextSecondary)
                    }
                    .padding(.top, 20)

                    // MARK: - 三维度平均分
                    HStack(spacing: 12) {
                        dimensionCard(title: "发音", score: avgPronunciation, color: .spSuccess)
                        dimensionCard(title: "语调", score: avgIntonation, color: .spAccent)
                        dimensionCard(title: "流利度", score: avgFluency, color: .spPrimary)
                    }
                    .padding(.horizontal, 16)

                    // MARK: - 每句详细评分
                    VStack(alignment: .leading, spacing: 12) {
                        Text("逐句评分")
                            .font(.spTitleSmall)
                            .foregroundColor(.spTextPrimary)
                            .padding(.horizontal, 16)

                        ForEach(Array(scores.enumerated()), id: \.offset) { index, score in
                            sentenceRow(index: index + 1, score: score)
                        }
                    }

                    // MARK: - 改进建议
                    VStack(alignment: .leading, spacing: 12) {
                        Text("改进建议")
                            .font(.spTitleSmall)
                            .foregroundColor(.spTextPrimary)

                        ForEach(suggestions, id: \.self) { tip in
                            HStack(alignment: .top, spacing: 8) {
                                Image(systemName: "lightbulb.fill")
                                    .foregroundColor(.spWarning)
                                    .font(.caption)
                                    .padding(.top, 2)
                                Text(tip)
                                    .font(.spBodySmall)
                                    .foregroundColor(.spTextSecondary)
                            }
                        }
                    }
                    .padding(16)
                    .background(Color.white)
                    .cornerRadius(12)
                    .padding(.horizontal, 16)

                    // MARK: - 完成按钮
                    Button {
                        onDismiss?()
                    } label: {
                        Text("完成")
                            .font(.spBodyMedium)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.spAccent)
                            .cornerRadius(12)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            }
            .background(Color.spBackground)
            .navigationTitle("练习报告")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("关闭") { onDismiss?() }
                        .foregroundColor(.spAccent)
                }
            }
        }
    }

    // MARK: - Components

    private func dimensionCard(title: String, score: Double, color: Color) -> some View {
        VStack(spacing: 8) {
            ScoreRing(score: score, color: color, lineWidth: 6, size: 60)
            Text(title)
                .font(.spCaption)
                .foregroundColor(.spTextSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color.white)
        .cornerRadius(12)
    }

    private func sentenceRow(index: Int, score: FollowReadViewModel.SentenceScore) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("第 \(index) 句")
                    .font(.spCaption)
                    .fontWeight(.semibold)
                    .foregroundColor(.spTextPrimary)
                Spacer()
                let avg = (score.pronunciation + score.intonation + score.fluency) / 3.0
                Text(String(format: "%.0f 分", avg))
                    .font(.spBodyMedium)
                    .fontWeight(.bold)
                    .foregroundColor(scoreColor(avg))
            }

            Text(score.sentence)
                .font(.spBodySmall)
                .foregroundColor(.spTextSecondary)
                .lineLimit(2)

            HStack(spacing: 16) {
                scoreTag("发音", score.pronunciation, .spSuccess)
                scoreTag("语调", score.intonation, .spAccent)
                scoreTag("流利度", score.fluency, .spPrimary)
            }
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(10)
        .padding(.horizontal, 16)
    }

    private func scoreTag(_ label: String, _ score: Double, _ color: Color) -> some View {
        HStack(spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: 6, height: 6)
            Text("\(label) \(Int(score))")
                .font(.system(size: 11))
                .foregroundColor(.spTextSecondary)
        }
    }

    // MARK: - Helpers

    private func scoreColor(_ score: Double) -> Color {
        if score >= 80 { return .spSuccess }
        if score >= 60 { return .spWarning }
        return .spError
    }

    private var suggestions: [String] {
        var tips: [String] = []
        if avgPronunciation < 70 {
            tips.append("发音准确度需要提升，建议放慢语速逐词模仿标准发音。")
        }
        if avgIntonation < 70 {
            tips.append("语调变化不够自然，注意句子中的升降调和重读词。")
        }
        if avgFluency < 70 {
            tips.append("流利度有待提高，减少停顿和犹豫，多做连读练习。")
        }
        if tips.isEmpty {
            tips.append("表现不错！继续保持每日跟读的习惯。")
        }
        return tips
    }
}

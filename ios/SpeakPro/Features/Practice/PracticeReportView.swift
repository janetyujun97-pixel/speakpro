import SwiftUI

/// 练习结束后的评分报告页
/// 显示总分环形图 + 5 维雷达图 + AI 反馈 + 改进建议
struct PracticeReportView: View {

    let sessionId: String
    @StateObject private var viewModel = PracticeReportViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {

                // MARK: - 总分
                VStack(spacing: 8) {
                    Text("练习报告")
                        .font(.spTitleLarge)
                        .foregroundColor(.spTextPrimary)

                    ScoreRing(
                        score: viewModel.overallScore,
                        color: scoreColor(viewModel.overallScore),
                        lineWidth: 12,
                        size: 120
                    )

                    Text(scoreLabel(viewModel.overallScore))
                        .font(.spBodyMedium)
                        .foregroundColor(.spTextSecondary)
                }
                .padding(.top, 16)

                // MARK: - 维度分析雷达图
                VStack(alignment: .leading, spacing: 12) {
                    Text("维度分析")
                        .font(.spTitleSmall)
                        .foregroundColor(.spTextPrimary)
                        .padding(.horizontal, 20)

                    RadarChart(
                        data: viewModel.dimensionScores,
                        labels: ["发音", "流利度", "语法", "内容", "连贯性"],
                        accentColor: .spAccent
                    )
                    .frame(height: 220)
                    .padding(.horizontal, 20)

                    // 维度分数列表
                    VStack(spacing: 8) {
                        ForEach(viewModel.dimensionItems, id: \.label) { item in
                            HStack {
                                Text(item.label)
                                    .font(.spBodySmall)
                                    .foregroundColor(.spTextSecondary)
                                Spacer()
                                Text("\(Int(item.score * 100))")
                                    .font(.spBodyMedium)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.spTextPrimary)
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                }
                .padding(.vertical, 16)
                .background(Color.spSurface)
                .cornerRadius(16)
                .padding(.horizontal, 16)

                // MARK: - AI 反馈
                if !viewModel.feedback.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("AI 反馈")
                            .font(.spTitleSmall)
                            .foregroundColor(.spTextPrimary)

                        Text(viewModel.feedback)
                            .font(.spBodySmall)
                            .foregroundColor(.spTextSecondary)
                            .lineSpacing(4)
                    }
                    .padding(20)
                    .background(Color.spSurface)
                    .cornerRadius(16)
                    .padding(.horizontal, 16)
                }

                // MARK: - 改进建议
                if !viewModel.suggestions.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("改进建议")
                            .font(.spTitleSmall)
                            .foregroundColor(.spTextPrimary)

                        ForEach(viewModel.suggestions, id: \.self) { suggestion in
                            HStack(alignment: .top, spacing: 8) {
                                Image(systemName: "lightbulb.fill")
                                    .foregroundColor(.spWarning)
                                    .font(.caption)
                                    .padding(.top, 2)
                                Text(suggestion)
                                    .font(.spBodySmall)
                                    .foregroundColor(.spTextSecondary)
                            }
                        }
                    }
                    .padding(20)
                    .background(Color.spSurface)
                    .cornerRadius(16)
                    .padding(.horizontal, 16)
                }

                // MARK: - 关闭按钮
                Button {
                    dismiss()
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
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadReport(sessionId: sessionId)
        }
    }

    // MARK: - Helpers

    private func scoreColor(_ score: Double) -> Color {
        if score >= 80 { return .spSuccess }
        if score >= 60 { return .spWarning }
        return .spError
    }

    private func scoreLabel(_ score: Double) -> String {
        if score >= 90 { return "优秀！继续保持" }
        if score >= 80 { return "不错，还有提升空间" }
        if score >= 60 { return "及格，需要更多练习" }
        return "继续努力！"
    }
}

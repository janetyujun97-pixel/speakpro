import SwiftUI

/// 模考总结页 — 总分 + 分部分得分 + 各题明细
struct MockExamSummaryView: View {

    let overallScore: Double
    let partAverages: [(part: Int, avg: Double)]
    let scores: [EnrichedQuestionScore]
    let onRetry: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {

                // MARK: - 总分
                VStack(spacing: 8) {
                    Text("考试报告")
                        .font(.spTitleLarge)
                        .foregroundColor(.spTextPrimary)

                    ScoreRing(
                        score: overallScore,
                        color: scoreColor(overallScore),
                        lineWidth: 14,
                        size: 140
                    )

                    Text(overallScore >= 80 ? "优秀" : overallScore >= 60 ? "良好" : "继续加油")
                        .font(.spBodyMedium)
                        .foregroundColor(.spTextSecondary)
                }
                .padding(.top, 20)

                // MARK: - 分部分得分
                VStack(alignment: .leading, spacing: 12) {
                    Text("各部分得分")
                        .font(.spTitleSmall)
                        .foregroundColor(.spTextPrimary)

                    ForEach(partAverages, id: \.part) { item in
                        HStack {
                            Text("Part \(item.part)")
                                .font(.spBodyMedium)
                                .foregroundColor(.spTextPrimary)
                                .frame(width: 60, alignment: .leading)

                            // 进度条
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(Color.spSurface)
                                        .frame(height: 8)
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(scoreColor(item.avg))
                                        .frame(width: geo.size.width * min(item.avg / 100.0, 1.0), height: 8)
                                }
                            }
                            .frame(height: 8)

                            Text(String(format: "%.0f", item.avg))
                                .font(.spBodyMedium)
                                .fontWeight(.semibold)
                                .foregroundColor(.spTextPrimary)
                                .frame(width: 40, alignment: .trailing)
                        }
                    }
                }
                .padding(20)
                .background(Color.spSurface)
                .cornerRadius(16)
                .padding(.horizontal, 16)

                // MARK: - 各题明细
                VStack(alignment: .leading, spacing: 12) {
                    Text("各题明细")
                        .font(.spTitleSmall)
                        .foregroundColor(.spTextPrimary)

                    ForEach(scores) { qs in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Part \(qs.part)")
                                    .font(.spCaption)
                                    .foregroundColor(.spAccent)
                                Text(qs.question)
                                    .font(.spBodySmall)
                                    .foregroundColor(.spTextSecondary)
                                    .lineLimit(2)
                            }
                            Spacer()
                            Text(String(format: "%.0f", qs.score))
                                .font(.spTitleSmall)
                                .fontWeight(.bold)
                                .foregroundColor(scoreColor(qs.score))
                        }
                        .padding(12)
                        .background(Color.white)
                        .cornerRadius(10)
                    }
                }
                .padding(20)
                .background(Color.spSurface)
                .cornerRadius(16)
                .padding(.horizontal, 16)

                // MARK: - 按钮
                VStack(spacing: 12) {
                    Button {
                        onRetry()
                    } label: {
                        Text("再测一次")
                            .font(.spBodyMedium)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.spAccent)
                            .cornerRadius(12)
                    }

                    Button {
                        onDismiss()
                    } label: {
                        Text("返回")
                            .font(.spBodyMedium)
                            .foregroundColor(.spTextSecondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
        }
        .background(Color.spBackground)
    }

    private func scoreColor(_ score: Double) -> Color {
        if score >= 80 { return .spSuccess }
        if score >= 60 { return .spWarning }
        return .spError
    }
}

import SwiftUI

/// 进度视图 — 注意：此处重命名为 SPProgressView 避免与 SwiftUI 内置 ProgressView 冲突
/// 在 AppCoordinator 的 TabView 中引用时使用 ProgressView() (本文件中的顶层 struct)
struct ProgressView: View {

    @StateObject private var viewModel = ProgressViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {

                    // MARK: - 综合评分曲线
                    scoreHistorySection

                    // MARK: - 雷达图
                    radarChartSection

                    // MARK: - 练习统计
                    practiceStatsSection

                    // MARK: - 薄弱项分析
                    weaknessSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
            .background(Color.spBackground)
            .navigationTitle("学习进度")
            .task {
                await viewModel.fetchProgress()
            }
        }
    }

    // MARK: - Score History (简化折线图)

    private var scoreHistorySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("评分趋势")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            // 简化的折线图
            GeometryReader { geo in
                let width = geo.size.width
                let height: CGFloat = 120
                let points = viewModel.scoreHistory
                let maxVal = points.max() ?? 100
                let minVal = max((points.min() ?? 0) - 10, 0)
                let range = max(maxVal - minVal, 1)

                Path { path in
                    for (index, score) in points.enumerated() {
                        let x = width * CGFloat(index) / CGFloat(max(points.count - 1, 1))
                        let y = height - (height * CGFloat(score - minVal) / CGFloat(range))
                        if index == 0 {
                            path.move(to: CGPoint(x: x, y: y))
                        } else {
                            path.addLine(to: CGPoint(x: x, y: y))
                        }
                    }
                }
                .stroke(Color.spAccent, lineWidth: 2)
                .frame(height: height)
            }
            .frame(height: 120)
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
    }

    // MARK: - Radar Chart

    private var radarChartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("能力分析")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            RadarChart(scores: viewModel.dimensionScores)
                .frame(maxWidth: .infinity)
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
    }

    // MARK: - Practice Stats

    private var practiceStatsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("练习统计")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 16) {
                statItem(value: "\(viewModel.stats.totalSessions)", label: "总练习次数")
                statItem(value: "\(viewModel.stats.totalMinutes)分钟", label: "总练习时长")
                statItem(value: "\(viewModel.stats.streakDays)天", label: "连续打卡")
            }
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
    }

    private func statItem(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.spTitleSmall)
                .foregroundColor(.spAccent)
            Text(label)
                .font(.spCaption)
                .foregroundColor(.spTextSecondary)
                .multilineTextAlignment(.center)
        }
    }

    // MARK: - Weakness Analysis

    private var weaknessSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("薄弱项分析")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            if viewModel.weakPoints.isEmpty {
                Text("暂无薄弱项数据")
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextSecondary)
                    .padding(.vertical, 12)
            } else {
                ForEach(viewModel.weakPoints, id: \.self) { point in
                    HStack(spacing: 10) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.spWarning)
                            .font(.spBodySmall)
                        Text(point)
                            .font(.spBodyMedium)
                            .foregroundColor(.spTextPrimary)
                    }
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.spWarning.opacity(0.06))
                    .cornerRadius(8)
                }
            }
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
    }
}

#Preview {
    ProgressView()
}

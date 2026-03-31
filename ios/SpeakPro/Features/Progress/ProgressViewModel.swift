import Foundation

/// 练习统计数据
struct PracticeStats {
    var totalSessions: Int = 0
    var totalMinutes: Int = 0
    var streakDays: Int = 0
}

/// 进度视图模型
final class ProgressViewModel: ObservableObject {

    // MARK: - Published

    @Published var stats = PracticeStats()
    @Published var scoreHistory: [Double] = []
    @Published var dimensionScores: [String: Double] = [:]
    @Published var weakPoints: [String] = []

    // MARK: - Fetch

    func fetchProgress() async {
        // TODO: 调用 API 获取学习进度数据
        await MainActor.run {
            stats = PracticeStats(
                totalSessions: 48,
                totalMinutes: 720,
                streakDays: 7
            )

            scoreHistory = [55, 58, 62, 60, 65, 68, 72, 70, 75, 78]

            dimensionScores = [
                "发音": 0.78,
                "语法": 0.62,
                "流利度": 0.71,
                "词汇": 0.55,
                "连贯性": 0.68
            ]

            weakPoints = [
                "词汇多样性不足，建议加强同义词替换练习",
                "语法中从句使用较少，可多练习复合句",
                "/θ/ 和 /ð/ 音发音需要改善"
            ]
        }
    }
}

import Foundation

/// 首页视图模型 —— 从真实 API 获取练习统计和待办作业
final class HomeViewModel: ObservableObject {

    @Published var streakDays: Int = 0
    @Published var todayProgress: Double = 0.0   // 0.0 ~ 1.0
    @Published var totalSessions: Int = 0
    @Published var recommendedPractices: [String] = []
    @Published var pendingHomework: [String] = []
    @Published var isLoading = false

    private let dailyGoal = 3 // 每日练习目标次数

    // MARK: - 获取首页数据

    func fetchHomeData() async {
        await MainActor.run { isLoading = true }

        // 并行获取统计和作业数据
        async let statsResult = fetchStats()
        async let homeworkResult = fetchPendingHomework()

        let _ = await (statsResult, homeworkResult)

        await MainActor.run { isLoading = false }
    }

    // MARK: - 练习统计

    private func fetchStats() async {
        do {
            let response: APIResponse<StatsResponse> = try await APIClient.shared.get(
                Endpoints.Practice.stats
            )

            await MainActor.run {
                if let data = response.data {
                    totalSessions = data.totalSessions
                    // 计算连续天数（从 recent.last7Days 近似推断）
                    streakDays = data.streakDays ?? min(data.recent.last7Days, 7)
                    // 今日进度 = 今日完成数 / 每日目标
                    let todayCount = data.todaySessionCount ?? data.recent.last7Days / 7
                    todayProgress = min(Double(todayCount) / Double(dailyGoal), 1.0)

                    // 根据薄弱项生成推荐练习
                    recommendedPractices = generateRecommendations(data.dimensions)
                }
            }
        } catch {
            print("[HomeVM] 统计加载失败: \(error)")
            await MainActor.run {
                // 显示默认推荐
                recommendedPractices = ["AI 对话练习", "跟读发音训练", "模考模拟"]
            }
        }
    }

    // MARK: - 待完成作业

    private func fetchPendingHomework() async {
        do {
            let response: APIResponse<[HomeworkItem]> = try await APIClient.shared.get(
                Endpoints.Assignments.list,
                queryItems: [URLQueryItem(name: "status", value: "pending")]
            )

            await MainActor.run {
                pendingHomework = response.data?.prefix(3).map { hw in
                    if let deadline = hw.dueDate {
                        return "\(hw.title) (截止 \(formatDeadline(deadline)))"
                    }
                    return hw.title
                } ?? []
            }
        } catch {
            print("[HomeVM] 作业加载失败: \(error)")
        }
    }

    // MARK: - 辅助方法

    private func generateRecommendations(_ dimensions: DimensionScores?) -> [String] {
        guard let dim = dimensions else {
            return ["AI 对话练习", "发音训练", "模考练习"]
        }

        var recs: [String] = []
        // 找出薄弱维度，推荐对应练习
        if dim.pronunciation < 70 { recs.append("发音跟读训练 — 提升发音准确度") }
        if dim.fluency < 70 { recs.append("AI 对话练习 — 提升流利度") }
        if dim.grammar < 70 { recs.append("朗读练习 — 强化语法表达") }
        if dim.content < 70 { recs.append("模考练习 — 提升内容组织能力") }
        if recs.isEmpty { recs.append("继续保持！尝试更高难度的练习") }
        return Array(recs.prefix(3))
    }

    private func formatDeadline(_ dateStr: String) -> String {
        let formatter = ISO8601DateFormatter()
        guard let date = formatter.date(from: dateStr) else { return dateStr }
        let diff = Calendar.current.dateComponents([.day, .hour], from: Date(), to: date)
        if let days = diff.day, days > 0 {
            return "\(days)天后"
        } else if let hours = diff.hour, hours > 0 {
            return "\(hours)小时后"
        }
        return "即将截止"
    }
}

// MARK: - API 响应模型

private struct StatsResponse: Decodable {
    let totalSessions: Int
    let averageScore: Double?
    let totalDurationMin: Int?
    let streakDays: Int?
    let todaySessionCount: Int?
    let dimensions: DimensionScores?
    let recent: RecentStats
}

private struct DimensionScores: Decodable {
    let pronunciation: Double
    let fluency: Double
    let grammar: Double
    let content: Double
}

private struct RecentStats: Decodable {
    let last7Days: Int
    let last30Days: Int
}

private struct HomeworkItem: Decodable {
    let id: String
    let title: String
    let dueDate: String?
}

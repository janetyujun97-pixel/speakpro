import Foundation

/// 推荐练习项（带跳转目标）
struct RecommendedItem: Identifiable {
    let id = UUID()
    let title: String
    let mode: PracticeMode
}

/// 首页视图模型 —— 从真实 API 获取练习统计和待办作业
final class HomeViewModel: ObservableObject {

    @Published var streakDays: Int = 0
    @Published var todayProgress: Double = 0.0   // 0.0 ~ 1.0
    @Published var totalSessions: Int = 0
    @Published var recommendedItems: [RecommendedItem] = []
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
                    recommendedItems = generateRecommendations(data.dimensions)
                }
            }
        } catch {
            print("[HomeVM] 统计加载失败: \(error)")
            await MainActor.run {
                // 显示默认推荐
                recommendedItems = [
                    RecommendedItem(title: "AI 对话练习", mode: .conversation),
                    RecommendedItem(title: "跟读发音训练", mode: .followRead),
                    RecommendedItem(title: "模考模拟", mode: .mockExam),
                ]
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
                // 只显示未完成的作业
                let pending = (response.data ?? []).filter { !$0.isCompleted }
                pendingHomework = pending.prefix(3).map { hw in
                    if let deadline = hw.dueDate {
                        return "\(hw.title) (截止 \(formatDeadline(deadline)))"
                    }
                    return hw.title
                }
            }
        } catch {
            print("[HomeVM] 作业加载失败: \(error)")
        }
    }

    // MARK: - 辅助方法

    private func generateRecommendations(_ dimensions: DimensionScores?) -> [RecommendedItem] {
        guard let dim = dimensions else {
            return [
                RecommendedItem(title: "AI 对话练习", mode: .conversation),
                RecommendedItem(title: "发音训练", mode: .followRead),
                RecommendedItem(title: "模考练习", mode: .mockExam),
            ]
        }

        var recs: [RecommendedItem] = []
        if dim.pronunciation < 70 { recs.append(RecommendedItem(title: "发音跟读训练 — 提升准确度", mode: .followRead)) }
        if dim.fluency < 70 { recs.append(RecommendedItem(title: "AI 对话练习 — 提升流利度", mode: .conversation)) }
        if dim.grammar < 70 { recs.append(RecommendedItem(title: "朗读练习 — 强化语法表达", mode: .readAloud)) }
        if dim.content < 70 { recs.append(RecommendedItem(title: "模考练习 — 提升内容组织", mode: .mockExam)) }
        if recs.isEmpty { recs.append(RecommendedItem(title: "继续保持！挑战更高难度", mode: .conversation)) }
        return Array(recs.prefix(3))
    }

    private func formatDeadline(_ dateStr: String) -> String {
        // 支持带毫秒和不带毫秒的 ISO 格式
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        var date = formatter.date(from: dateStr)
        if date == nil {
            formatter.formatOptions = [.withInternetDateTime]
            date = formatter.date(from: dateStr)
        }
        guard let parsedDate = date else {
            // 最后尝试截取前19个字符 + Z
            let cleaned = String(dateStr.prefix(19)) + "Z"
            let fallback = ISO8601DateFormatter()
            guard let d = fallback.date(from: cleaned) else { return "截止日期未知" }
            return d.deadlineString
        }
        return parsedDate.deadlineString
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
    let submissions: [HomeworkSubmission]?

    // 判断当前用户是否已提交/已批改
    var isCompleted: Bool {
        guard let subs = submissions, !subs.isEmpty else { return false }
        return subs.contains { $0.status == "submitted" || $0.status == "graded" }
    }
}

private struct HomeworkSubmission: Decodable {
    let status: String
}

import Foundation

/// 练习统计数据
struct PracticeStats {
    var totalSessions: Int = 0
    var totalMinutes: Int = 0
    var streakDays: Int = 0
}

/// 进度视图模型 —— 从真实 API 获取得分历史和维度分析
final class ProgressViewModel: ObservableObject {

    // MARK: - Published

    @Published var stats = PracticeStats()
    @Published var scoreHistory: [Double] = []
    @Published var dimensionScores: [String: Double] = [:]
    @Published var weakPoints: [String] = []
    @Published var isLoading = false

    // MARK: - 获取进度数据

    func fetchProgress() async {
        await MainActor.run { isLoading = true }

        // 并行获取统计和历史记录
        async let statsResult = fetchStats()
        async let historyResult = fetchScoreHistory()

        let _ = await (statsResult, historyResult)

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
                    stats = PracticeStats(
                        totalSessions: data.totalSessions,
                        totalMinutes: data.totalDurationMin ?? 0,
                        streakDays: data.streakDays ?? 0
                    )

                    // 映射维度评分（归一化为 0.0-1.0）
                    if let dim = data.dimensions {
                        dimensionScores = [
                            "发音": dim.pronunciation / 100.0,
                            "语法": dim.grammar / 100.0,
                            "流利度": dim.fluency / 100.0,
                            "词汇": (dim.content ?? 50) / 100.0,
                            "连贯性": (dim.content ?? 50) / 100.0,
                        ]
                    }

                    // 生成薄弱项提示
                    weakPoints = generateWeakPoints()
                }
            }
        } catch {
            print("[ProgressVM] 统计获取失败: \(error)")
            await MainActor.run { setPlaceholderData() }
        }
    }

    // MARK: - 得分历史

    private func fetchScoreHistory() async {
        do {
            let response: APIResponse<[SessionItem]> = try await APIClient.shared.get(
                Endpoints.Practice.sessions,
                queryItems: [URLQueryItem(name: "limit", value: "20")]
            )

            await MainActor.run {
                if let sessions = response.data {
                    scoreHistory = sessions
                        .compactMap { $0.overallScore }
                        .reversed()  // 按时间正序
                        .map { $0 }
                    // 至少保留一些数据
                    if scoreHistory.isEmpty {
                        scoreHistory = [0]
                    }
                }
            }
        } catch {
            print("[ProgressVM] 历史获取失败: \(error)")
        }
    }

    // MARK: - 薄弱项分析

    private func generateWeakPoints() -> [String] {
        var points: [String] = []
        for (name, score) in dimensionScores.sorted(by: { $0.value < $1.value }) {
            if score < 0.7 {
                switch name {
                case "发音":
                    points.append("发音准确度偏低（\(Int(score * 100))分），建议多做跟读练习")
                case "语法":
                    points.append("语法得分较低（\(Int(score * 100))分），注意时态和主谓一致")
                case "流利度":
                    points.append("流利度需提升（\(Int(score * 100))分），减少停顿和填充词")
                case "词汇":
                    points.append("词汇量不足（\(Int(score * 100))分），尝试使用更丰富的同义词")
                case "连贯性":
                    points.append("表达连贯性可改进（\(Int(score * 100))分），多用连接词过渡")
                default:
                    points.append("\(name)得分偏低（\(Int(score * 100))分）")
                }
            }
            if points.count >= 3 { break }
        }
        if points.isEmpty {
            points.append("各维度表现均衡，继续保持！")
        }
        return points
    }

    // MARK: - 占位数据

    private func setPlaceholderData() {
        stats = PracticeStats(totalSessions: 0, totalMinutes: 0, streakDays: 0)
        scoreHistory = []
        dimensionScores = [
            "发音": 0.5, "语法": 0.5, "流利度": 0.5, "词汇": 0.5, "连贯性": 0.5
        ]
        weakPoints = ["暂无数据，开始练习后将显示薄弱项分析"]
    }
}

// MARK: - API 响应模型

private struct StatsResponse: Decodable {
    let totalSessions: Int
    let totalDurationMin: Int?
    let streakDays: Int?
    let dimensions: DimScores?
}

private struct DimScores: Decodable {
    let pronunciation: Double
    let fluency: Double
    let grammar: Double
    let content: Double?
}

private struct SessionItem: Decodable {
    let id: String
    let overallScore: Double?
    let createdAt: String?
}

import Foundation

struct DimensionItem: Identifiable {
    let id = UUID()
    let label: String
    let score: Double // 0.0 ~ 1.0
}

@MainActor
final class PracticeReportViewModel: ObservableObject {

    @Published var overallScore: Double = 0
    @Published var dimensionScores: [Double] = [0, 0, 0, 0, 0]
    @Published var dimensionItems: [DimensionItem] = []
    @Published var feedback: String = ""
    @Published var suggestions: [String] = []
    @Published var isLoading = false

    func loadReport(sessionId: String) async {
        isLoading = true

        do {
            let response: APIResponse<SessionReport> = try await APIClient.shared.get(
                "/practice/sessions/\(sessionId)"
            )

            guard let report = response.data else {
                setPlaceholder()
                return
            }

            overallScore = report.overallScore ?? 0

            // 提取维度分数（归一化到 0-1）
            let pron = extractScore(report.pronunciationScore) / 100.0
            let flu = extractScore(report.fluencyScore) / 100.0
            let gram = extractScore(report.grammarScore) / 100.0
            let cont = extractScore(report.contentScore) / 100.0
            let coherence = (pron + flu + gram + cont) / 4.0 // 连贯性取平均值近似

            dimensionScores = [pron, flu, gram, cont, coherence]
            dimensionItems = [
                DimensionItem(label: "发音准确度", score: pron),
                DimensionItem(label: "流利度", score: flu),
                DimensionItem(label: "语法正确性", score: gram),
                DimensionItem(label: "内容相关性", score: cont),
                DimensionItem(label: "连贯性", score: coherence),
            ]

            feedback = report.aiFeedback ?? ""
            suggestions = generateSuggestions(pron: pron, flu: flu, gram: gram, cont: cont)

        } catch {
            setPlaceholder()
        }

        isLoading = false
    }

    // MARK: - Helpers

    private func extractScore(_ scoreObj: ScoreObj?) -> Double {
        guard let obj = scoreObj else { return 0 }
        return obj.overall ?? obj.score ?? 0
    }

    private func generateSuggestions(pron: Double, flu: Double, gram: Double, cont: Double) -> [String] {
        var result: [String] = []
        if pron < 0.7 {
            result.append("发音准确度偏低，建议多做跟读练习，注意元音和辅音的区分。")
        }
        if flu < 0.7 {
            result.append("流利度需要提升，尝试减少停顿和填充词（如 um, uh），多做限时练习。")
        }
        if gram < 0.7 {
            result.append("语法得分较低，注意时态一致性和主谓一致，可以多阅读英文文章。")
        }
        if cont < 0.7 {
            result.append("内容相关性不够，回答时要紧扣题目要求，用具体例子支撑观点。")
        }
        if result.isEmpty {
            result.append("表现不错！继续保持每日练习的习惯，尝试更有难度的话题。")
        }
        return result
    }

    private func setPlaceholder() {
        overallScore = 0
        dimensionScores = [0, 0, 0, 0, 0]
        dimensionItems = [
            DimensionItem(label: "发音准确度", score: 0),
            DimensionItem(label: "流利度", score: 0),
            DimensionItem(label: "语法正确性", score: 0),
            DimensionItem(label: "内容相关性", score: 0),
            DimensionItem(label: "连贯性", score: 0),
        ]
        feedback = "暂无评测数据"
        suggestions = []
    }
}

// MARK: - API Models

private struct SessionReport: Decodable {
    let id: String
    let overallScore: Double?
    let pronunciationScore: ScoreObj?
    let fluencyScore: ScoreObj?
    let grammarScore: ScoreObj?
    let contentScore: ScoreObj?
    let aiFeedback: String?
    let transcript: String?
}

private struct ScoreObj: Decodable {
    let overall: Double?
    let score: Double?
}

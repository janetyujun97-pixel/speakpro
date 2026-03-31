import Foundation

/// 首页视图模型
final class HomeViewModel: ObservableObject {

    @Published var streakDays: Int = 0
    @Published var todayProgress: Double = 0.0   // 0.0 ~ 1.0
    @Published var recommendedPractices: [String] = []
    @Published var pendingHomework: [String] = []

    // MARK: - Fetch Home Data

    func fetchHomeData() async {
        // TODO: 调用 API 获取首页数据
        // 暂用占位数据
        await MainActor.run {
            streakDays = 7
            todayProgress = 0.35
            recommendedPractices = [
                "雅思 Part2 话题",
                "发音纠正训练",
                "高频词汇跟读"
            ]
            pendingHomework = [
                "Unit 5 口语作业 (截止明天)",
                "模考练习 - Set 3"
            ]
        }
    }
}

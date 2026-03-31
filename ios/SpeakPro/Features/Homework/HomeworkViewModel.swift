import Foundation

// MARK: - Models

struct HomeworkAssignment: Identifiable {
    let id: String
    let title: String
    let teacherName: String
    let deadline: Date
    let isCompleted: Bool
    let score: Double?
    let questions: [HomeworkQuestion]

    static let sample = HomeworkAssignment(
        id: "hw_1",
        title: "Unit 5 口语话题练习",
        teacherName: "王老师",
        deadline: Date().addingTimeInterval(86400),
        isCompleted: false,
        score: nil,
        questions: [
            HomeworkQuestion(text: "Describe your favorite place to relax.", isCompleted: true, score: 78),
            HomeworkQuestion(text: "Talk about a skill you'd like to learn.", isCompleted: false, score: nil),
            HomeworkQuestion(text: "Describe a book that influenced you.", isCompleted: false, score: nil)
        ]
    )
}

struct HomeworkQuestion {
    let text: String
    let isCompleted: Bool
    let score: Double?
}

// MARK: - ViewModel

final class HomeworkViewModel: ObservableObject {

    enum HomeworkTab {
        case pending
        case completed
    }

    // MARK: - Published

    @Published var assignments: [HomeworkAssignment] = []
    @Published var selectedTab: HomeworkTab = .pending
    @Published var isSubmitting = false

    var filteredAssignments: [HomeworkAssignment] {
        switch selectedTab {
        case .pending:
            return assignments.filter { !$0.isCompleted }
        case .completed:
            return assignments.filter { $0.isCompleted }
        }
    }

    // MARK: - Fetch

    func fetchAssignments() async {
        // TODO: 调用 API 获取作业列表
        await MainActor.run {
            assignments = [
                HomeworkAssignment(
                    id: "hw_1",
                    title: "Unit 5 口语话题练习",
                    teacherName: "王老师",
                    deadline: Date().addingTimeInterval(86400),
                    isCompleted: false,
                    score: nil,
                    questions: [
                        HomeworkQuestion(text: "Describe your favorite place.", isCompleted: true, score: 78),
                        HomeworkQuestion(text: "Talk about a skill you'd like to learn.", isCompleted: false, score: nil)
                    ]
                ),
                HomeworkAssignment(
                    id: "hw_2",
                    title: "模考练习 - Set 3",
                    teacherName: "李老师",
                    deadline: Date().addingTimeInterval(172800),
                    isCompleted: false,
                    score: nil,
                    questions: [
                        HomeworkQuestion(text: "Part 1 questions", isCompleted: false, score: nil)
                    ]
                ),
                HomeworkAssignment(
                    id: "hw_3",
                    title: "Unit 4 跟读作业",
                    teacherName: "王老师",
                    deadline: Date().addingTimeInterval(-86400),
                    isCompleted: true,
                    score: 82,
                    questions: [
                        HomeworkQuestion(text: "Follow-read passage 1", isCompleted: true, score: 85),
                        HomeworkQuestion(text: "Follow-read passage 2", isCompleted: true, score: 79)
                    ]
                )
            ]
        }
    }

    // MARK: - Submit

    func submitAssignment(id: String) async {
        // TODO: 调用 API 提交作业
        isSubmitting = true
        // 模拟网络延迟
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        await MainActor.run {
            isSubmitting = false
            // TODO: 刷新列表
        }
    }
}

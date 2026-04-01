import Foundation

// MARK: - Models

struct HomeworkAssignment: Identifiable, Decodable {
    let id: String
    let title: String
    let description: String?
    let questionIds: [String]?
    let dueDate: String?
    let submissions: [SubmissionInfo]?
    let teacher: TeacherInfo?
    let createdAt: String?

    var teacherName: String { teacher?.name ?? "未知教师" }
    var isCompleted: Bool { mySubmission?.status == "graded" }
    var isSubmitted: Bool { mySubmission?.status == "submitted" || isCompleted }
    var score: Double? { mySubmission?.teacherScore }

    // 当前用户的提交（简化处理：取第一个）
    var mySubmission: SubmissionInfo? { submissions?.first }

    var deadline: Date? {
        guard let dateStr = dueDate else { return nil }
        let fmt = ISO8601DateFormatter()
        return fmt.date(from: dateStr)
    }
}

struct SubmissionInfo: Decodable {
    let id: String
    let status: String
    let teacherScore: Double?
    let teacherComment: String?
}

struct TeacherInfo: Decodable {
    let id: String
    let name: String
}

struct HomeworkQuestion: Identifiable {
    let id: String
    let text: String
    var isCompleted: Bool
    var score: Double?
    var sessionId: String?
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
    @Published var isLoading = false
    @Published var isSubmitting = false
    @Published var errorMessage: String?

    var filteredAssignments: [HomeworkAssignment] {
        switch selectedTab {
        case .pending:
            return assignments.filter { !$0.isCompleted && !$0.isSubmitted }
        case .completed:
            return assignments.filter { $0.isCompleted || $0.isSubmitted }
        }
    }

    // MARK: - 获取作业列表

    func fetchAssignments() async {
        await MainActor.run { isLoading = true; errorMessage = nil }

        do {
            let response: APIResponse<[HomeworkAssignment]> = try await APIClient.shared.get(
                Endpoints.Assignments.list
            )

            await MainActor.run {
                assignments = response.data ?? []
                isLoading = false
            }
        } catch {
            await MainActor.run {
                isLoading = false
                errorMessage = "加载失败: \(error.localizedDescription)"
                // 无数据时保持空列表
            }
        }
    }

    // MARK: - 提交作业

    func submitAssignment(id: String, sessionIds: [String]) async {
        await MainActor.run { isSubmitting = true; errorMessage = nil }

        do {
            let _: APIResponse<SubmissionInfo> = try await APIClient.shared.post(
                Endpoints.Assignments.submit(id: id),
                body: SubmitBody(sessionIds: sessionIds)
            )

            // 刷新列表
            await fetchAssignments()
            await MainActor.run { isSubmitting = false }
        } catch {
            await MainActor.run {
                isSubmitting = false
                errorMessage = "提交失败: \(error.localizedDescription)"
            }
        }
    }
}

// MARK: - 请求模型

private struct SubmitBody: Encodable {
    let sessionIds: [String]
}

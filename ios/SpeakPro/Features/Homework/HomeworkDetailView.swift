import SwiftUI

/// 作业详情视图 — 适配新的 HomeworkAssignment 模型
struct HomeworkDetailView: View {

    let assignment: HomeworkAssignment
    @StateObject private var viewModel = HomeworkViewModel()
    @State private var questions: [HWQuestion] = []
    @State private var loadingQuestions = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                headerSection

                if !assignment.isCompleted {
                    deadlineSection
                }

                infoSection

                if !assignment.isSubmitted {
                    submitButton
                }

                if let score = assignment.score {
                    scoreSection(score: score)
                }
            }
            .padding(20)
        }
        .background(Color.spBackground)
        .navigationTitle("作业详情")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadQuestions()
        }
    }

    // MARK: - Load Questions

    private func loadQuestions() async {
        guard let ids = assignment.questionIds, !ids.isEmpty else { return }
        loadingQuestions = true
        do {
            let resp: APIResponse<HWQListResponse> = try await APIClient.shared.get(
                Endpoints.Questions.list
            )
            if let items = resp.data?.items {
                // 按 questionIds 过滤并排序
                let idSet = Set(ids)
                questions = items.filter { idSet.contains($0.id) }
            }
        } catch {
            print("[HomeworkDetail] 加载题目失败: \(error)")
        }
        loadingQuestions = false
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(assignment.title)
                .font(.spTitleMedium)
                .foregroundColor(.spTextPrimary)

            HStack(spacing: 12) {
                Label(assignment.teacherName, systemImage: "person.fill")
                Label("\(assignment.questionIds?.count ?? 0) 道题", systemImage: "list.number")
            }
            .font(.spBodySmall)
            .foregroundColor(.spTextSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
    }

    // MARK: - Deadline

    private var deadlineSection: some View {
        Group {
            if let deadline = assignment.deadline {
                HStack {
                    Image(systemName: "clock.badge.exclamationmark.fill")
                        .foregroundColor(.spWarning)
                    Text("截止时间: \(deadline, style: .date) \(deadline, style: .time)")
                        .font(.spBodyMedium)
                        .foregroundColor(.spTextPrimary)
                    Spacer()
                    if deadline.timeIntervalSinceNow < 0 {
                        Text("已过期")
                            .font(.spCaption)
                            .foregroundColor(.spError)
                    } else if deadline.timeIntervalSinceNow < 86400 {
                        Text("即将截止")
                            .font(.spCaption)
                            .foregroundColor(.spWarning)
                    }
                }
                .padding(14)
                .background(Color.spWarning.opacity(0.08))
                .cornerRadius(10)
            }
        }
    }

    // MARK: - Info

    private var infoSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("作业信息")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            if let desc = assignment.description, !desc.isEmpty {
                Text(desc)
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextSecondary)
                    .padding(12)
                    .background(Color.white)
                    .cornerRadius(10)
            }

            HStack {
                Label(assignment.isSubmitted ? "已提交" : "未提交", systemImage: assignment.isSubmitted ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(assignment.isSubmitted ? .spSuccess : .spTextSecondary)
                Spacer()
                if let sub = assignment.mySubmission {
                    let statusText = sub.status == "graded" ? "已批改" : sub.status == "submitted" ? "已提交" : "待完成"
                    Text(statusText)
                        .font(.spCaption)
                        .foregroundColor(sub.status == "graded" ? .spSuccess : .spWarning)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background((sub.status == "graded" ? Color.spSuccess : Color.spWarning).opacity(0.1))
                        .cornerRadius(4)
                }
            }
            .font(.spBodySmall)
            .padding(12)
            .background(Color.white)
            .cornerRadius(10)

            // 题目列表
            if let qIds = assignment.questionIds, !qIds.isEmpty {
                questionsSection(questionIds: qIds)
            }
        }
    }

    // MARK: - Score

    private func scoreSection(score: Double) -> some View {
        VStack(spacing: 8) {
            Text("教师评分")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)
            ScoreRing(score: score, color: score >= 80 ? .spSuccess : score >= 60 ? .spWarning : .spError, size: 100)
            if let comment = assignment.mySubmission?.teacherComment {
                Text(comment)
                    .font(.spBodySmall)
                    .foregroundColor(.spTextSecondary)
                    .padding(12)
                    .background(Color.white)
                    .cornerRadius(10)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(16)
        .background(Color.spSurface)
        .cornerRadius(12)
    }

    // MARK: - Questions List

    private func questionsSection(questionIds: [String]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("练习题目 (\(questionIds.count) 道)")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            if loadingQuestions {
                HStack {
                    Spacer()
                    SwiftUI.ProgressView()
                    Text("加载题目中...")
                        .font(.spCaption)
                        .foregroundColor(.spTextSecondary)
                    Spacer()
                }
                .padding(.vertical, 20)
            } else if questions.isEmpty {
                // 没有加载到题目详情，用序号显示
                ForEach(Array(questionIds.enumerated()), id: \.offset) { index, _ in
                    questionRow(index: index, title: "第 \(index + 1) 题", subtitle: "口语练习")
                }
            } else {
                ForEach(Array(questions.enumerated()), id: \.offset) { index, q in
                    questionRow(
                        index: index,
                        title: q.topic ?? q.section ?? "第 \(index + 1) 题",
                        subtitle: q.promptText,
                        examType: q.examType,
                        section: q.section
                    )
                }
            }
        }
    }

    @State private var expandedQuestion: Int? = nil

    private func questionRow(index: Int, title: String, subtitle: String, examType: String? = nil, section: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            // 主行
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    expandedQuestion = expandedQuestion == index ? nil : index
                }
            } label: {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.spAccent.opacity(0.1))
                            .frame(width: 32, height: 32)
                        Text("\(index + 1)")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.spAccent)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 6) {
                            if let et = examType {
                                Text(et)
                                    .font(.system(size: 9, weight: .semibold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 2)
                                    .background(Color.spAccent)
                                    .cornerRadius(3)
                            }
                            if let sec = section {
                                Text(sec)
                                    .font(.system(size: 9))
                                    .foregroundColor(.spTextSecondary)
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 2)
                                    .background(Color(.systemGray6))
                                    .cornerRadius(3)
                            }
                        }
                        Text(title)
                            .font(.spBodyMedium)
                            .foregroundColor(.spTextPrimary)
                            .lineLimit(expandedQuestion == index ? nil : 1)
                    }

                    Spacer()

                    if assignment.isSubmitted {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.spSuccess)
                    }

                    Image(systemName: expandedQuestion == index ? "chevron.up" : "chevron.down")
                        .font(.system(size: 11))
                        .foregroundColor(.spTextSecondary)
                }
            }
            .buttonStyle(.plain)
            .padding(12)

            // 展开详情
            if expandedQuestion == index {
                VStack(alignment: .leading, spacing: 10) {
                    Divider()

                    Text(subtitle)
                        .font(.spBodySmall)
                        .foregroundColor(.spTextSecondary)
                        .lineSpacing(4)
                        .padding(.horizontal, 12)

                    if !assignment.isSubmitted {
                        NavigationLink(destination: ConversationView()) {
                            HStack(spacing: 6) {
                                Image(systemName: "mic.fill")
                                    .font(.system(size: 12))
                                Text("开始练习")
                                    .font(.spCaption)
                                    .fontWeight(.semibold)
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.spAccent)
                            .cornerRadius(8)
                        }
                        .padding(.horizontal, 12)
                    }
                }
                .padding(.bottom, 12)
            }
        }
        .background(Color.white)
        .cornerRadius(10)
    }

    // MARK: - Submit

    private var submitButton: some View {
        Button {
            Task {
                await viewModel.submitAssignment(id: assignment.id, sessionIds: [])
            }
        } label: {
            Text(viewModel.isSubmitting ? "提交中..." : "提交作业")
                .font(.spBodyLarge)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(viewModel.isSubmitting ? Color.gray : Color.spAccent)
                .cornerRadius(12)
        }
        .disabled(viewModel.isSubmitting)
    }
}

// MARK: - API Models

private struct HWQListResponse: Decodable {
    let items: [HWQuestion]?
}

struct HWQuestion: Decodable, Identifiable {
    let id: String
    let examType: String?
    let section: String?
    let topic: String?
    let promptText: String

    enum CodingKeys: String, CodingKey {
        case id, examType, section, topic, promptText
    }
}

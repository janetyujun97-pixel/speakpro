import SwiftUI

/// 作业详情视图
struct HomeworkDetailView: View {

    let assignment: HomeworkAssignment
    @StateObject private var viewModel = HomeworkViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {

                // MARK: - 作业标题 & 信息
                headerSection

                // MARK: - 截止时间
                if !assignment.isCompleted {
                    deadlineSection
                }

                // MARK: - 题目列表与进度
                questionsSection

                // MARK: - 提交按钮
                if !assignment.isCompleted {
                    submitButton
                }
            }
            .padding(20)
        }
        .background(Color.spBackground)
        .navigationTitle("作业详情")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Header

    private var headerSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(assignment.title)
                .font(.spTitleMedium)
                .foregroundColor(.spTextPrimary)

            HStack(spacing: 12) {
                Label(assignment.teacherName, systemImage: "person.fill")
                Label("\(assignment.questions.count) 道题", systemImage: "list.number")
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
        HStack {
            Image(systemName: "clock.badge.exclamationmark.fill")
                .foregroundColor(.spWarning)
            Text("截止时间: \(assignment.deadline.formattedDateTime)")
                .font(.spBodyMedium)
                .foregroundColor(.spTextPrimary)
            Spacer()
            Text(assignment.deadline.deadlineString)
                .font(.spBodySmall)
                .foregroundColor(.spWarning)
        }
        .padding(14)
        .background(Color.spWarning.opacity(0.08))
        .cornerRadius(10)
    }

    // MARK: - Questions

    private var questionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("题目列表")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            ForEach(Array(assignment.questions.enumerated()), id: \.offset) { index, question in
                HStack(spacing: 12) {
                    // 序号 / 完成标记
                    ZStack {
                        Circle()
                            .fill(question.isCompleted ? Color.spSuccess : Color.gray.opacity(0.2))
                            .frame(width: 28, height: 28)

                        if question.isCompleted {
                            Image(systemName: "checkmark")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(.white)
                        } else {
                            Text("\(index + 1)")
                                .font(.spCaption)
                                .foregroundColor(.spTextSecondary)
                        }
                    }

                    Text(question.text)
                        .font(.spBodyMedium)
                        .foregroundColor(.spTextPrimary)
                        .lineLimit(2)

                    Spacer()

                    if let score = question.score {
                        Text("\(Int(score))分")
                            .font(.spCaption)
                            .foregroundColor(.spAccent)
                    }
                }
                .padding(12)
                .background(Color.white)
                .cornerRadius(10)
            }
        }
    }

    // MARK: - Submit

    private var submitButton: some View {
        Button {
            Task {
                await viewModel.submitAssignment(id: assignment.id)
            }
        } label: {
            Text("提交作业")
                .font(.spBodyLarge)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Color.spAccent)
                .cornerRadius(12)
        }
    }
}

#Preview {
    NavigationStack {
        HomeworkDetailView(
            assignment: HomeworkAssignment.sample
        )
    }
}

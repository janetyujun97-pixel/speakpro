import SwiftUI

/// 作业列表视图
struct HomeworkListView: View {

    @StateObject private var viewModel = HomeworkViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // 顶部 Tab: 待完成 / 已完成
                Picker("", selection: $viewModel.selectedTab) {
                    Text("待完成").tag(HomeworkViewModel.HomeworkTab.pending)
                    Text("已完成").tag(HomeworkViewModel.HomeworkTab.completed)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 20)
                .padding(.vertical, 12)

                // 列表
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.filteredAssignments) { assignment in
                            NavigationLink(destination: HomeworkDetailView(assignment: assignment)) {
                                homeworkRow(assignment)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                }
            }
            .background(Color.spBackground)
            .navigationTitle("作业")
            .task {
                await viewModel.fetchAssignments()
            }
            .onReceive(NotificationCenter.default.publisher(for: .switchToPendingHomework)) { _ in
                viewModel.selectedTab = .pending
            }
        }
    }

    // MARK: - Row

    private func homeworkRow(_ assignment: HomeworkAssignment) -> some View {
        HStack(spacing: 14) {
            // 状态图标
            ZStack {
                Circle()
                    .fill(assignment.isCompleted ? Color.spSuccess.opacity(0.1) : Color.spWarning.opacity(0.1))
                    .frame(width: 40, height: 40)

                Image(systemName: assignment.isCompleted ? "checkmark.circle.fill" : "clock.fill")
                    .foregroundColor(assignment.isCompleted ? .spSuccess : .spWarning)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(assignment.title)
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextPrimary)

                HStack(spacing: 8) {
                    Text(assignment.teacherName)
                        .font(.spCaption)
                        .foregroundColor(.spTextSecondary)

                    if !assignment.isCompleted, let deadline = assignment.deadline {
                        Text(deadline, style: .relative)
                            .font(.spCaption)
                            .foregroundColor(
                                deadline.timeIntervalSinceNow < 86400
                                    ? .spError : .spTextSecondary
                            )
                    }
                }
            }

            Spacer()

            // 分数（已完成时显示）
            if assignment.isCompleted, let score = assignment.score {
                Text("\(Int(score))")
                    .font(.spTitleSmall)
                    .foregroundColor(.spAccent)
            }

            Image(systemName: "chevron.right")
                .font(.spCaption)
                .foregroundColor(.spTextSecondary)
        }
        .padding(14)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
    }
}

#Preview {
    HomeworkListView()
}

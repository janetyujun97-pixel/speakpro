import SwiftUI

/// 模考视图 — 模拟完整的口语考试流程
struct MockExamView: View {

    @StateObject private var viewModel = MockExamViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {

            // MARK: - 顶部状态栏
            examHeader

            Divider()

            // MARK: - 题目显示
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Part \(viewModel.currentPart)")
                        .font(.spCaption)
                        .foregroundColor(.spAccent)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.spAccent.opacity(0.1))
                        .cornerRadius(6)

                    Text(viewModel.currentQuestion)
                        .font(.spBodyLarge)
                        .foregroundColor(.spTextPrimary)
                        .lineSpacing(6)

                    if !viewModel.subQuestions.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(viewModel.subQuestions, id: \.self) { sub in
                                HStack(alignment: .top, spacing: 8) {
                                    Circle()
                                        .fill(Color.spTextSecondary)
                                        .frame(width: 6, height: 6)
                                        .padding(.top, 6)
                                    Text(sub)
                                        .font(.spBodyMedium)
                                        .foregroundColor(.spTextSecondary)
                                }
                            }
                        }
                    }
                }
                .padding(20)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)

            Spacer()

            // MARK: - 录音控制区
            VStack(spacing: 16) {
                // 准备/录音/已完成 状态提示
                Text(viewModel.statusText)
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextSecondary)

                RecordButton(isRecording: $viewModel.isRecording) {
                    if viewModel.isRecording {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                }

                // 下一题按钮
                if viewModel.canProceed {
                    Button("下一题") {
                        viewModel.nextQuestion()
                    }
                    .font(.spBodyMedium)
                    .foregroundColor(.spAccent)
                }
            }
            .padding(.bottom, 32)
        }
        .background(Color.spBackground)
        .navigationTitle("模拟考试")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("结束考试") {
                    viewModel.endExam()
                    dismiss()
                }
                .foregroundColor(.spError)
            }
        }
    }

    // MARK: - Exam Header

    private var examHeader: some View {
        HStack {
            // 进度
            Text("题目 \(viewModel.currentQuestionIndex + 1)/\(viewModel.totalQuestions)")
                .font(.spBodySmall)
                .foregroundColor(.spTextSecondary)

            Spacer()

            // 计时器
            HStack(spacing: 4) {
                Image(systemName: "timer")
                Text(viewModel.formattedTime)
                    .monospacedDigit()
            }
            .font(.spBodyMedium)
            .foregroundColor(viewModel.remainingTime < 30 ? .spError : .spTextPrimary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 10)
        .background(Color.white)
    }
}

#Preview {
    NavigationStack {
        MockExamView()
    }
}

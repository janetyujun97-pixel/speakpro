import SwiftUI

/// 模考视图 — 完整计时口语考试流程
struct MockExamView: View {

    @StateObject private var viewModel = MockExamViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            switch viewModel.phase {
            case .loading:
                loadingView
            case .sectionTransition:
                sectionTransitionView
            case .finished:
                MockExamSummaryView(
                    overallScore: viewModel.overallScore,
                    partAverages: viewModel.partAverages,
                    scores: viewModel.examScores,
                    onRetry: { viewModel.loadExamQuestions() },
                    onDismiss: { dismiss() }
                )
            default:
                examContentView
            }
        }
        .background(Color.spBackground)
        .navigationTitle("模拟考试")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if viewModel.phase != .finished {
                    Button("结束考试") {
                        viewModel.endExam()
                    }
                    .foregroundColor(.spError)
                }
            }
        }
    }

    // MARK: - 加载中

    private var loadingView: some View {
        VStack(spacing: 16) {
            SwiftUI.ProgressView()
                .scaleEffect(1.2)
            Text("正在加载考试题目...")
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - 部分过渡屏

    private var sectionTransitionView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(.spSuccess)

            Text("Part \(viewModel.transitionFromPart) 完成！")
                .font(.spTitleLarge)
                .foregroundColor(.spTextPrimary)

            Text("即将进入 Part \(viewModel.transitionToPart)")
                .font(.spBodyLarge)
                .foregroundColor(.spTextSecondary)

            Spacer()

            Button {
                viewModel.continueAfterTransition()
            } label: {
                Text("继续")
                    .font(.spBodyMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.spAccent)
                    .cornerRadius(12)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 40)
        }
    }

    // MARK: - 考试内容

    private var examContentView: some View {
        VStack(spacing: 0) {
            // 顶部状态栏
            examHeader

            // 进度条
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(Color.spSurface)
                        .frame(height: 3)
                    Rectangle()
                        .fill(Color.spAccent)
                        .frame(width: geo.size.width * viewModel.progress, height: 3)
                        .animation(.easeInOut(duration: 0.3), value: viewModel.progress)
                }
            }
            .frame(height: 3)

            // 暂停遮罩
            if viewModel.isPaused {
                pauseOverlay
            } else {
                // 题目显示
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

                // 录音控制区
                VStack(spacing: 16) {
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
        }
    }

    // MARK: - 暂停遮罩

    private var pauseOverlay: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "pause.circle.fill")
                .font(.system(size: 64))
                .foregroundColor(.spAccent)

            Text("考试已暂停")
                .font(.spTitleMedium)
                .foregroundColor(.spTextPrimary)

            Text("剩余时间: \(viewModel.formattedTime)")
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)

            Button {
                viewModel.togglePause()
            } label: {
                Text("继续考试")
                    .font(.spBodyMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(width: 200)
                    .padding(.vertical, 14)
                    .background(Color.spAccent)
                    .cornerRadius(12)
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.spBackground)
    }

    // MARK: - Exam Header

    private var examHeader: some View {
        HStack {
            // 进度
            Text("题目 \(viewModel.currentQuestionIndex + 1)/\(viewModel.totalQuestions)")
                .font(.spBodySmall)
                .foregroundColor(.spTextSecondary)

            Spacer()

            // 暂停按钮
            Button {
                viewModel.togglePause()
            } label: {
                Image(systemName: viewModel.isPaused ? "play.fill" : "pause.fill")
                    .font(.spBodySmall)
                    .foregroundColor(.spTextSecondary)
                    .padding(6)
                    .background(Color.spSurface)
                    .cornerRadius(6)
            }

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

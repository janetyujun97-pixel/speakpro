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
            case .evaluating:
                evaluatingView
            case .showingResult:
                resultView
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
                if viewModel.phase == .inProgress || viewModel.phase == .ready {
                    Button("结束考试") { viewModel.endExam() }
                        .foregroundColor(.spAccent)
                }
            }
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            SwiftUI.ProgressView()
                .scaleEffect(1.5)
            Text("正在加载题目...")
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Exam Content (Ready + InProgress)

    private var examContentView: some View {
        VStack(spacing: 0) {
            // 进度条 + 计时器
            HStack {
                Text("题目 \(viewModel.currentQuestionIndex + 1)/\(viewModel.totalQuestions)")
                    .font(.spCaption)
                    .foregroundColor(.spTextSecondary)

                Spacer()

                // 暂停按钮
                Button { viewModel.togglePause() } label: {
                    Image(systemName: viewModel.isPaused ? "play.fill" : "pause.fill")
                        .font(.system(size: 16))
                        .foregroundColor(.spTextSecondary)
                }

                // 计时
                HStack(spacing: 4) {
                    Image(systemName: "timer")
                        .font(.system(size: 12))
                    Text(viewModel.formattedTime)
                        .font(.spBodyMedium)
                        .monospacedDigit()
                }
                .foregroundColor(viewModel.remainingTime < 30 ? .spError : .spTextSecondary)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 10)

            // 进度条
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(Color(.systemGray5))
                        .frame(height: 3)
                    Rectangle()
                        .fill(Color.spAccent)
                        .frame(width: geo.size.width * viewModel.progress, height: 3)
                        .animation(.easeInOut(duration: 0.3), value: viewModel.progress)
                }
            }
            .frame(height: 3)

            // 题目内容
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Part \(viewModel.currentPart)")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.spAccent)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(Color.spAccent.opacity(0.1))
                        .cornerRadius(4)

                    Text(viewModel.currentQuestion)
                        .font(.spBodyLarge)
                        .foregroundColor(.spTextPrimary)
                        .lineSpacing(6)

                    if !viewModel.subQuestions.isEmpty {
                        ForEach(viewModel.subQuestions, id: \.self) { sub in
                            HStack(alignment: .top, spacing: 8) {
                                Text("•")
                                    .foregroundColor(.spAccent)
                                Text(sub)
                                    .font(.spBodyMedium)
                                    .foregroundColor(.spTextSecondary)
                            }
                        }
                    }
                }
                .padding(20)
            }

            Spacer()

            // 错误提示
            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.spCaption)
                    .foregroundColor(.spError)
                    .padding(.horizontal, 20)
            }

            // 录音波形
            if viewModel.isRecording {
                WaveformView(data: viewModel.audioRecorder.waveformData, barColor: .spAccent)
                    .frame(height: 30)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 8)
            }

            // 录音按钮
            RecordButton(isRecording: $viewModel.isRecording) {
                if viewModel.isRecording {
                    viewModel.stopRecording()
                } else {
                    viewModel.startRecording()
                }
            }
            .disabled(viewModel.isPaused)
            .opacity(viewModel.isPaused ? 0.5 : 1)
            .padding(.bottom, 32)
        }
        .overlay {
            if viewModel.isPaused {
                Color.black.opacity(0.5)
                    .ignoresSafeArea()
                    .overlay {
                        VStack(spacing: 16) {
                            Image(systemName: "pause.circle.fill")
                                .font(.system(size: 48))
                                .foregroundColor(.white)
                            Text("考试已暂停")
                                .font(.spTitleMedium)
                                .foregroundColor(.white)
                            Button("继续") { viewModel.togglePause() }
                                .font(.spBodyMedium)
                                .fontWeight(.semibold)
                                .foregroundColor(.spAccent)
                                .padding(.horizontal, 40)
                                .padding(.vertical, 12)
                                .background(Color.white)
                                .cornerRadius(12)
                        }
                    }
            }
        }
    }

    // MARK: - Evaluating

    private var evaluatingView: some View {
        VStack(spacing: 20) {
            Spacer()
            SwiftUI.ProgressView()
                .scaleEffect(1.5)
            Text(viewModel.evaluationProgress)
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)
            Text("请稍候，AI 正在全面分析您的回答...")
                .font(.spCaption)
                .foregroundColor(.spTextSecondary.opacity(0.7))
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Result View

    private var resultView: some View {
        MockExamResultView(
            question: viewModel.currentQuestion,
            part: viewModel.currentPart,
            result: viewModel.currentResult ?? FullEvaluateResult(
                transcript: nil, wordCount: nil, sentenceCount: nil,
                pronunciationScore: nil, grammarScore: nil, contentScore: nil,
                overallScore: 0, aiFeedback: nil, revisedAnswer: nil,
                mindMap: nil, keywords: nil, sampleAnswers: nil, revisedAudioB64: nil
            ),
            audioURL: viewModel.currentAudioURL,
            questionIndex: viewModel.currentQuestionIndex + 1,
            totalQuestions: viewModel.totalQuestions,
            onNext: { viewModel.dismissResult() },
            onRedo: { viewModel.redoCurrentQuestion() }
        )
    }

    // MARK: - Section Transition

    private var sectionTransitionView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "arrow.right.circle.fill")
                .font(.system(size: 56))
                .foregroundColor(.spAccent)

            Text("Part \(viewModel.transitionFromPart) 完成")
                .font(.spTitleMedium)
                .foregroundColor(.spTextPrimary)

            Text("即将进入 Part \(viewModel.transitionToPart)")
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)

            Button {
                viewModel.continueAfterTransition()
            } label: {
                Text("继续")
                    .font(.spBodyMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 48)
                    .padding(.vertical, 14)
                    .background(Color.spAccent)
                    .cornerRadius(12)
            }

            Spacer()
        }
    }
}

#Preview {
    NavigationStack {
        MockExamView()
    }
}

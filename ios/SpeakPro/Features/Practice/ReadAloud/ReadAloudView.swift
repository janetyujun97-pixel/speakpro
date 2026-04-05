import SwiftUI

/// 朗读练习视图 — 阅读 → 录音 → 评测结果 → 回放/改进建议
struct ReadAloudView: View {

    @StateObject private var viewModel = ReadAloudViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            switch viewModel.phase {
            case .reading:
                readingPhase
            case .recording:
                recordingPhase
            case .evaluating:
                evaluatingPhase
            case .result:
                resultPhase
            }
        }
        .background(Color.spBackground)
        .navigationTitle("朗读练习")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Phase: Reading（阅读 + 听示范）

    private var readingPhase: some View {
        VStack(spacing: 0) {
            // 进度
            if viewModel.totalArticles > 1 {
                HStack {
                    Text("第 \(viewModel.currentIndex + 1) / \(viewModel.totalArticles) 篇")
                        .font(.spCaption)
                        .foregroundColor(.spTextSecondary)
                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
            }

            // 文章内容
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(viewModel.articleTitle)
                        .font(.spTitleMedium)
                        .foregroundColor(.spTextPrimary)

                    Text(viewModel.articleText)
                        .font(.spBodyLarge)
                        .foregroundColor(.spTextPrimary)
                        .lineSpacing(8)
                }
                .padding(20)
            }
            .background(Color.white)

            Divider()

            // 示范播放 + 语速
            HStack {
                Button {
                    viewModel.playDemo()
                } label: {
                    Label(
                        viewModel.isPlayingDemo ? "暂停示范" : "听示范朗读",
                        systemImage: viewModel.isPlayingDemo ? "pause.circle.fill" : "play.circle.fill"
                    )
                    .font(.spBodyMedium)
                }
                .tint(.spPrimary)

                Spacer()

                HStack(spacing: 4) {
                    Text("语速")
                        .font(.spCaption)
                        .foregroundColor(.spTextSecondary)
                    Picker("", selection: $viewModel.playbackSpeed) {
                        Text("0.5x").tag(0.5)
                        Text("1.0x").tag(1.0)
                        Text("1.5x").tag(1.5)
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 160)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)

            // 错误提示
            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.spCaption)
                    .foregroundColor(.spError)
                    .padding(.horizontal, 20)
            }

            Spacer()

            // 开始录音按钮
            Button {
                viewModel.startRecording()
            } label: {
                Label("开始朗读", systemImage: "mic.fill")
                    .font(.spBodyMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.spAccent)
                    .cornerRadius(12)
            }
            .padding(.horizontal, 40)
            .padding(.bottom, 32)
        }
    }

    // MARK: - Phase: Recording（录音中）

    private var recordingPhase: some View {
        VStack(spacing: 0) {
            // 文章（录音时继续显示供参考）
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(viewModel.articleTitle)
                        .font(.spTitleMedium)
                        .foregroundColor(.spTextPrimary)

                    Text(viewModel.articleText)
                        .font(.spBodyLarge)
                        .foregroundColor(.spTextSecondary)
                        .lineSpacing(8)
                }
                .padding(20)
            }
            .background(Color.white)

            Divider()

            // 录音波形
            VStack(spacing: 8) {
                Text("正在录音...")
                    .font(.spBodyMedium)
                    .foregroundColor(.spAccent)

                WaveformView(data: viewModel.audioRecorder.waveformData, barColor: .spAccent)
                    .frame(height: 40)
                    .padding(.horizontal, 20)
            }
            .padding(.vertical, 12)

            Spacer()

            // 停止录音按钮
            RecordButton(isRecording: .constant(true)) {
                viewModel.stopRecording()
            }
            .padding(.bottom, 16)

            Text("点击停止录音")
                .font(.spCaption)
                .foregroundColor(.spTextSecondary)
                .padding(.bottom, 32)
        }
    }

    // MARK: - Phase: Evaluating（评测中）

    private var evaluatingPhase: some View {
        VStack(spacing: 20) {
            Spacer()
            SwiftUI.ProgressView()
                .scaleEffect(1.5)
            Text("正在评测你的朗读...")
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)
            Spacer()
        }
    }

    // MARK: - Phase: Result（评测结果）

    private var resultPhase: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 20) {
                    // 总分
                    VStack(spacing: 8) {
                        Text("朗读评测结果")
                            .font(.spTitleLarge)
                            .foregroundColor(.spTextPrimary)

                        ScoreRing(
                            score: viewModel.overallScore,
                            color: scoreColor(viewModel.overallScore),
                            lineWidth: 12,
                            size: 120
                        )
                    }
                    .padding(.top, 16)

                    // 三维度评分
                    HStack(spacing: 12) {
                        dimensionCard(title: "发音", score: viewModel.pronunciationScore, color: .spSuccess)
                        dimensionCard(title: "流利度", score: viewModel.fluencyScore, color: .spAccent)
                        dimensionCard(title: "完整度", score: viewModel.completenessScore, color: .spPrimary)
                    }
                    .padding(.horizontal, 16)

                    // 录音回放
                    VStack(alignment: .leading, spacing: 12) {
                        Text("你的录音")
                            .font(.spTitleSmall)
                            .foregroundColor(.spTextPrimary)

                        HStack {
                            Button {
                                viewModel.playStudentRecording()
                            } label: {
                                HStack(spacing: 8) {
                                    Image(systemName: viewModel.isPlayingStudent ? "stop.fill" : "play.fill")
                                        .font(.system(size: 16))
                                    Text(viewModel.isPlayingStudent ? "停止" : "播放录音")
                                        .font(.spBodyMedium)
                                }
                                .foregroundColor(.white)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(Color.spAccent)
                                .cornerRadius(10)
                            }

                            Spacer()

                            Button {
                                viewModel.playDemo()
                            } label: {
                                HStack(spacing: 8) {
                                    Image(systemName: viewModel.isPlayingDemo ? "stop.fill" : "play.fill")
                                        .font(.system(size: 16))
                                    Text(viewModel.isPlayingDemo ? "停止" : "听示范")
                                        .font(.spBodyMedium)
                                }
                                .foregroundColor(.spPrimary)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(Color.spPrimary.opacity(0.1))
                                .cornerRadius(10)
                            }
                        }

                        // 录音波形
                        let waveform = viewModel.audioRecorder.waveformData
                        if !waveform.isEmpty {
                            WaveformView(data: waveform, barColor: .spAccent)
                                .frame(height: 30)
                        }
                    }
                    .padding(16)
                    .background(Color.white)
                    .cornerRadius(12)
                    .padding(.horizontal, 16)

                    // AI 反馈
                    if let feedback = viewModel.aiFeedback, !feedback.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("改进建议")
                                .font(.spTitleSmall)
                                .foregroundColor(.spTextPrimary)

                            RichFeedbackView(text: feedback)
                        }
                        .padding(16)
                        .background(Color.white)
                        .cornerRadius(12)
                        .padding(.horizontal, 16)
                    }

                    // 自动生成的建议
                    VStack(alignment: .leading, spacing: 8) {
                        Text("练习提示")
                            .font(.spTitleSmall)
                            .foregroundColor(.spTextPrimary)

                        ForEach(generateTips(), id: \.self) { tip in
                            HStack(alignment: .top, spacing: 8) {
                                Image(systemName: "lightbulb.fill")
                                    .foregroundColor(.spWarning)
                                    .font(.caption)
                                    .padding(.top, 2)
                                Text(tip)
                                    .font(.spBodySmall)
                                    .foregroundColor(.spTextSecondary)
                            }
                        }
                    }
                    .padding(16)
                    .background(Color.white)
                    .cornerRadius(12)
                    .padding(.horizontal, 16)

                    // 错误提示
                    if let error = viewModel.errorMessage {
                        Text(error)
                            .font(.spCaption)
                            .foregroundColor(.spError)
                            .padding(.horizontal, 16)
                    }
                }
                .padding(.bottom, 16)
            }

            // 底部操作
            VStack(spacing: 16) {
                // 主操作按钮
                if !viewModel.isLastArticle {
                    Button {
                        viewModel.nextArticle()
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "forward.fill")
                                .font(.system(size: 14))
                            Text("下一篇文章")
                                .font(.spBodyMedium)
                                .fontWeight(.semibold)
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.spAccent)
                        .cornerRadius(12)
                    }
                }

                // 次要操作
                HStack(spacing: 12) {
                    Button {
                        viewModel.retryRecording()
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.counterclockwise")
                                .font(.system(size: 13))
                            Text("重新朗读")
                                .font(.spBodySmall)
                        }
                        .foregroundColor(.spAccent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color.spAccent.opacity(0.1))
                        .cornerRadius(10)
                    }

                    Button {
                        dismiss()
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "xmark")
                                .font(.system(size: 13))
                            Text("结束练习")
                                .font(.spBodySmall)
                        }
                        .foregroundColor(.spTextSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color(.systemGray6))
                        .cornerRadius(10)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(Color.white)
        }
    }

    // MARK: - Components

    private func dimensionCard(title: String, score: Double, color: Color) -> some View {
        VStack(spacing: 8) {
            ScoreRing(score: score, color: color, lineWidth: 6, size: 60)
            Text(title)
                .font(.spCaption)
                .foregroundColor(.spTextSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color.white)
        .cornerRadius(12)
    }

    private func scoreColor(_ score: Double) -> Color {
        if score >= 80 { return .spSuccess }
        if score >= 60 { return .spWarning }
        return .spError
    }

    private func generateTips() -> [String] {
        var tips: [String] = []
        if viewModel.pronunciationScore < 70 {
            tips.append("发音准确度需要提升，建议先听示范朗读，注意每个单词的重音和元音发音。")
        }
        if viewModel.fluencyScore < 70 {
            tips.append("朗读流畅度不够，尝试减少停顿，注意句子之间的自然衔接和连读。")
        }
        if viewModel.completenessScore < 70 {
            tips.append("朗读完整度不足，确保每个单词都清晰读出，不要跳过或吞掉词语。")
        }
        if tips.isEmpty {
            tips.append("表现不错！可以尝试提高语速或挑战更长的段落来进一步提升。")
        }
        return tips
    }
}

#Preview {
    NavigationStack {
        ReadAloudView()
    }
}

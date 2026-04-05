import SwiftUI

/// 跟读练习视图 — 完整流程：听参考音 → 录音 → 评测 → 查看结果
struct FollowReadView: View {

    @StateObject private var viewModel = FollowReadViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showReport = false

    var body: some View {
        VStack(spacing: 0) {
            // MARK: - 进度指示
            progressHeader

            // MARK: - 参考文本
            referenceTextSection

            Divider()

            // MARK: - 根据状态显示不同界面
            switch viewModel.phase {
            case .ready:
                readyPhaseView
            case .listening:
                listeningPhaseView
            case .recording:
                recordingPhaseView
            case .evaluating:
                evaluatingPhaseView
            case .result:
                resultPhaseView
            }

            Spacer()

            // MARK: - 底部操作按钮
            bottomActions

            // MARK: - 错误提示
            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.spCaption)
                    .foregroundColor(.spError)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 8)
            }
        }
        .background(Color.spBackground)
        .navigationTitle("跟读练习")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showReport) {
            FollowReadReportView(scores: viewModel.scoreHistory, onDismiss: {
                showReport = false
                dismiss()
            })
        }
    }

    // MARK: - Progress Header

    private var progressHeader: some View {
        HStack {
            Text("第 \(viewModel.currentSentenceIndex + 1) / \(viewModel.totalSentences) 句")
                .font(.spCaption)
                .foregroundColor(.spTextSecondary)
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
    }

    // MARK: - Reference Text

    private var referenceTextSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("参考句子")
                .font(.spCaption)
                .foregroundColor(.spTextSecondary)

            Text(viewModel.currentSentence)
                .font(.spBodyLarge)
                .foregroundColor(.spTextPrimary)
                .lineSpacing(6)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.white)
    }

    // MARK: - Phase: Ready（准备开始）

    private var readyPhaseView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "headphones")
                .font(.system(size: 48))
                .foregroundColor(.spSuccess)

            Text("点击下方按钮\n先听一遍标准发音")
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)
                .multilineTextAlignment(.center)

            Button {
                viewModel.playReferenceAndTransition()
            } label: {
                Label("播放参考音", systemImage: "speaker.wave.2.fill")
                    .font(.spBodyMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.spSuccess)
                    .cornerRadius(12)
            }
            .padding(.horizontal, 40)

            Spacer()
        }
        .padding(20)
    }

    // MARK: - Phase: Listening（正在播放参考音）

    private var listeningPhaseView: some View {
        VStack(spacing: 20) {
            Spacer()

            // 参考波形
            VStack(alignment: .leading, spacing: 4) {
                Text("参考音频")
                    .font(.spCaption)
                    .foregroundColor(.spSuccess)
                WaveformView(data: viewModel.referenceWaveform, barColor: .spSuccess)
                    .frame(height: 40)
            }
            .padding(.horizontal, 20)

            // 播放动画
            HStack(spacing: 4) {
                ForEach(0..<5, id: \.self) { i in
                    Capsule()
                        .fill(Color.spSuccess)
                        .frame(width: 4, height: CGFloat([16, 24, 32, 24, 16][i]))
                        .animation(
                            .easeInOut(duration: 0.5)
                            .repeatForever(autoreverses: true)
                            .delay(Double(i) * 0.1),
                            value: viewModel.isPlayingReference
                        )
                }
            }

            Text("正在播放参考音...")
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)

            Spacer()
        }
    }

    // MARK: - Phase: Recording（录音阶段）

    private var recordingPhaseView: some View {
        VStack(spacing: 20) {
            Spacer()

            if viewModel.isRecording {
                // 正在录音
                VStack(alignment: .leading, spacing: 4) {
                    Text("你的录音")
                        .font(.spCaption)
                        .foregroundColor(.spAccent)
                    WaveformView(data: viewModel.audioRecorder.waveformData, barColor: .spAccent)
                        .frame(height: 40)
                }
                .padding(.horizontal, 20)

                RecordButton(isRecording: .constant(true)) {
                    viewModel.stopRecording()
                }

                Text("点击停止录音")
                    .font(.spCaption)
                    .foregroundColor(.spTextSecondary)
            } else {
                // 等待开始录音
                Image(systemName: "mic.fill")
                    .font(.system(size: 48))
                    .foregroundColor(.spAccent)

                Text("参考音播放完毕\n点击下方按钮开始录音")
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextSecondary)
                    .multilineTextAlignment(.center)

                Button {
                    viewModel.startRecording()
                } label: {
                    Label("开始录音", systemImage: "mic.fill")
                        .font(.spBodyMedium)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.spAccent)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 40)
            }

            Spacer()
        }
    }

    // MARK: - Phase: Evaluating（评测中）

    private var evaluatingPhaseView: some View {
        VStack(spacing: 20) {
            Spacer()

            SwiftUI.ProgressView()
                .scaleEffect(1.5)
                .tint(.spAccent)

            Text("正在评测你的发音...")
                .font(.spBodyMedium)
                .foregroundColor(.spTextSecondary)

            Spacer()
        }
    }

    // MARK: - Phase: Result（评测结果）

    private var resultPhaseView: some View {
        ScrollView {
            VStack(spacing: 16) {
                // 参考音 — 可播放
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("参考")
                            .font(.spCaption)
                            .foregroundColor(.spSuccess)
                        Spacer()
                        Button {
                            viewModel.playReference()
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: viewModel.isPlayingReference ? "stop.fill" : "play.fill")
                                    .font(.system(size: 12))
                                Text(viewModel.isPlayingReference ? "停止" : "播放")
                                    .font(.spCaption)
                            }
                            .foregroundColor(.spSuccess)
                        }
                    }
                    WaveformView(data: viewModel.referenceWaveform, barColor: .spSuccess)
                        .frame(height: 30)
                }
                .padding(16)
                .background(Color.white)
                .cornerRadius(12)
                .padding(.horizontal, 16)

                // 我的录音 — 可播放
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("你的录音")
                            .font(.spCaption)
                            .foregroundColor(.spAccent)
                        Spacer()
                        if viewModel.lastRecordingURL != nil {
                            Button {
                                viewModel.playStudentRecording()
                            } label: {
                                HStack(spacing: 4) {
                                    Image(systemName: viewModel.isPlayingStudent ? "stop.fill" : "play.fill")
                                        .font(.system(size: 12))
                                    Text(viewModel.isPlayingStudent ? "停止" : "播放")
                                        .font(.spCaption)
                                }
                                .foregroundColor(.spAccent)
                            }
                        }
                    }
                    // 使用 audioRecorder 的实时波形数据（更可靠）
                    let waveform = viewModel.studentWaveform.isEmpty
                        ? viewModel.audioRecorder.waveformData
                        : viewModel.studentWaveform
                    if waveform.isEmpty {
                        HStack {
                            Spacer()
                            Text("（无波形数据）")
                                .font(.spCaption)
                                .foregroundColor(.spTextSecondary)
                            Spacer()
                        }
                        .frame(height: 30)
                    } else {
                        WaveformView(data: waveform, barColor: .spAccent)
                            .frame(height: 30)
                    }
                }
                .padding(16)
                .background(Color.white)
                .cornerRadius(12)
                .padding(.horizontal, 16)

                // 评分卡片
                HStack(spacing: 12) {
                    scoreCard(title: "发音", score: viewModel.pronunciationScore, color: .spSuccess)
                    scoreCard(title: "语调", score: viewModel.intonationScore, color: .spAccent)
                    scoreCard(title: "流利度", score: viewModel.fluencyScore, color: .spPrimary)
                }
                .padding(.horizontal, 16)

                // 音素纠正
                if !viewModel.phonemeErrors.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("发音纠正建议")
                            .font(.spTitleSmall)
                            .foregroundColor(.spTextPrimary)

                        ForEach(viewModel.phonemeErrors, id: \.self) { error in
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.circle.fill")
                                    .foregroundColor(.spWarning)
                                    .font(.spBodySmall)
                                Text(error)
                                    .font(.spBodySmall)
                                    .foregroundColor(.spTextSecondary)
                            }
                        }
                    }
                    .padding(16)
                    .background(Color.white)
                    .cornerRadius(12)
                    .padding(.horizontal, 16)
                }
            }
        }
    }

    // MARK: - Score Card

    private func scoreCard(title: String, score: Double, color: Color) -> some View {
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

    // MARK: - Bottom Actions

    private var bottomActions: some View {
        Group {
            switch viewModel.phase {
            case .ready:
                EmptyView()

            case .listening:
                EmptyView()

            case .recording:
                EmptyView()

            case .evaluating:
                EmptyView()

            case .result:
                VStack(spacing: 12) {
                    HStack(spacing: 16) {
                        // 再听一次
                        Button {
                            viewModel.playReference()
                        } label: {
                            Label("再听一次", systemImage: "speaker.wave.2.fill")
                                .font(.spBodyMedium)
                        }
                        .buttonStyle(.bordered)
                        .tint(.spPrimary)
                        .disabled(viewModel.isPlayingReference)

                        // 重录
                        Button {
                            viewModel.retryRecording()
                        } label: {
                            Label("重录", systemImage: "arrow.counterclockwise")
                                .font(.spBodyMedium)
                        }
                        .buttonStyle(.bordered)
                        .tint(.spAccent)

                        if !viewModel.isCompleted {
                            // 下一句
                            Button {
                                viewModel.nextSentence()
                            } label: {
                                Label("下一句", systemImage: "forward.fill")
                                    .font(.spBodyMedium)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.spAccent)
                        }
                    }

                    // 全部完成后显示"查看报告"和"结束练习"
                    if viewModel.isCompleted {
                        Button {
                            showReport = true
                        } label: {
                            Label("查看完整报告", systemImage: "doc.text.magnifyingglass")
                                .font(.spBodyMedium)
                                .fontWeight(.semibold)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.spAccent)
                                .cornerRadius(12)
                        }

                        Button {
                            dismiss()
                        } label: {
                            Text("结束练习")
                                .font(.spBodyMedium)
                                .foregroundColor(.spTextSecondary)
                        }
                    }
                }
                .padding(20)
                .background(Color.white)
            }
        }
    }
}

#Preview {
    NavigationStack {
        FollowReadView()
    }
}

import SwiftUI

/// 朗读练习视图
struct ReadAloudView: View {

    @StateObject private var viewModel = ReadAloudViewModel()

    var body: some View {
        VStack(spacing: 0) {

            // MARK: - 文本显示区域
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(viewModel.articleTitle)
                        .font(.spTitleMedium)
                        .foregroundColor(.spTextPrimary)

                    Text(viewModel.articleText)
                        .font(.spBodyLarge)
                        .foregroundColor(.spTextPrimary)
                        .lineSpacing(8)
                        // TODO: 高亮当前朗读到的句子
                }
                .padding(20)
            }
            .background(Color.white)

            Divider()

            // MARK: - TTS 示范播放
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

                // 语速调节
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

            // MARK: - 评分结果（录音后显示）
            if viewModel.hasScore {
                HStack(spacing: 16) {
                    ScoreRing(score: viewModel.overallScore, color: .spAccent, lineWidth: 6, size: 60)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("综合评分")
                            .font(.spTitleSmall)
                        Text("发音 \(Int(viewModel.pronunciationScore)) | 流利 \(Int(viewModel.fluencyScore)) | 完整度 \(Int(viewModel.completenessScore))")
                            .font(.spCaption)
                            .foregroundColor(.spTextSecondary)
                    }
                    Spacer()
                }
                .padding(16)
                .background(Color.white)
                .cornerRadius(12)
                .padding(.horizontal, 16)
            }

            Spacer()

            // MARK: - 录音按钮
            RecordButton(isRecording: $viewModel.isRecording) {
                if viewModel.isRecording {
                    viewModel.stopRecording()
                } else {
                    viewModel.startRecording()
                }
            }
            .padding(.bottom, 32)
        }
        .background(Color.spBackground)
        .navigationTitle("朗读练习")
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        ReadAloudView()
    }
}

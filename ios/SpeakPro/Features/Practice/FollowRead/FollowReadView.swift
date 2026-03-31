import SwiftUI

/// 跟读练习视图
struct FollowReadView: View {

    @StateObject private var viewModel = FollowReadViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {

            // MARK: - 参考文本
            referenceTextSection

            Divider()

            // MARK: - 双波形对比
            waveformComparisonSection

            // MARK: - 评分卡片
            scoreCardsSection

            // MARK: - 音素纠正建议
            if !viewModel.phonemeErrors.isEmpty {
                phonemeCorrectionSection
            }

            Spacer()

            // MARK: - 底部操作按钮
            actionButtonsSection
        }
        .background(Color.spBackground)
        .navigationTitle("跟读练习")
        .navigationBarTitleDisplayMode(.inline)
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

            // TODO: 高亮当前正在播放的词
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.white)
    }

    // MARK: - Waveform Comparison

    private var waveformComparisonSection: some View {
        VStack(spacing: 12) {
            // 参考音频波形
            VStack(alignment: .leading, spacing: 4) {
                Text("参考")
                    .font(.spCaption)
                    .foregroundColor(.spSuccess)
                WaveformView(data: viewModel.referenceWaveform, barColor: .spSuccess)
                    .frame(height: 30)
            }

            // 学生录音波形
            VStack(alignment: .leading, spacing: 4) {
                Text("你的录音")
                    .font(.spCaption)
                    .foregroundColor(.spAccent)
                WaveformView(data: viewModel.studentWaveform, barColor: .spAccent)
                    .frame(height: 30)
            }
        }
        .padding(16)
    }

    // MARK: - Score Cards

    private var scoreCardsSection: some View {
        HStack(spacing: 12) {
            scoreCard(title: "发音", score: viewModel.pronunciationScore, color: .spSuccess)
            scoreCard(title: "语调", score: viewModel.intonationScore, color: .spAccent)
            scoreCard(title: "流利度", score: viewModel.fluencyScore, color: .spPrimary)
        }
        .padding(.horizontal, 16)
    }

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

    // MARK: - Phoneme Correction

    private var phonemeCorrectionSection: some View {
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
        .padding(.top, 12)
    }

    // MARK: - Action Buttons

    private var actionButtonsSection: some View {
        HStack(spacing: 16) {
            // 再听一次
            Button {
                // TODO: 重新播放参考音频
            } label: {
                Label("再听一次", systemImage: "speaker.wave.2.fill")
                    .font(.spBodyMedium)
            }
            .buttonStyle(.bordered)
            .tint(.spPrimary)

            // 重新录音
            Button {
                // TODO: 重新开始录音
            } label: {
                Label("重录", systemImage: "arrow.counterclockwise")
                    .font(.spBodyMedium)
            }
            .buttonStyle(.bordered)
            .tint(.spAccent)

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
        .padding(20)
        .background(Color.white)
    }
}

#Preview {
    NavigationStack {
        FollowReadView()
    }
}

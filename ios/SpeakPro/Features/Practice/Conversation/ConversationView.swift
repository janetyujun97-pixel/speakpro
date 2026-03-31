import SwiftUI

/// AI 对话练习视图
struct ConversationView: View {

    @StateObject private var viewModel = ConversationViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {

            // MARK: - 顶部：考试类型 + 计时器
            headerBar

            // MARK: - 中间：聊天气泡
            chatArea

            // MARK: - 实时反馈面板
            if !viewModel.scores.isEmpty {
                feedbackPanel
            }

            // MARK: - 底部：波形 + 录音按钮
            recordingArea
        }
        .background(Color.spBackground)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("结束") {
                    viewModel.endConversation()
                    dismiss()
                }
                .foregroundColor(.spAccent)
            }
        }
    }

    // MARK: - Header Bar

    private var headerBar: some View {
        HStack {
            // 考试类型标签
            Text("雅思 Part 2")
                .font(.spTitleSmall)
                .foregroundColor(.spPrimary)

            Spacer()

            // 倒计时
            HStack(spacing: 4) {
                Image(systemName: "timer")
                    .font(.spBodySmall)
                Text(viewModel.formattedRemainingTime)
                    .font(.spBodyMedium)
                    .monospacedDigit()
            }
            .foregroundColor(viewModel.remainingTime < 60 ? .spError : .spTextSecondary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.white)
    }

    // MARK: - Chat Area

    private var chatArea: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.messages) { message in
                        ChatBubbleView(
                            isExaminer: message.isExaminer,
                            text: message.text
                        )
                        .id(message.id)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
            .onChange(of: viewModel.messages.count) { _, _ in
                if let lastID = viewModel.messages.last?.id {
                    withAnimation {
                        proxy.scrollTo(lastID, anchor: .bottom)
                    }
                }
            }
        }
    }

    // MARK: - Feedback Panel

    private var feedbackPanel: some View {
        HStack(spacing: 16) {
            ForEach(Array(viewModel.scores), id: \.key) { key, value in
                VStack(spacing: 4) {
                    Text("\(Int(value))")
                        .font(.spTitleSmall)
                        .foregroundColor(.spAccent)
                    Text(key)
                        .font(.spCaption)
                        .foregroundColor(.spTextSecondary)
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 8)
        .background(Color.white.opacity(0.9))
    }

    // MARK: - Recording Area

    private var recordingArea: some View {
        VStack(spacing: 12) {
            // 波形可视化
            WaveformView(data: viewModel.isRecording
                ? viewModel.audioRecorder.waveformData
                : [])
                .frame(height: 40)
                .padding(.horizontal, 20)

            // 录音按钮
            RecordButton(isRecording: $viewModel.isRecording) {
                if viewModel.isRecording {
                    viewModel.stopAndSendAudio()
                } else {
                    viewModel.startRecording()
                }
            }
            .padding(.bottom, 16)
        }
        .padding(.top, 8)
        .background(Color.white)
    }
}

#Preview {
    NavigationStack {
        ConversationView()
    }
}

import SwiftUI

/// AI 对话练习视图
struct ConversationView: View {

    @StateObject private var viewModel = ConversationViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {

            // MARK: - 顶部：考试类型 + 状态 + 计时器
            headerBar

            // MARK: - 中间：聊天气泡
            chatArea

            // MARK: - 实时反馈面板
            if !viewModel.scores.isEmpty {
                feedbackPanel
            }

            // MARK: - 状态提示
            if !viewModel.processingStatus.isEmpty {
                Text(viewModel.processingStatus)
                    .font(.spCaption)
                    .foregroundColor(.spTextSecondary)
                    .padding(.vertical, 6)
            }

            // MARK: - 错误提示
            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.spCaption)
                    .foregroundColor(.spError)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 6)
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
        .task {
            // ⚡ 进入页面自动连接 WebSocket
            viewModel.startConversation()
        }
    }

    // MARK: - Header Bar

    private var headerBar: some View {
        HStack {
            // 考试类型
            Text("雅思 Part 2")
                .font(.spTitleSmall)
                .foregroundColor(.spPrimary)

            Spacer()

            // 连接状态
            if viewModel.isConnecting {
                HStack(spacing: 4) {
                    SwiftUI.ProgressView()
                        .scaleEffect(0.7)
                    Text("连接中...")
                        .font(.spCaption)
                        .foregroundColor(.spTextSecondary)
                }
            } else if viewModel.isConnected {
                // 倒计时
                HStack(spacing: 4) {
                    Image(systemName: "timer")
                        .font(.spBodySmall)
                    Text(viewModel.formattedRemainingTime)
                        .font(.spBodyMedium)
                        .monospacedDigit()
                }
                .foregroundColor(viewModel.remainingTime < 60 ? .spError : .spTextSecondary)
            } else {
                Button("重新连接") {
                    viewModel.startConversation()
                }
                .font(.spCaption)
                .foregroundColor(.spAccent)
            }
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
                        if message.isVoiceMessage {
                            // 语音消息（用户 + 考官通用）
                            voiceMessageBubble(message: message)
                                .id(message.id)
                        } else {
                            // 纯文字消息
                            ChatBubbleView(isExaminer: message.isExaminer, text: message.text)
                                .id(message.id)
                        }
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

    // MARK: - Voice Message Bubble（点击播放 + 长按菜单）

    private func voiceMessageBubble(message: ChatMessage) -> some View {
        let isExaminer = message.isExaminer
        let bubbleColor = isExaminer ? Color.spPrimary : Color.spAccent
        let alignment: HorizontalAlignment = isExaminer ? .leading : .trailing

        return HStack(alignment: .top, spacing: 8) {
            if isExaminer {
                // 考官头像（左）
                Circle()
                    .fill(Color.spPrimary.opacity(0.1))
                    .frame(width: 32, height: 32)
                    .overlay(
                        Image(systemName: "person.badge.shield.checkmark.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.spPrimary)
                    )
            } else {
                Spacer()
            }

            VStack(alignment: alignment, spacing: 4) {
                // 语音气泡
                Button {
                    viewModel.playAudio(for: message)
                } label: {
                    HStack(spacing: 8) {
                        if isExaminer {
                            // 考官：波形在左，播放按钮在右
                            waveformBars(color: .white)
                            durationLabel(message: message)
                            playIcon(message: message)
                        } else {
                            // 用户：播放按钮在左
                            playIcon(message: message)
                            durationLabel(message: message)
                            waveformBars(color: .white)
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(bubbleColor)
                    .cornerRadius(16)
                }
                .buttonStyle(.plain)
                .contextMenu {
                    // 转文字
                    if message.transcribedText == nil {
                        Button {
                            viewModel.convertToText(messageId: message.id)
                        } label: {
                            Label("转文字", systemImage: "text.bubble")
                        }
                    }

                    // 删除
                    Button(role: .destructive) {
                        viewModel.deleteMessage(id: message.id)
                    } label: {
                        Label("删除", systemImage: "trash")
                    }
                }

                // 转写文字
                if let transcript = message.transcribedText {
                    Text(transcript)
                        .font(.spCaption)
                        .foregroundColor(.spTextSecondary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.white)
                        .cornerRadius(8)
                        .frame(maxWidth: 220, alignment: isExaminer ? .leading : .trailing)
                }
            }

            if !isExaminer {
                // 用户头像（右）
                Circle()
                    .fill(Color.spAccent.opacity(0.1))
                    .frame(width: 32, height: 32)
                    .overlay(
                        Image(systemName: "person.fill")
                            .font(.system(size: 14))
                            .foregroundColor(.spAccent)
                    )
            } else {
                Spacer()
            }
        }
    }

    // MARK: - Voice Bubble Components

    @ViewBuilder
    private func playIcon(message: ChatMessage) -> some View {
        if viewModel.playingMessageId == message.id {
            HStack(spacing: 2) {
                ForEach(0..<3, id: \.self) { i in
                    Capsule()
                        .fill(Color.white)
                        .frame(width: 3, height: CGFloat([12, 18, 10][i]))
                }
            }
        } else {
            Image(systemName: "play.fill")
                .font(.system(size: 14))
                .foregroundColor(.white)
        }
    }

    @ViewBuilder
    private func durationLabel(message: ChatMessage) -> some View {
        if let dur = message.duration, dur > 0 {
            Text("\(Int(dur))\"")
                .font(.spBodySmall)
                .foregroundColor(.white.opacity(0.9))
        }
    }

    private func waveformBars(color: Color) -> some View {
        HStack(spacing: 1.5) {
            ForEach(0..<8, id: \.self) { i in
                Capsule()
                    .fill(color.opacity(0.6))
                    .frame(width: 2, height: CGFloat([8, 14, 10, 16, 12, 18, 9, 13][i]))
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
            // 波形
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
            .disabled(!viewModel.isConnected && !viewModel.isConnecting)
            .opacity(viewModel.isConnected ? 1.0 : 0.5)
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

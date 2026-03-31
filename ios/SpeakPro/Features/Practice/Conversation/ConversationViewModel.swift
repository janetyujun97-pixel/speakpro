import Foundation
import Combine

/// 对话消息模型
struct ChatMessage: Identifiable {
    let id = UUID()
    let text: String
    let isExaminer: Bool
    let timestamp: Date

    init(text: String, isExaminer: Bool) {
        self.text = text
        self.isExaminer = isExaminer
        self.timestamp = Date()
    }
}

/// AI 对话练习视图模型
final class ConversationViewModel: ObservableObject {

    // MARK: - Published

    @Published var messages: [ChatMessage] = []
    @Published var isRecording = false
    @Published var scores: [String: Double] = [:]
    @Published var remainingTime: TimeInterval = 120  // 默认 2 分钟

    // MARK: - Dependencies

    let audioRecorder = AudioRecorder()
    private let wsManager = WebSocketManager()
    private var timer: Timer?
    private var cancellables = Set<AnyCancellable>()

    var formattedRemainingTime: String {
        let minutes = Int(remainingTime) / 60
        let seconds = Int(remainingTime) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init() {
        setupWebSocketHandlers()

        // 添加考官的开场白
        messages.append(ChatMessage(
            text: "Good afternoon. My name is the AI examiner. Can you tell me your full name please?",
            isExaminer: true
        ))
    }

    // MARK: - WebSocket Setup

    private func setupWebSocketHandlers() {
        wsManager.onMessage = { [weak self] message in
            switch message {
            case .text(let text):
                // TODO: 解析服务器返回的 JSON，提取考官回复和评分
                self?.handleServerMessage(text)
            case .data:
                break
            }
        }
    }

    private func handleServerMessage(_ text: String) {
        // TODO: 解析 JSON 消息
        // 暂时直接作为考官回复
        let reply = ChatMessage(text: text, isExaminer: true)
        messages.append(reply)
    }

    // MARK: - Conversation Lifecycle

    func startConversation() {
        // TODO: 调用 API 创建 session，获取 sessionId
        let sessionId = UUID().uuidString
        wsManager.connect(sessionId: sessionId)
        startTimer()
    }

    func endConversation() {
        stopTimer()
        wsManager.disconnect()
        if isRecording {
            _ = audioRecorder.stopRecording()
            isRecording = false
        }
    }

    // MARK: - Recording

    func startRecording() {
        do {
            try audioRecorder.startRecording()
            isRecording = true
        } catch {
            print("[ConversationVM] 录音启动失败: \(error)")
        }
    }

    func stopAndSendAudio() {
        guard let audioURL = audioRecorder.stopRecording() else { return }
        isRecording = false
        sendAudio(fileURL: audioURL)
    }

    func sendAudio(fileURL: URL) {
        // TODO: 读取音频文件数据，通过 WebSocket 发送给服务器
        guard let data = try? Data(contentsOf: fileURL) else { return }
        wsManager.send(data: data)

        // 添加用户占位消息
        messages.append(ChatMessage(text: "[语音消息]", isExaminer: false))

        // TODO: 模拟评分反馈（实际应从服务器接收）
        scores = [
            "发音": 7.5,
            "流利度": 6.0,
            "语法": 7.0
        ]
    }

    // MARK: - Timer

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            if self.remainingTime > 0 {
                self.remainingTime -= 1
            } else {
                self.endConversation()
            }
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }

    deinit {
        stopTimer()
    }
}

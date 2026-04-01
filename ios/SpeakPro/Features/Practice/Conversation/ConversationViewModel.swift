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

/// AI 对话练习视图模型 —— 通过 WebSocket 与 Go 服务实时交互
final class ConversationViewModel: ObservableObject {

    // MARK: - Published

    @Published var messages: [ChatMessage] = []
    @Published var isRecording = false
    @Published var scores: [String: Double] = [:]
    @Published var remainingTime: TimeInterval = 120
    @Published var isConnecting = false
    @Published var currentTranscript: String = ""
    @Published var processingStatus: String = ""
    @Published var errorMessage: String?

    // MARK: - Configuration

    var examType: String = "IELTS"
    var section: String = "Part1"

    // MARK: - Dependencies

    let audioRecorder = AudioRecorder()
    private let wsManager = WebSocketManager()
    private var timer: Timer?
    private var cancellables = Set<AnyCancellable>()
    private var sessionId: String?
    private var audioSequence = 0

    var formattedRemainingTime: String {
        let minutes = Int(remainingTime) / 60
        let seconds = Int(remainingTime) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init() {
        setupWebSocketHandlers()
    }

    // MARK: - WebSocket Setup

    private func setupWebSocketHandlers() {
        wsManager.onTypedMessage = { [weak self] message in
            self?.handleServerMessage(message)
        }

        wsManager.onDisconnect = { [weak self] error in
            self?.handleDisconnect(error)
        }
    }

    private func handleServerMessage(_ message: WSServerMessage) {
        switch message {
        case .sessionReady(let payload):
            isConnecting = false
            remainingTime = TimeInterval(payload.timeLimitSec)
            // 替换/添加考官开场白
            if messages.isEmpty {
                messages.append(ChatMessage(text: payload.examinerGreeting, isExaminer: true))
            }
            startTimer()

        case .transcript(let payload):
            currentTranscript = payload.text
            if payload.isFinal {
                // 替换 "[语音消息]" 占位为真实转写文本
                if let lastIndex = messages.lastIndex(where: { !$0.isExaminer && $0.text == "[语音消息]" }) {
                    messages[lastIndex] = ChatMessage(text: payload.text, isExaminer: false)
                }
                currentTranscript = ""
            }

        case .examiner(let payload):
            messages.append(ChatMessage(text: payload.text, isExaminer: true))
            processingStatus = ""
            // TODO: 如果有 ttsAudioB64，可以自动播放考官语音

        case .scoreUpdate(let payload):
            scores = [:]
            if let pron = payload.pronunciation?.overall {
                scores["发音"] = pron
            }
            if let flu = payload.pronunciation?.fluency {
                scores["流利度"] = flu
            }
            if let gram = payload.grammar?.score {
                scores["语法"] = gram * 10 // 0-10 → 0-100
            }
            scores["总分"] = payload.overall
            processingStatus = ""

        case .error(let payload):
            errorMessage = "\(payload.code): \(payload.message)"
            processingStatus = ""

        case .processing(let payload):
            processingStatus = payload.message

        case .ping:
            // 自动处理 pong（在 WebSocketManager 中）
            break

        case .unknown(let raw):
            print("[ConversationVM] 未知消息: \(raw)")
        }
    }

    private func handleDisconnect(_ error: Error?) {
        if let error = error {
            errorMessage = "连接断开: \(error.localizedDescription)"
        }
        isConnecting = false
    }

    // MARK: - Conversation Lifecycle

    /// 开始对话：创建 session → 连接 WebSocket → 发送 session_init
    func startConversation() {
        isConnecting = true
        errorMessage = nil

        Task { @MainActor in
            do {
                // 1. 通过 NestJS 创建 practice session
                let response: APIResponse<SessionResponse> = try await APIClient.shared.post(
                    Endpoints.Practice.start,
                    body: StartSessionRequest(questionId: nil, mode: "conversation")
                )

                guard let session = response.data else {
                    errorMessage = "创建会话失败"
                    isConnecting = false
                    return
                }

                self.sessionId = session.id

                // 2. 连接 WebSocket
                wsManager.connect(sessionId: session.id)

                // 3. 稍延迟后发送 session_init（等待连接建立）
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
                    guard let self = self else { return }
                    self.wsManager.sendJSON(WSClientMessage(
                        type: .sessionInit,
                        data: SessionInitData(
                            sessionId: session.id,
                            examType: self.examType,
                            section: self.section,
                            mode: "conversation"
                        )
                    ))
                }
            } catch {
                errorMessage = "连接失败: \(error.localizedDescription)"
                isConnecting = false
            }
        }
    }

    func endConversation() {
        stopTimer()
        wsManager.disconnect()
        if isRecording {
            _ = audioRecorder.stopRecording()
            isRecording = false
        }
        processingStatus = ""
    }

    // MARK: - Recording

    func startRecording() {
        guard !isRecording else { return }
        errorMessage = nil
        audioSequence = 0

        // 设置音频流式推送回调
        audioRecorder.onAudioBuffer = { [weak self] int16Data in
            guard let self = self else { return }
            self.audioSequence += 1
            let b64 = int16Data.base64EncodedString()
            self.wsManager.sendJSON(WSClientMessage(
                type: .audioChunk,
                data: AudioChunkData(
                    sequence: self.audioSequence,
                    audioB64: b64,
                    isFinal: false
                )
            ))
        }

        do {
            try audioRecorder.startRecording()
            isRecording = true
        } catch {
            errorMessage = "录音启动失败: \(error.localizedDescription)"
        }
    }

    func stopAndSendAudio() {
        guard isRecording else { return }

        // 停止录音
        _ = audioRecorder.stopRecording()
        isRecording = false
        audioRecorder.onAudioBuffer = nil

        // 添加占位消息
        messages.append(ChatMessage(text: "[语音消息]", isExaminer: false))

        // 发送 audio_complete 信号
        guard let sessionId = sessionId else { return }
        wsManager.sendJSON(WSClientMessage(
            type: .audioComplete,
            data: AudioCompleteData(sessionId: sessionId, referenceText: nil)
        ))

        processingStatus = "正在处理..."
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

// MARK: - API 模型

private struct StartSessionRequest: Encodable {
    let questionId: String?
    let mode: String
}

private struct SessionResponse: Decodable {
    let id: String
    let mode: String
}

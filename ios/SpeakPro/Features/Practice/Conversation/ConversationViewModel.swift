import Foundation
import AVFoundation
import Combine

/// 对话消息模型
struct ChatMessage: Identifiable {
    let id = UUID()
    let text: String
    let isExaminer: Bool
    let timestamp: Date
    var audioURL: URL?          // 语音消息的本地音频文件
    var audioData: Data?        // 语音消息的原始音频数据
    var duration: TimeInterval?  // 语音时长
    var transcribedText: String? // 转文字后的文本

    var isVoiceMessage: Bool { audioURL != nil || audioData != nil }

    init(text: String, isExaminer: Bool, audioURL: URL? = nil, duration: TimeInterval? = nil) {
        self.text = text
        self.isExaminer = isExaminer
        self.timestamp = Date()
        self.audioURL = audioURL
        self.duration = duration
    }
}

/// AI 对话练习视图模型 —— 通过 WebSocket 与 Go 服务实时交互
@MainActor
final class ConversationViewModel: ObservableObject {

    // MARK: - Published

    @Published var messages: [ChatMessage] = []
    @Published var isRecording = false
    @Published var scores: [String: Double] = [:]
    @Published var remainingTime: TimeInterval = 120
    @Published var isConnecting = false
    @Published var isConnected = false
    @Published var currentTranscript: String = ""
    @Published var processingStatus: String = ""
    @Published var errorMessage: String?

    // 语音播放状态
    @Published var playingMessageId: UUID?

    // MARK: - Configuration

    var examType: String = "IELTS"
    var section: String = "Part1"

    // MARK: - Dependencies

    let audioRecorder = AudioRecorder()
    private let wsManager = WebSocketManager()
    private var audioPlayer: AVAudioPlayer?
    private var timer: Timer?
    private var cancellables = Set<AnyCancellable>()
    private var sessionId: String?
    private var audioSequence = 0
    private var recordingStartTime: Date?

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
            Task { @MainActor in
                self?.handleServerMessage(message)
            }
        }

        wsManager.onDisconnect = { [weak self] error in
            Task { @MainActor in
                self?.handleDisconnect(error)
            }
        }
    }

    private func handleServerMessage(_ message: WSServerMessage) {
        switch message {
        case .sessionReady(let payload):
            isConnecting = false
            isConnected = true
            remainingTime = TimeInterval(payload.timeLimitSec)
            if messages.isEmpty {
                // 开场白作为语音消息
                var greetingMsg = ChatMessage(text: payload.examinerGreeting, isExaminer: true)
                if let b64 = payload.greetingTtsB64, !b64.isEmpty,
                   let audioData = Data(base64Encoded: b64) {
                    greetingMsg.audioData = audioData
                    greetingMsg.duration = estimateAudioDuration(dataSize: audioData.count)
                }
                messages.append(greetingMsg)
                // 自动播放开场白
                if greetingMsg.audioData != nil {
                    playAudio(for: greetingMsg)
                }
            }
            startTimer()

        case .transcript(let payload):
            currentTranscript = payload.text
            if payload.isFinal {
                if let lastIndex = messages.lastIndex(where: { !$0.isExaminer && $0.text == "[语音消息]" }) {
                    messages[lastIndex].transcribedText = payload.text
                }
                currentTranscript = ""
            }

        case .examiner(let payload):
            // 考官回复作为语音消息
            var msg = ChatMessage(text: payload.text, isExaminer: true)
            if let b64 = payload.ttsAudioB64, !b64.isEmpty,
               let audioData = Data(base64Encoded: b64) {
                msg.audioData = audioData
                msg.duration = estimateAudioDuration(dataSize: audioData.count)
            }
            messages.append(msg)
            processingStatus = ""
            // 自动播放考官回复
            if msg.audioData != nil {
                playAudio(for: msg)
            }

        case .scoreUpdate(let payload):
            scores = [:]
            if let pron = payload.pronunciation?.overall {
                scores["发音"] = pron
            }
            if let flu = payload.pronunciation?.fluency {
                scores["流利度"] = flu
            }
            if let gram = payload.grammar?.score {
                // 语法评分已统一为 0-100 百分制（兼容旧的 0-10 分制）
                scores["语法"] = gram <= 10 ? gram * 10 : gram
            }
            scores["总分"] = payload.overall
            processingStatus = ""

        case .error(let payload):
            errorMessage = "\(payload.code): \(payload.message)"
            processingStatus = ""

        case .processing(let payload):
            processingStatus = payload.message

        case .ping:
            break

        case .unknown(let raw):
            print("[ConversationVM] 未知消息: \(raw)")
        }
    }

    private func handleDisconnect(_ error: Error?) {
        isConnected = false
        if let error = error {
            errorMessage = "连接断开: \(error.localizedDescription)"
        }
        isConnecting = false
    }

    // MARK: - Conversation Lifecycle

    /// 开始对话：创建 session → 连接 WebSocket → 发送 session_init
    func startConversation() {
        guard !isConnecting && !isConnected else { return }

        // 重置所有状态（支持重新开始对话）
        stopTimer()
        stopAudio()
        messages = []
        scores = [:]
        remainingTime = 120
        currentTranscript = ""
        processingStatus = ""
        isRecording = false
        playingMessageId = nil
        sessionId = nil
        audioSequence = 0

        isConnecting = true
        errorMessage = nil

        Task {
            do {
                // 1. 创建 practice session
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

                // 3. 延迟发送 session_init
                try? await Task.sleep(nanoseconds: 500_000_000)
                wsManager.sendJSON(WSClientMessage(
                    type: .sessionInit,
                    data: SessionInitData(
                        sessionId: session.id,
                        examType: self.examType,
                        section: self.section,
                        mode: "conversation"
                    )
                ))
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
        isConnected = false
        processingStatus = ""
    }

    // MARK: - Recording

    func startRecording() {
        guard !isRecording else { return }
        errorMessage = nil
        audioSequence = 0
        recordingStartTime = Date()

        // 流式音频推送
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

        Task {
            do {
                try await audioRecorder.requestAndStartRecording()
                isRecording = true
            } catch {
                errorMessage = "录音启动失败: \(error.localizedDescription)"
            }
        }
    }

    func stopAndSendAudio() {
        guard isRecording else { return }

        let audioURL = audioRecorder.stopRecording()
        isRecording = false
        audioRecorder.onAudioBuffer = nil

        // 计算录音时长
        let duration = recordingStartTime.map { Date().timeIntervalSince($0) } ?? 0

        // 添加语音消息（带音频文件路径）
        messages.append(ChatMessage(
            text: "[语音消息]",
            isExaminer: false,
            audioURL: audioURL,
            duration: duration
        ))

        // 发送 audio_complete
        guard let sessionId = sessionId else { return }
        wsManager.sendJSON(WSClientMessage(
            type: .audioComplete,
            data: AudioCompleteData(sessionId: sessionId, referenceText: nil)
        ))

        processingStatus = "正在处理..."
    }

    // MARK: - 语音播放

    func playAudio(for message: ChatMessage) {
        // 如果正在播放同一条，停止
        if playingMessageId == message.id {
            stopAudio()
            return
        }

        stopAudio()

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)

            if let audioData = message.audioData {
                // 检测音频格式：MP3 以 0xFF 0xFB 或 "ID3" 开头；WAV 以 "RIFF" 开头
                if audioData.count > 3 && (
                    // MP3 sync word
                    (audioData[0] == 0xFF && (audioData[1] & 0xE0) == 0xE0) ||
                    // ID3 tag
                    (audioData[0] == 0x49 && audioData[1] == 0x44 && audioData[2] == 0x33)
                ) {
                    // MP3 格式（Fish Audio 返回），直接播放
                    audioPlayer = try AVAudioPlayer(data: audioData)
                } else {
                    // PCM 原始数据（讯飞返回），加 WAV 头后播放
                    let wavData = pcmToWAV(pcmData: audioData, sampleRate: 16000, channels: 1, bitsPerSample: 16)
                    audioPlayer = try AVAudioPlayer(data: wavData)
                }
            } else if let url = message.audioURL {
                audioPlayer = try AVAudioPlayer(contentsOf: url)
            } else {
                return
            }

            audioPlayer?.delegate = AudioPlayerDelegateProxy.shared
            AudioPlayerDelegateProxy.shared.onFinish = { [weak self] in
                Task { @MainActor in
                    self?.playingMessageId = nil
                }
            }
            audioPlayer?.play()
            playingMessageId = message.id
        } catch {
            print("[ConversationVM] 播放失败: \(error)")
        }
    }

    /// 估算 PCM 音频时长（16kHz, 16-bit, mono）
    private func estimateAudioDuration(dataSize: Int) -> TimeInterval {
        // 16kHz * 16bit * 1ch = 32000 bytes/sec
        return Double(dataSize) / 32000.0
    }

    /// PCM 原始数据包装为 WAV 格式（浏览器/AVAudioPlayer 需要 WAV 头）
    private func pcmToWAV(pcmData: Data, sampleRate: Int, channels: Int, bitsPerSample: Int) -> Data {
        let byteRate = sampleRate * channels * bitsPerSample / 8
        let blockAlign = channels * bitsPerSample / 8
        let dataSize = pcmData.count
        let fileSize = 36 + dataSize

        var wav = Data()
        wav.append(contentsOf: "RIFF".utf8)
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(fileSize).littleEndian) { Array($0) })
        wav.append(contentsOf: "WAVE".utf8)
        wav.append(contentsOf: "fmt ".utf8)
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(16).littleEndian) { Array($0) }) // subchunk size
        wav.append(contentsOf: withUnsafeBytes(of: UInt16(1).littleEndian) { Array($0) })  // PCM format
        wav.append(contentsOf: withUnsafeBytes(of: UInt16(channels).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(sampleRate).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(byteRate).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt16(blockAlign).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt16(bitsPerSample).littleEndian) { Array($0) })
        wav.append(contentsOf: "data".utf8)
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(dataSize).littleEndian) { Array($0) })
        wav.append(pcmData)
        return wav
    }

    func stopAudio() {
        audioPlayer?.stop()
        audioPlayer = nil
        playingMessageId = nil
    }

    // MARK: - 转文字

    func convertToText(messageId: UUID) {
        guard let index = messages.firstIndex(where: { $0.id == messageId }) else { return }
        if messages[index].transcribedText != nil { return } // 已有转写

        // 考官消息：直接用消息自带的 text 字段（TTS 的原文）
        if messages[index].isExaminer {
            messages[index].transcribedText = messages[index].text
            return
        }

        // 用户消息：如果已经有 ASR 转写结果，直接用
        // (服务端通过 WebSocket 返回的 transcript 消息会自动填充 transcribedText)
        // 如果确实没有，显示提示
        if messages[index].text != "[语音消息]" {
            messages[index].transcribedText = messages[index].text
        } else {
            messages[index].transcribedText = "（语音识别中，请稍候...）"
        }
    }

    // MARK: - 删除消息

    func deleteMessage(id: UUID) {
        messages.removeAll { $0.id == id }
    }

    // MARK: - Timer

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self = self else { return }
                if self.remainingTime > 0 {
                    self.remainingTime -= 1
                } else {
                    self.endConversation()
                }
            }
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }

    deinit {
        timer?.invalidate()
    }
}

// MARK: - AVAudioPlayerDelegate proxy

private class AudioPlayerDelegateProxy: NSObject, AVAudioPlayerDelegate {
    static let shared = AudioPlayerDelegateProxy()
    var onFinish: (() -> Void)?

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        onFinish?()
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

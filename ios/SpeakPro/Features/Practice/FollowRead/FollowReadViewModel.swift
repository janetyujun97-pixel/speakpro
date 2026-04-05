import Foundation
import Combine

/// 跟读练习视图模型 —— TTS 参考音播放 + 录音 + ISE 评测
final class FollowReadViewModel: ObservableObject {

    /// 跟读练习的阶段
    enum Phase {
        case ready       // 初始：准备播放参考音
        case listening   // 播放参考音中
        case recording   // 录音中
        case evaluating  // 评测中
        case result      // 显示评测结果
    }

    private var cancellables = Set<AnyCancellable>()

    // MARK: - Published

    @Published var phase: Phase = .ready

    @Published var currentSentence: String = ""
    @Published var currentSentenceIndex: Int = 0
    @Published var totalSentences: Int = 0

    @Published var pronunciationScore: Double = 0
    @Published var intonationScore: Double = 0
    @Published var fluencyScore: Double = 0
    @Published var hasScore = false

    @Published var phonemeErrors: [String] = []
    @Published var isCompleted = false  // 所有句子完成

    @Published var referenceWaveform: [Float] = []
    @Published var studentWaveform: [Float] = []

    // 每句评分历史（用于最终报告）
    struct SentenceScore {
        let sentence: String
        let pronunciation: Double
        let intonation: Double
        let fluency: Double
    }
    @Published var scoreHistory: [SentenceScore] = []

    @Published var isRecording = false
    @Published var isPlayingReference = false
    @Published var isPlayingStudent = false
    @Published var isEvaluating = false
    @Published var errorMessage: String?
    @Published var lastRecordingURL: URL?

    // MARK: - Dependencies

    let audioRecorder = AudioRecorder()
    private let audioPlayer = AudioPlayer()
    private var sentences: [SentenceItem] = []
    private var sessionId: String?

    init() {
        loadSentences()
    }

    // MARK: - 加载句子列表

    func loadSentences() {
        Task { @MainActor in
            do {
                let response: APIResponse<QuestionListResponse> = try await APIClient.shared.get(
                    Endpoints.Questions.list,
                    queryItems: [
                        URLQueryItem(name: "exam_type", value: "IELTS"),
                        URLQueryItem(name: "section", value: "FollowRead"),
                        URLQueryItem(name: "limit", value: "10"),
                    ]
                )

                if let questions = response.data?.items, !questions.isEmpty {
                    sentences = questions.map { SentenceItem(id: $0.id, text: $0.promptText) }
                } else {
                    // 使用默认句子
                    sentences = Self.defaultSentences
                }
                totalSentences = sentences.count
                loadCurrentSentence()
            } catch {
                sentences = Self.defaultSentences
                totalSentences = sentences.count
                loadCurrentSentence()
                print("[FollowReadVM] 句子加载失败: \(error)")
            }
        }
    }

    // MARK: - 导航

    func nextSentence() {
        guard currentSentenceIndex < sentences.count - 1 else { return }
        currentSentenceIndex += 1
        resetScores()
        loadCurrentSentence()
        phase = .ready
    }

    func previousSentence() {
        guard currentSentenceIndex > 0 else { return }
        currentSentenceIndex -= 1
        resetScores()
        loadCurrentSentence()
    }

    private func loadCurrentSentence() {
        guard currentSentenceIndex < sentences.count else { return }
        currentSentence = sentences[currentSentenceIndex].text
    }

    private func resetScores() {
        hasScore = false
        pronunciationScore = 0
        intonationScore = 0
        fluencyScore = 0
        phonemeErrors = []
        studentWaveform = []
        referenceWaveform = []
    }

    // MARK: - 重录

    func retryRecording() {
        hasScore = false
        pronunciationScore = 0
        intonationScore = 0
        fluencyScore = 0
        phonemeErrors = []
        studentWaveform = []
        phase = .ready
    }

    // MARK: - 播放参考音并自动进入录音阶段

    func playReferenceAndTransition() {
        phase = .listening
        errorMessage = nil
        playReference { [weak self] in
            guard let self = self else { return }
            if self.errorMessage != nil {
                DispatchQueue.main.async { self.phase = .ready }
                return
            }
            // 播放完成后进入录音准备阶段（等待用户点击录音按钮）
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                self.phase = .recording
            }
        }
    }

    // MARK: - TTS 参考音播放

    func playReference(completion: (() -> Void)? = nil) {
        if isPlayingReference {
            audioPlayer.stop()
            isPlayingReference = false
            return
        }

        guard !currentSentence.isEmpty else { return }
        isPlayingReference = true

        Task { @MainActor in
            do {
                let response: APIResponse<TTSResponseData> = try await APIClient.shared.post(
                    Endpoints.TTS.synthesize,
                    body: TTSRequestBody(text: currentSentence, speed: 50)
                )

                guard let ttsData = response.data,
                      let audioData = Data(base64Encoded: ttsData.audioB64) else {
                    isPlayingReference = false
                    errorMessage = "TTS 返回数据为空"
                    completion?()
                    return
                }

                // 根据格式选择文件扩展名（Fish Audio 返回 mp3，讯飞返回 pcm）
                let ext = ttsData.format ?? "mp3"
                let tempURL = AudioFileManager.shared.createTempFileURL(extension: ext)
                try audioData.write(to: tempURL)

                // 生成参考波形
                let waveform = WaveformGenerator.generateWaveform(from: tempURL, samplesCount: 60)
                referenceWaveform = waveform

                audioPlayer.play(url: tempURL)
                audioPlayer.$isPlaying
                    .dropFirst()
                    .filter { !$0 }
                    .first()
                    .sink { [weak self] _ in
                        self?.isPlayingReference = false
                        completion?()
                    }
                    .store(in: &cancellables)
            } catch {
                isPlayingReference = false
                errorMessage = "参考音播放失败"
                completion?()
            }
        }
    }

    // MARK: - 播放学生录音

    func playStudentRecording() {
        guard let url = lastRecordingURL else { return }
        if isPlayingStudent {
            audioPlayer.stop()
            isPlayingStudent = false
            return
        }
        isPlayingStudent = true
        audioPlayer.play(url: url)
        audioPlayer.$isPlaying
            .dropFirst()
            .filter { !$0 }
            .first()
            .sink { [weak self] _ in self?.isPlayingStudent = false }
            .store(in: &cancellables)
    }

    // MARK: - 录音

    func startRecording() {
        hasScore = false
        errorMessage = nil
        studentWaveform = []

        Task { @MainActor in
            do {
                try await audioRecorder.requestAndStartRecording()
                isRecording = true
            } catch {
                isRecording = false
                errorMessage = "录音启动失败: \(error.localizedDescription)"
            }
        }
    }

    func stopRecording() {
        guard let audioURL = audioRecorder.stopRecording() else {
            isRecording = false
            phase = .ready
            errorMessage = "录音文件保存失败"
            return
        }
        isRecording = false
        lastRecordingURL = audioURL
        // stopRecording 后波形数据仍保留在 audioRecorder 中
        studentWaveform = audioRecorder.waveformData

        // 检查文件是否有数据
        let fileSize = (try? FileManager.default.attributesOfItem(atPath: audioURL.path)[.size] as? Int) ?? 0
        print("[FollowRead] 停止录音: file=\(audioURL.lastPathComponent), size=\(fileSize) bytes, waveform_points=\(studentWaveform.count)")

        if fileSize < 100 {
            phase = .result
            errorMessage = "录音时间太短（\(fileSize) 字节），请重试"
            return
        }

        phase = .evaluating
        evaluateAudio(fileURL: audioURL)
    }

    // MARK: - 评测

    private func evaluateAudio(fileURL: URL) {
        isEvaluating = true

        Task { @MainActor in
            do {
                guard let audioData = try? Data(contentsOf: fileURL), audioData.count > 100 else {
                    isEvaluating = false
                    phase = .result
                    errorMessage = "录音文件为空（\(((try? Data(contentsOf: fileURL))?.count ?? 0)) 字节），请重试"
                    return
                }

                print("[FollowRead] 录音文件大小: \(audioData.count) 字节, URL: \(fileURL)")

                // 调用发音评测接口
                let evalResp: APIResponse<EvalResponse> = try await APIClient.shared.post(
                    Endpoints.Assessment.evaluate,
                    body: EvalRequestBody(
                        audioB64: audioData.base64EncodedString(),
                        referenceText: currentSentence
                    )
                )

                if let result = evalResp.data {
                    hasScore = true
                    pronunciationScore = result.pronunciationScore?.overall ?? 0
                    intonationScore = result.pronunciationScore?.intonation ?? 0
                    fluencyScore = result.pronunciationScore?.fluency ?? 0

                    // 保存到评分历史
                    scoreHistory.append(SentenceScore(
                        sentence: currentSentence,
                        pronunciation: pronunciationScore,
                        intonation: intonationScore,
                        fluency: fluencyScore
                    ))
                }

                isEvaluating = false
                phase = .result

                // 检查是否全部完成
                if currentSentenceIndex >= totalSentences - 1 {
                    isCompleted = true
                }

                // AI 反馈（可选，失败不影响评分）
                if let sid = sessionId, !sid.isEmpty {
                    do {
                        let feedbackResp: APIResponse<FeedbackResponse> = try await APIClient.shared.post(
                            Endpoints.Assessment.feedback,
                            body: FeedbackRequestBody(
                                sessionId: sid,
                                transcript: currentSentence,
                                referenceText: currentSentence
                            )
                        )
                        if let feedback = feedbackResp.data {
                            phonemeErrors = feedback.corrections ?? []
                        }
                    } catch {
                        print("[FollowRead] AI 反馈获取失败（不影响评分）: \(error)")
                    }
                }
            } catch {
                isEvaluating = false
                phase = .result
                errorMessage = "评测失败: \(error)"
            }
        }
    }

    // MARK: - 默认句子

    private static let defaultSentences: [SentenceItem] = [
        SentenceItem(id: "1", text: "The quick brown fox jumps over the lazy dog."),
        SentenceItem(id: "2", text: "She sells seashells by the seashore."),
        SentenceItem(id: "3", text: "How much wood would a woodchuck chuck?"),
        SentenceItem(id: "4", text: "Peter Piper picked a peck of pickled peppers."),
        SentenceItem(id: "5", text: "I scream, you scream, we all scream for ice cream."),
    ]
}

// MARK: - 内部模型

private struct SentenceItem {
    let id: String
    let text: String
}

private struct QuestionListResponse: Decodable {
    let items: [QuestionItem]?
}

private struct QuestionItem: Decodable {
    let id: String
    let promptText: String
}

private struct TTSRequestBody: Encodable {
    let text: String
    let speed: Int
}

private struct TTSResponseData: Decodable {
    let audioB64: String
    let format: String?
    let sampleRate: Int?
}

private struct EvalRequestBody: Encodable {
    let audioB64: String
    let referenceText: String
}

private struct EvalResponse: Decodable {
    let pronunciationScore: PronScore?
}

private struct PronScore: Decodable {
    let overall: Double?
    let fluency: Double?
    let intonation: Double?
    let integrity: Double?
}

private struct FeedbackRequestBody: Encodable {
    let sessionId: String
    let transcript: String
    let referenceText: String
}

private struct FeedbackResponse: Decodable {
    let corrections: [String]?
    let aiFeedback: String?
}

import Foundation

/// 跟读练习视图模型 —— TTS 参考音播放 + 录音 + ISE 评测
final class FollowReadViewModel: ObservableObject {

    // MARK: - Published

    @Published var currentSentence: String = ""
    @Published var currentSentenceIndex: Int = 0
    @Published var totalSentences: Int = 0

    @Published var pronunciationScore: Double = 0
    @Published var intonationScore: Double = 0
    @Published var fluencyScore: Double = 0
    @Published var hasScore = false

    @Published var phonemeErrors: [String] = []

    @Published var referenceWaveform: [Float] = []
    @Published var studentWaveform: [Float] = []

    @Published var isRecording = false
    @Published var isPlayingReference = false
    @Published var isEvaluating = false
    @Published var errorMessage: String?

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

    // MARK: - TTS 参考音播放

    func playReference() {
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
                      let pcmData = Data(base64Encoded: ttsData.audioB64) else {
                    isPlayingReference = false
                    return
                }

                let tempURL = AudioFileManager.shared.createTempFileURL(extension: "pcm")
                try pcmData.write(to: tempURL)

                // 生成参考波形
                let waveform = WaveformGenerator.generateWaveform(from: tempURL, samples: 60)
                referenceWaveform = waveform

                audioPlayer.play(url: tempURL)
                audioPlayer.onPlaybackFinished = { [weak self] in
                    self?.isPlayingReference = false
                }
            } catch {
                isPlayingReference = false
                errorMessage = "参考音播放失败"
            }
        }
    }

    // MARK: - 录音

    func startRecording() {
        isRecording = true
        hasScore = false
        errorMessage = nil
        studentWaveform = []

        do {
            try audioRecorder.startRecording()
        } catch {
            isRecording = false
            errorMessage = "录音启动失败"
        }
    }

    func stopRecording() {
        guard let audioURL = audioRecorder.stopRecording() else {
            isRecording = false
            return
        }
        isRecording = false

        // 捕获学生波形
        studentWaveform = audioRecorder.waveformData

        // 发送评测
        evaluateAudio(fileURL: audioURL)
    }

    // MARK: - 评测

    private func evaluateAudio(fileURL: URL) {
        isEvaluating = true

        Task { @MainActor in
            do {
                guard let audioData = try? Data(contentsOf: fileURL) else {
                    isEvaluating = false
                    errorMessage = "无法读取录音"
                    return
                }

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
                }

                // 获取 AI 反馈（音素级纠错）
                let feedbackResp: APIResponse<FeedbackResponse> = try await APIClient.shared.post(
                    Endpoints.Assessment.feedback,
                    body: FeedbackRequestBody(
                        sessionId: sessionId ?? "",
                        transcript: currentSentence,
                        referenceText: currentSentence
                    )
                )

                if let feedback = feedbackResp.data {
                    phonemeErrors = feedback.corrections ?? []
                }

                isEvaluating = false
            } catch {
                isEvaluating = false
                errorMessage = "评测失败: \(error.localizedDescription)"
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

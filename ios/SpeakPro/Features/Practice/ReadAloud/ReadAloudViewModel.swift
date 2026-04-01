import Foundation

/// 朗读练习视图模型 —— 加载题目、TTS 示范、录音评测
final class ReadAloudViewModel: ObservableObject {

    // MARK: - Published

    @Published var articleTitle: String = "加载中..."
    @Published var articleText: String = ""

    @Published var isRecording = false
    @Published var isPlayingDemo = false
    @Published var isLoading = false
    @Published var playbackSpeed: Double = 1.0
    @Published var errorMessage: String?

    // 评分
    @Published var hasScore = false
    @Published var overallScore: Double = 0
    @Published var pronunciationScore: Double = 0
    @Published var fluencyScore: Double = 0
    @Published var completenessScore: Double = 0

    // MARK: - Dependencies

    let audioRecorder = AudioRecorder()
    private let audioPlayer = AudioPlayer()
    private var questionId: String?
    private var sessionId: String?

    init() {
        loadQuestion()
    }

    // MARK: - 加载题目

    func loadQuestion() {
        isLoading = true
        errorMessage = nil

        Task { @MainActor in
            do {
                let response: APIResponse<QuestionListResponse> = try await APIClient.shared.get(
                    Endpoints.Questions.list,
                    queryItems: [
                        URLQueryItem(name: "exam_type", value: "IELTS"),
                        URLQueryItem(name: "section", value: "ReadAloud"),
                        URLQueryItem(name: "limit", value: "1"),
                    ]
                )

                if let questions = response.data?.items, let question = questions.first {
                    questionId = question.id
                    articleTitle = question.topic ?? "Reading Passage"
                    articleText = question.promptText
                } else {
                    // 使用默认文本
                    articleTitle = "Sample Reading Passage"
                    articleText = """
                    Climate change is one of the most pressing issues of our time. Scientists around the world \
                    have observed significant changes in global temperatures over the past century. The effects \
                    of rising temperatures include more frequent extreme weather events, rising sea levels, and \
                    shifts in ecosystems that affect biodiversity.
                    """
                }
                isLoading = false
            } catch {
                isLoading = false
                // 使用默认文本
                articleTitle = "Sample Reading Passage"
                articleText = """
                Climate change is one of the most pressing issues of our time. Scientists around the world \
                have observed significant changes in global temperatures over the past century.
                """
                print("[ReadAloudVM] 题目加载失败: \(error)")
            }
        }
    }

    // MARK: - TTS 示范播放

    func playDemo() {
        if isPlayingDemo {
            audioPlayer.stop()
            isPlayingDemo = false
            return
        }

        guard !articleText.isEmpty else { return }
        isPlayingDemo = true
        errorMessage = nil

        Task { @MainActor in
            do {
                // 将语速映射为讯飞 speed 参数（0-100）
                let speed: Int
                switch playbackSpeed {
                case 0.5:  speed = 30
                case 1.5:  speed = 70
                default:   speed = 50
                }

                let response: APIResponse<TTSResponseData> = try await APIClient.shared.post(
                    Endpoints.TTS.synthesize,
                    body: TTSRequestBody(text: articleText, speed: speed)
                )

                guard let ttsData = response.data,
                      let pcmData = Data(base64Encoded: ttsData.audioB64) else {
                    isPlayingDemo = false
                    errorMessage = "TTS 返回数据无效"
                    return
                }

                // 将 PCM 数据写入临时文件并播放
                let tempURL = AudioFileManager.shared.createTempFileURL(extension: "pcm")
                try pcmData.write(to: tempURL)
                audioPlayer.play(url: tempURL)
                audioPlayer.onPlaybackFinished = { [weak self] in
                    self?.isPlayingDemo = false
                }
            } catch {
                isPlayingDemo = false
                errorMessage = "TTS 播放失败: \(error.localizedDescription)"
            }
        }
    }

    // MARK: - 录音

    func startRecording() {
        isRecording = true
        hasScore = false
        errorMessage = nil

        do {
            try audioRecorder.startRecording()
        } catch {
            isRecording = false
            errorMessage = "录音启动失败: \(error.localizedDescription)"
        }
    }

    func stopRecording() {
        guard let audioURL = audioRecorder.stopRecording() else {
            isRecording = false
            return
        }
        isRecording = false

        // 发送到评测服务
        evaluateAudio(fileURL: audioURL)
    }

    // MARK: - 评测

    private func evaluateAudio(fileURL: URL) {
        Task { @MainActor in
            do {
                guard let audioData = try? Data(contentsOf: fileURL) else {
                    errorMessage = "无法读取录音文件"
                    return
                }

                // 先创建 session
                let sessionResp: APIResponse<SessionResponse> = try await APIClient.shared.post(
                    Endpoints.Practice.start,
                    body: StartSessionBody(questionId: questionId, mode: "read_aloud")
                )
                sessionId = sessionResp.data?.id

                // 调用评测接口（multipart 音频上传到 Go 服务）
                let evalResp: APIResponse<EvaluationResponse> = try await APIClient.shared.post(
                    Endpoints.Assessment.evaluate,
                    body: EvalRequestBody(
                        audioB64: audioData.base64EncodedString(),
                        referenceText: articleText
                    )
                )

                if let result = evalResp.data {
                    hasScore = true
                    overallScore = result.overallScore
                    pronunciationScore = result.pronunciationScore?.overall ?? 0
                    fluencyScore = result.pronunciationScore?.fluency ?? 0
                    completenessScore = result.pronunciationScore?.integrity ?? 0
                }
            } catch {
                errorMessage = "评测失败: \(error.localizedDescription)"
            }
        }
    }
}

// MARK: - API 模型

private struct QuestionListResponse: Decodable {
    let items: [QuestionItem]?
    let total: Int?
}

private struct QuestionItem: Decodable {
    let id: String
    let examType: String?
    let section: String?
    let topic: String?
    let promptText: String
}

private struct TTSRequestBody: Encodable {
    let text: String
    let speed: Int
}

private struct TTSResponseData: Decodable {
    let audioB64: String
    let format: String?
    let durationMs: Int?
}

private struct StartSessionBody: Encodable {
    let questionId: String?
    let mode: String
}

private struct SessionResponse: Decodable {
    let id: String
}

private struct EvalRequestBody: Encodable {
    let audioB64: String
    let referenceText: String
}

private struct EvaluationResponse: Decodable {
    let overallScore: Double
    let pronunciationScore: PronScoreResponse?
    let grammarScore: GramScoreResponse?
    let aiFeedback: String?
}

private struct PronScoreResponse: Decodable {
    let overall: Double?
    let fluency: Double?
    let integrity: Double?
    let stress: Double?
    let intonation: Double?
}

private struct GramScoreResponse: Decodable {
    let score: Double?
}

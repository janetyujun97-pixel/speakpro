import Foundation
import Combine

/// 朗读练习视图模型
final class ReadAloudViewModel: ObservableObject {

    /// 练习阶段
    enum Phase {
        case reading     // 阅读文章 + 听示范
        case recording   // 录音中
        case evaluating  // 评测中
        case result      // 显示结果
    }

    private var cancellables = Set<AnyCancellable>()

    // MARK: - Published

    @Published var phase: Phase = .reading
    @Published var articleTitle: String = "加载中..."
    @Published var articleText: String = ""
    @Published var currentIndex: Int = 0
    @Published var totalArticles: Int = 0

    @Published var isRecording = false
    @Published var isPlayingDemo = false
    @Published var isPlayingStudent = false
    @Published var playbackSpeed: Double = 1.0
    @Published var errorMessage: String?

    // 评分
    @Published var hasScore = false
    @Published var overallScore: Double = 0
    @Published var pronunciationScore: Double = 0
    @Published var fluencyScore: Double = 0
    @Published var completenessScore: Double = 0
    @Published var aiFeedback: String?
    @Published var lastRecordingURL: URL?

    // MARK: - Dependencies

    let audioRecorder = AudioRecorder()
    private let audioPlayer = AudioPlayer()
    private var questionId: String?
    private var articles: [ArticleItem] = []

    struct ArticleItem {
        let id: String
        let title: String
        let text: String
    }

    init() {
        loadQuestion()
    }

    // MARK: - 加载题目

    func loadQuestion() {
        Task { @MainActor in
            do {
                let response: APIResponse<QuestionListResponse> = try await APIClient.shared.get(
                    Endpoints.Questions.list,
                    queryItems: [
                        URLQueryItem(name: "exam_type", value: "IELTS"),
                        URLQueryItem(name: "limit", value: "20"),
                    ]
                )

                if let questions = response.data?.items, !questions.isEmpty {
                    articles = questions.map {
                        ArticleItem(id: $0.id, title: $0.topic ?? "Reading Passage", text: $0.promptText)
                    }
                } else {
                    articles = Self.defaultArticles
                }
                totalArticles = articles.count
                loadCurrentArticle()
            } catch {
                articles = Self.defaultArticles
                totalArticles = articles.count
                loadCurrentArticle()
            }
        }
    }

    private func loadCurrentArticle() {
        guard currentIndex < articles.count else { return }
        let article = articles[currentIndex]
        questionId = article.id
        articleTitle = article.title
        articleText = article.text
    }

    /// 下一篇文章
    func nextArticle() {
        guard currentIndex < articles.count - 1 else { return }
        currentIndex += 1
        resetForNewArticle()
        loadCurrentArticle()
    }

    private func resetForNewArticle() {
        phase = .reading
        hasScore = false
        overallScore = 0
        pronunciationScore = 0
        fluencyScore = 0
        completenessScore = 0
        aiFeedback = nil
        errorMessage = nil
        lastRecordingURL = nil
    }

    var isLastArticle: Bool {
        currentIndex >= articles.count - 1
    }

    private static let defaultArticles: [ArticleItem] = [
        ArticleItem(id: "d1", title: "Climate Change",
                    text: "Climate change is one of the most pressing issues of our time. Scientists around the world have observed significant changes in global temperatures over the past century. The effects of rising temperatures include more frequent extreme weather events, rising sea levels, and shifts in ecosystems that affect biodiversity."),
        ArticleItem(id: "d2", title: "Artificial Intelligence",
                    text: "Artificial intelligence has transformed the way we live and work. From voice assistants to self-driving cars, AI technologies are becoming increasingly integrated into our daily lives. While these advancements offer tremendous benefits, they also raise important questions about privacy, employment, and ethics."),
        ArticleItem(id: "d3", title: "Space Exploration",
                    text: "The exploration of space has captivated human imagination for centuries. Recent developments in rocket technology have made space travel more accessible than ever before. Private companies are now competing alongside government agencies to push the boundaries of what is possible in space."),
        ArticleItem(id: "d4", title: "Sustainable Energy",
                    text: "The transition to sustainable energy sources is crucial for the future of our planet. Solar and wind power have become increasingly cost-effective alternatives to fossil fuels. Many countries are setting ambitious targets to achieve carbon neutrality within the next few decades."),
        ArticleItem(id: "d5", title: "Global Education",
                    text: "Education is widely recognized as one of the most powerful tools for reducing poverty and inequality. Access to quality education varies significantly across different regions of the world. Technology has the potential to bridge this gap by providing learning opportunities to underserved communities."),
    ]

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
                      let audioData = Data(base64Encoded: ttsData.audioB64) else {
                    isPlayingDemo = false
                    errorMessage = "TTS 返回数据无效"
                    return
                }

                let ext = ttsData.format ?? "mp3"
                let tempURL = AudioFileManager.shared.createTempFileURL(extension: ext)
                try audioData.write(to: tempURL)
                audioPlayer.play(url: tempURL)
                audioPlayer.$isPlaying
                    .dropFirst()
                    .filter { !$0 }
                    .first()
                    .sink { [weak self] _ in self?.isPlayingDemo = false }
                    .store(in: &cancellables)
            } catch {
                isPlayingDemo = false
                errorMessage = "TTS 播放失败"
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

        Task { @MainActor in
            do {
                try await audioRecorder.requestAndStartRecording()
                isRecording = true
                phase = .recording
            } catch {
                isRecording = false
                errorMessage = "录音启动失败: \(error.localizedDescription)"
            }
        }
    }

    func stopRecording() {
        guard let audioURL = audioRecorder.stopRecording() else {
            isRecording = false
            phase = .reading
            return
        }
        isRecording = false
        lastRecordingURL = audioURL

        phase = .evaluating
        evaluateAudio(fileURL: audioURL)
    }

    // MARK: - 重录

    func retryRecording() {
        hasScore = false
        overallScore = 0
        pronunciationScore = 0
        fluencyScore = 0
        completenessScore = 0
        aiFeedback = nil
        errorMessage = nil
        phase = .reading
    }

    // MARK: - 评测

    private func evaluateAudio(fileURL: URL) {
        Task { @MainActor in
            do {
                guard let audioData = try? Data(contentsOf: fileURL), audioData.count > 100 else {
                    errorMessage = "录音文件为空，请重试"
                    phase = .result
                    return
                }

                // 调用评测接口
                let evalResp: APIResponse<EvaluationResponse> = try await APIClient.shared.post(
                    Endpoints.Assessment.evaluate,
                    body: EvalRequestBody(
                        audioB64: audioData.base64EncodedString(),
                        referenceText: articleText
                    )
                )

                if let result = evalResp.data {
                    hasScore = true
                    pronunciationScore = result.pronunciationScore?.overall ?? 0
                    fluencyScore = result.pronunciationScore?.fluency ?? 0
                    completenessScore = result.pronunciationScore?.integrity ?? 0
                    // 综合分 = 三维度加权
                    overallScore = pronunciationScore * 0.4 + fluencyScore * 0.3 + completenessScore * 0.3
                }

                phase = .result

                // AI 反馈（可选）
                do {
                    let feedbackResp: APIResponse<FeedbackResponse> = try await APIClient.shared.post(
                        Endpoints.Assessment.feedback,
                        body: FeedbackRequestBody(
                            sessionId: "read_aloud_\(Date().timeIntervalSince1970)",
                            transcript: articleText,
                            referenceText: articleText
                        )
                    )
                    aiFeedback = feedbackResp.data?.aiFeedback
                } catch {
                    // AI 反馈失败不影响评分
                }

            } catch {
                errorMessage = "评测失败: \(error.localizedDescription)"
                phase = .result
            }
        }
    }
}

// MARK: - API 模型

private struct QuestionListResponse: Decodable {
    let items: [QuestionItem]?
}

private struct QuestionItem: Decodable {
    let id: String
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
}

private struct EvalRequestBody: Encodable {
    let audioB64: String
    let referenceText: String
}

private struct EvaluationResponse: Decodable {
    let pronunciationScore: PronScoreResp?
}

private struct PronScoreResp: Decodable {
    let overall: Double?
    let fluency: Double?
    let integrity: Double?
    let intonation: Double?
}

private struct FeedbackRequestBody: Encodable {
    let sessionId: String
    let transcript: String
    let referenceText: String
}

private struct FeedbackResponse: Decodable {
    let aiFeedback: String?
    let corrections: [String]?
}

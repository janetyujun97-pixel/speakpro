import Foundation

/// 模考视图模型 —— 真实题目加载、逐题录音评测、汇总报告
final class MockExamViewModel: ObservableObject {

    // MARK: - Published

    @Published var currentPart: Int = 1
    @Published var currentQuestionIndex: Int = 0
    @Published var currentQuestion: String = ""
    @Published var subQuestions: [String] = []

    @Published var isRecording = false
    @Published var canProceed = false
    @Published var remainingTime: TimeInterval = 60
    @Published var statusText: String = "正在加载题目..."
    @Published var errorMessage: String?
    @Published var isLoading = true

    // 考试结果
    @Published var isExamFinished = false
    @Published var examScores: [QuestionScore] = []
    @Published var overallScore: Double = 0

    // MARK: - Data

    var totalQuestions: Int { questions.count }
    private var questions: [ExamQuestion] = []
    private var timer: Timer?
    private let audioRecorder = AudioRecorder()

    var formattedTime: String {
        let minutes = Int(remainingTime) / 60
        let seconds = Int(remainingTime) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init() {
        loadExamQuestions()
    }

    // MARK: - 加载真实题目

    func loadExamQuestions() {
        isLoading = true

        Task { @MainActor in
            var allQuestions: [ExamQuestion] = []

            // 分 Part 加载题目
            for (part, section, count, timeLimit) in [
                (1, "Part1", 3, 60),
                (2, "Part2", 1, 120),
                (3, "Part3", 2, 60),
            ] {
                do {
                    let resp: APIResponse<QListResponse> = try await APIClient.shared.get(
                        Endpoints.Questions.list,
                        queryItems: [
                            URLQueryItem(name: "exam_type", value: "IELTS"),
                            URLQueryItem(name: "section", value: section),
                            URLQueryItem(name: "limit", value: "\(count)"),
                        ]
                    )
                    if let items = resp.data?.items {
                        for item in items {
                            allQuestions.append(ExamQuestion(
                                id: item.id,
                                part: part,
                                question: item.promptText,
                                subs: [], // Part2 的 sub 可以从 promptText 中解析
                                timeLimit: timeLimit
                            ))
                        }
                    }
                } catch {
                    print("[MockExamVM] Part\(part) 题目加载失败: \(error)")
                }
            }

            // 如果 API 加载失败，使用默认题目
            if allQuestions.isEmpty {
                allQuestions = Self.defaultQuestions
            }

            questions = allQuestions
            isLoading = false
            loadCurrentQuestion()
            statusText = "准备好后请开始录音"
        }
    }

    // MARK: - 录音

    func startRecording() {
        isRecording = true
        statusText = "正在录音..."
        canProceed = false
        errorMessage = nil

        do {
            try audioRecorder.startRecording()
            startTimer()
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
        stopTimer()
        statusText = "正在评测..."

        // 评测当前题目
        evaluateQuestion(fileURL: audioURL)
    }

    // MARK: - 评测单题

    private func evaluateQuestion(fileURL: URL) {
        Task { @MainActor in
            do {
                guard let audioData = try? Data(contentsOf: fileURL) else {
                    statusText = "录音文件读取失败"
                    canProceed = true
                    return
                }

                let evalResp: APIResponse<EvalResult> = try await APIClient.shared.post(
                    Endpoints.Assessment.evaluate,
                    body: EvalBody(
                        audioB64: audioData.base64EncodedString(),
                        referenceText: currentQuestion
                    )
                )

                let score = evalResp.data?.overallScore ?? 0
                let qScore = QuestionScore(
                    part: currentPart,
                    question: currentQuestion,
                    score: score
                )
                examScores.append(qScore)

                statusText = String(format: "评分: %.0f 分", score)
                canProceed = true
            } catch {
                statusText = "评测失败"
                canProceed = true
                // 记录 0 分，不阻塞考试流程
                examScores.append(QuestionScore(
                    part: currentPart,
                    question: currentQuestion,
                    score: 0
                ))
            }
        }
    }

    // MARK: - 导航

    func nextQuestion() {
        guard currentQuestionIndex < questions.count - 1 else {
            endExam()
            return
        }
        currentQuestionIndex += 1
        loadCurrentQuestion()
        canProceed = false
        statusText = "准备好后请开始录音"
    }

    func endExam() {
        stopTimer()
        isExamFinished = true

        // 计算总分
        let validScores = examScores.filter { $0.score > 0 }
        if !validScores.isEmpty {
            overallScore = validScores.reduce(0) { $0 + $1.score } / Double(validScores.count)
        }
        statusText = String(format: "考试结束 — 总分: %.0f", overallScore)
    }

    private func loadCurrentQuestion() {
        guard currentQuestionIndex < questions.count else { return }
        let q = questions[currentQuestionIndex]
        currentPart = q.part
        currentQuestion = q.question
        subQuestions = q.subs
        remainingTime = TimeInterval(q.timeLimit)
    }

    // MARK: - Timer

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            if self.remainingTime > 0 {
                self.remainingTime -= 1
            } else {
                self.stopRecording()
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

    // MARK: - 默认题目（API 不可用时）

    private static let defaultQuestions: [ExamQuestion] = [
        ExamQuestion(id: "d1", part: 1, question: "Do you work or are you a student?", subs: [], timeLimit: 60),
        ExamQuestion(id: "d2", part: 1, question: "What do you enjoy most about your studies?", subs: [], timeLimit: 60),
        ExamQuestion(id: "d3", part: 2, question: "Describe a place you have visited that you particularly liked.",
                     subs: ["Where is it?", "When did you go there?", "What did you do there?", "Why did you like it?"], timeLimit: 120),
        ExamQuestion(id: "d4", part: 3, question: "Do you think tourism is good for a country's economy?", subs: [], timeLimit: 60),
    ]
}

// MARK: - 内部模型

struct QuestionScore: Identifiable {
    let id = UUID()
    let part: Int
    let question: String
    let score: Double
}

private struct ExamQuestion {
    let id: String
    let part: Int
    let question: String
    let subs: [String]
    let timeLimit: Int
}

private struct QListResponse: Decodable {
    let items: [QItem]?
}

private struct QItem: Decodable {
    let id: String
    let promptText: String
}

private struct EvalBody: Encodable {
    let audioB64: String
    let referenceText: String
}

private struct EvalResult: Decodable {
    let overallScore: Double?
}

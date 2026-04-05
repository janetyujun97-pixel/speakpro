import Foundation

/// 模考视图模型 —— 真实题目加载、逐题录音、完整AI评测、分部分汇总
final class MockExamViewModel: ObservableObject {

    // MARK: - 考试阶段

    enum ExamPhase: Equatable {
        case loading
        case ready             // 准备开始
        case inProgress        // 答题中
        case evaluating        // AI 评测中
        case showingResult     // 显示评测结果
        case sectionTransition // Part 过渡屏
        case finished          // 考试结束
    }

    // MARK: - Published

    @Published var phase: ExamPhase = .loading
    @Published var currentPart: Int = 1
    @Published var currentQuestionIndex: Int = 0
    @Published var currentQuestion: String = ""
    @Published var subQuestions: [String] = []

    @Published var isRecording = false
    @Published var isPaused = false
    @Published var remainingTime: TimeInterval = 60
    @Published var evaluationProgress: String = ""
    @Published var errorMessage: String?

    // 当前题评测结果
    @Published var currentResult: FullEvaluateResult?
    @Published var currentAudioURL: URL?

    // 考试结果
    @Published var examScores: [EnrichedQuestionScore] = []
    @Published var overallScore: Double = 0

    // 部分过渡
    @Published var transitionFromPart: Int = 0
    @Published var transitionToPart: Int = 0

    // MARK: - Data

    var totalQuestions: Int { questions.count }

    var partAverages: [(part: Int, avg: Double)] {
        [1, 2, 3].map { part in
            let scores = examScores.filter { $0.part == part }.map { $0.score }
            let avg = scores.isEmpty ? 0 : scores.reduce(0, +) / Double(scores.count)
            return (part, avg)
        }
    }

    var progress: Double {
        guard totalQuestions > 0 else { return 0 }
        return Double(currentQuestionIndex) / Double(totalQuestions)
    }

    private var questions: [ExamQuestion] = []
    private var timer: Timer?
    let audioRecorder = AudioRecorder()

    var formattedTime: String {
        let minutes = Int(remainingTime) / 60
        let seconds = Int(remainingTime) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init() {
        loadExamQuestions()
    }

    // MARK: - 加载题目

    func loadExamQuestions() {
        phase = .loading

        Task { @MainActor in
            var allQuestions: [ExamQuestion] = []

            for (part, section, count, timeLimit) in [
                (1, "Part1", 3, 180),
                (2, "Part2", 1, 180),
                (3, "Part3", 2, 180),
            ] as [(Int, String, Int, Int)] {
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
                                id: item.id, part: part, question: item.promptText,
                                subs: [], timeLimit: timeLimit
                            ))
                        }
                    }
                } catch {
                    print("[MockExamVM] Part\(part) 题目加载失败: \(error)")
                }
            }

            if allQuestions.isEmpty {
                allQuestions = Self.defaultQuestions
            }

            questions = allQuestions
            loadCurrentQuestion()
            phase = .ready
        }
    }

    // MARK: - 暂停/恢复

    func togglePause() {
        isPaused.toggle()
        if isPaused {
            stopTimer()
        } else if isRecording {
            startTimer()
        }
    }

    // MARK: - 录音

    func startRecording() {
        guard !isPaused else { return }
        errorMessage = nil

        Task { @MainActor in
            do {
                try await audioRecorder.requestAndStartRecording()
                isRecording = true
                phase = .inProgress
                startTimer()
            } catch {
                isRecording = false
                errorMessage = "录音启动失败: \(error.localizedDescription)"
            }
        }
    }

    func stopRecording() {
        guard let audioURL = audioRecorder.stopRecording() else {
            isRecording = false
            return
        }
        isRecording = false
        stopTimer()

        evaluateQuestion(fileURL: audioURL)
    }

    // MARK: - 完整评测

    private func evaluateQuestion(fileURL: URL) {
        Task { @MainActor in
            phase = .evaluating
            evaluationProgress = "正在识别语音..."

            do {
                guard let audioData = try? Data(contentsOf: fileURL), audioData.count > 100 else {
                    errorMessage = "录音文件为空"
                    phase = .ready
                    return
                }

                let currentQ = questions[currentQuestionIndex]

                evaluationProgress = "AI 正在分析您的回答..."

                let evalResp: APIResponse<FullEvaluateResult> = try await APIClient.shared.post(
                    Endpoints.Assessment.fullEvaluate,
                    body: FullEvaluateBody(
                        audioB64: audioData.base64EncodedString(),
                        referenceText: currentQuestion,
                        examType: "IELTS",
                        section: "Part\(currentPart)",
                        questionId: currentQ.id
                    )
                )

                let result = evalResp.data
                currentResult = result
                currentAudioURL = fileURL

                let score = result?.overallScore ?? 0

                examScores.append(EnrichedQuestionScore(
                    part: currentPart,
                    question: currentQuestion,
                    score: score,
                    audioURL: fileURL,
                    fullResult: result
                ))

                phase = .showingResult

            } catch {
                examScores.append(EnrichedQuestionScore(
                    part: currentPart, question: currentQuestion,
                    score: 0, audioURL: fileURL, fullResult: nil
                ))
                errorMessage = "评测失败: \(error.localizedDescription)"
                phase = .showingResult
            }
        }
    }

    // MARK: - 导航

    func dismissResult() {
        currentResult = nil
        currentAudioURL = nil
        nextQuestion()
    }

    func redoCurrentQuestion() {
        // 移除最后一个分数
        if !examScores.isEmpty { examScores.removeLast() }
        currentResult = nil
        currentAudioURL = nil
        phase = .ready
    }

    func nextQuestion() {
        guard currentQuestionIndex < questions.count - 1 else {
            endExam()
            return
        }

        let currentPartNum = questions[currentQuestionIndex].part
        let nextIndex = currentQuestionIndex + 1
        let nextPartNum = questions[nextIndex].part

        if nextPartNum != currentPartNum {
            transitionFromPart = currentPartNum
            transitionToPart = nextPartNum
            phase = .sectionTransition
            return
        }

        advanceToNextQuestion()
    }

    func continueAfterTransition() {
        advanceToNextQuestion()
    }

    private func advanceToNextQuestion() {
        currentQuestionIndex += 1
        loadCurrentQuestion()
        phase = .ready
    }

    func endExam() {
        stopTimer()
        phase = .finished

        let validScores = examScores.filter { $0.score > 0 }
        if !validScores.isEmpty {
            overallScore = validScores.reduce(0) { $0 + $1.score } / Double(validScores.count)
        }
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
        stopTimer()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            guard let self = self, !self.isPaused else { return }
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

    deinit { timer?.invalidate() }

    // MARK: - 默认题目

    private static let defaultQuestions: [ExamQuestion] = [
        ExamQuestion(id: "d1", part: 1, question: "Do you work or are you a student?", subs: [], timeLimit: 180),
        ExamQuestion(id: "d2", part: 1, question: "What do you enjoy most about your studies?", subs: [], timeLimit: 180),
        ExamQuestion(id: "d3", part: 2, question: "Describe a place you have visited that you particularly liked.",
                     subs: ["Where is it?", "When did you go there?", "What did you do there?", "Why did you like it?"], timeLimit: 180),
        ExamQuestion(id: "d4", part: 3, question: "Do you think tourism is good for a country's economy?", subs: [], timeLimit: 180),
    ]
}

// MARK: - 内部模型

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

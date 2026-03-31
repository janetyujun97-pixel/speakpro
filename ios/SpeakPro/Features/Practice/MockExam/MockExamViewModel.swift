import Foundation

/// 模考视图模型
final class MockExamViewModel: ObservableObject {

    // MARK: - Published

    @Published var currentPart: Int = 1
    @Published var currentQuestionIndex: Int = 0
    @Published var currentQuestion: String = ""
    @Published var subQuestions: [String] = []

    @Published var isRecording = false
    @Published var canProceed = false
    @Published var remainingTime: TimeInterval = 120
    @Published var statusText: String = "准备好后请开始录音"

    // MARK: - Data

    let totalQuestions = 4
    private var timer: Timer?

    private let questions: [(part: Int, question: String, subs: [String])] = [
        (1, "Do you work or are you a student?", []),
        (1, "What do you enjoy most about your studies?", []),
        (2, "Describe a place you have visited that you particularly liked.",
         ["Where is it?", "When did you go there?", "What did you do there?", "Why did you like it?"]),
        (3, "Do you think tourism is good for a country's economy?", [])
    ]

    var formattedTime: String {
        let minutes = Int(remainingTime) / 60
        let seconds = Int(remainingTime) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    init() {
        loadCurrentQuestion()
    }

    // MARK: - Methods

    func startRecording() {
        // TODO: 开始录音
        isRecording = true
        statusText = "正在录音..."
        canProceed = false
        startTimer()
    }

    func stopRecording() {
        // TODO: 停止录音，发送评估
        isRecording = false
        stopTimer()
        statusText = "录音完成"
        canProceed = true
    }

    func nextQuestion() {
        guard currentQuestionIndex < totalQuestions - 1 else {
            endExam()
            return
        }
        currentQuestionIndex += 1
        loadCurrentQuestion()
        canProceed = false
        statusText = "准备好后请开始录音"
        remainingTime = currentPart == 2 ? 120 : 60
    }

    func endExam() {
        stopTimer()
        // TODO: 汇总所有录音，提交综合评估
        statusText = "考试已结束"
    }

    private func loadCurrentQuestion() {
        guard currentQuestionIndex < questions.count else { return }
        let q = questions[currentQuestionIndex]
        currentPart = q.part
        currentQuestion = q.question
        subQuestions = q.subs
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
}

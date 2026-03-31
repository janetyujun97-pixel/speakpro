import Foundation

/// 跟读练习视图模型
final class FollowReadViewModel: ObservableObject {

    // MARK: - Published

    @Published var currentSentence: String = "The quick brown fox jumps over the lazy dog."
    @Published var currentSentenceIndex: Int = 0

    @Published var pronunciationScore: Double = 0
    @Published var intonationScore: Double = 0
    @Published var fluencyScore: Double = 0

    @Published var phonemeErrors: [String] = []

    @Published var referenceWaveform: [Float] = []
    @Published var studentWaveform: [Float] = []

    @Published var isRecording = false
    @Published var isPlayingReference = false

    // MARK: - Data

    private let sentences: [String] = [
        "The quick brown fox jumps over the lazy dog.",
        "She sells seashells by the seashore.",
        "How much wood would a woodchuck chuck?",
        "Peter Piper picked a peck of pickled peppers.",
        "I scream, you scream, we all scream for ice cream."
    ]

    init() {
        loadSentence()
        // 模拟初始评分
        pronunciationScore = 78
        intonationScore = 65
        fluencyScore = 72
        phonemeErrors = [
            "\"th\" 发音：舌尖应轻触上齿",
            "\"fox\" 中 /ɒ/ 元音偏短，需要更饱满"
        ]
        // 模拟波形数据
        referenceWaveform = (0..<60).map { _ in Float.random(in: 0.1...0.8) }
        studentWaveform = (0..<60).map { _ in Float.random(in: 0.05...0.7) }
    }

    // MARK: - Methods

    func nextSentence() {
        currentSentenceIndex = (currentSentenceIndex + 1) % sentences.count
        loadSentence()
        // TODO: 重置评分和波形
    }

    func previousSentence() {
        currentSentenceIndex = max(0, currentSentenceIndex - 1)
        loadSentence()
    }

    private func loadSentence() {
        currentSentence = sentences[currentSentenceIndex]
    }

    func playReference() {
        // TODO: 调用 TTS 接口播放参考音频
        isPlayingReference = true
    }

    func startRecording() {
        // TODO: 开始录音
        isRecording = true
    }

    func stopRecording() {
        // TODO: 停止录音并发送评估
        isRecording = false
    }
}

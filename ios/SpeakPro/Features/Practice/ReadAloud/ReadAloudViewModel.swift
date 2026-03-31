import Foundation

/// 朗读练习视图模型
final class ReadAloudViewModel: ObservableObject {

    // MARK: - Published

    @Published var articleTitle: String = "Sample Reading Passage"
    @Published var articleText: String = """
    Climate change is one of the most pressing issues of our time. Scientists around the world \
    have observed significant changes in global temperatures over the past century. The effects \
    of rising temperatures include more frequent extreme weather events, rising sea levels, and \
    shifts in ecosystems that affect biodiversity.
    """

    @Published var isRecording = false
    @Published var isPlayingDemo = false
    @Published var playbackSpeed: Double = 1.0

    // 评分
    @Published var hasScore = false
    @Published var overallScore: Double = 0
    @Published var pronunciationScore: Double = 0
    @Published var fluencyScore: Double = 0
    @Published var completenessScore: Double = 0

    // MARK: - TTS Demo

    func playDemo() {
        // TODO: 调用 TTS 接口播放示范音频
        isPlayingDemo.toggle()
    }

    // MARK: - Recording

    func startRecording() {
        // TODO: 启动录音
        isRecording = true
        hasScore = false
    }

    func stopRecording() {
        // TODO: 停止录音，发送到服务器评估
        isRecording = false

        // 模拟评分结果
        hasScore = true
        overallScore = 75
        pronunciationScore = 78
        fluencyScore = 70
        completenessScore = 82
    }
}

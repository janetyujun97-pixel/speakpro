import Foundation
import AVFoundation
import Combine

/// 录音管理器 — 基于 AVAudioEngine，支持波形可视化 + 流式推送
final class AudioRecorder: ObservableObject {

    @Published var isRecording = false
    @Published var waveformData: [Float] = []

    private var audioEngine: AVAudioEngine?
    private var inputNode: AVAudioInputNode?
    private var audioFile: AVAudioFile?
    private var recordingURL: URL?

    // MARK: - 音频格式 (讯飞要求: 16kHz, 16-bit, mono PCM)

    private var recordingFormat: AVAudioFormat? {
        AVAudioFormat(
            commonFormat: .pcmFormatInt16,
            sampleRate: 16000,
            channels: 1,
            interleaved: true
        )
    }

    /// 流式音频数据回调（用于实时推送到 WebSocket / ASR）
    var onAudioBuffer: ((Data) -> Void)?

    // MARK: - Permission

    func requestMicrophonePermission() async -> Bool {
        await withCheckedContinuation { continuation in
            AVAudioApplication.requestRecordPermission { granted in
                continuation.resume(returning: granted)
            }
        }
    }

    // MARK: - Start Recording

    func startRecording() throws {
        let engine = AVAudioEngine()
        let input = engine.inputNode

        // 准备本地文件缓存
        let url = AudioFileManager.shared.createTempFileURL(extension: "wav")
        recordingURL = url

        guard let format = recordingFormat else {
            print("[AudioRecorder] 无法创建录音格式")
            return
        }

        audioFile = try AVAudioFile(forWriting: url, settings: format.settings)

        // 安装 tap 获取音频缓冲区
        let inputFormat = input.outputFormat(forBus: 0)
        input.installTap(onBus: 0, bufferSize: 1024, format: inputFormat) { [weak self] buffer, _ in
            guard let self = self else { return }

            // 1. 提取波形可视化数据
            self.extractWaveformData(from: buffer)

            // 2. 写入本地文件
            try? self.audioFile?.write(from: buffer)

            // 3. 流式推送音频数据（转换为 PCM Data）
            if let channelData = buffer.floatChannelData?[0] {
                let frameCount = Int(buffer.frameLength)
                let data = Data(bytes: channelData, count: frameCount * MemoryLayout<Float>.size)
                self.onAudioBuffer?(data)
            }
        }

        engine.prepare()
        try engine.start()

        audioEngine = engine
        inputNode = input
        isRecording = true
    }

    // MARK: - Stop Recording

    @discardableResult
    func stopRecording() -> URL? {
        inputNode?.removeTap(onBus: 0)
        audioEngine?.stop()
        audioEngine = nil
        audioFile = nil
        isRecording = false
        return recordingURL
    }

    // MARK: - Waveform Extraction

    private func extractWaveformData(from buffer: AVAudioPCMBuffer) {
        guard let channelData = buffer.floatChannelData?[0] else { return }
        let frameCount = Int(buffer.frameLength)

        // 取平均振幅作为一个可视化样本
        var sum: Float = 0
        for i in 0..<frameCount {
            sum += abs(channelData[i])
        }
        let average = sum / Float(frameCount)

        DispatchQueue.main.async {
            self.waveformData.append(average)
            // 保留最近的 200 个样本用于显示
            if self.waveformData.count > 200 {
                self.waveformData.removeFirst(self.waveformData.count - 200)
            }
        }
    }
}

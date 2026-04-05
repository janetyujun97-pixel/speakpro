import Foundation
import AVFoundation
import Combine

/// 录音管理器 — 基于 AVAudioEngine，支持波形可视化 + 流式推送
/// 硬件采样率（48kHz）→ AVAudioConverter 重采样为 16kHz mono PCM
final class AudioRecorder: ObservableObject {

    @Published var isRecording = false
    @Published var waveformData: [Float] = []
    @Published var permissionDenied = false

    private var audioEngine: AVAudioEngine?
    private var inputNode: AVAudioInputNode?
    private var audioConverter: AVAudioConverter?
    private var recordingURL: URL?

    // 收集所有 PCM 数据，停止时写入文件
    private var collectedPCMData = Data()
    // 在音频线程同步收集波形（不依赖主线程 async）
    private var rawWaveformSamples: [Float] = []
    private let waveformLock = NSLock()

    // MARK: - 目标格式（讯飞: 16kHz, 16-bit, mono）
    private let targetSampleRate: Double = 16000
    private let targetChannels: AVAudioChannelCount = 1

    private var targetFloatFormat: AVAudioFormat? {
        AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: targetSampleRate, channels: targetChannels, interleaved: false)
    }

    /// 流式音频数据回调
    var onAudioBuffer: ((Data) -> Void)?

    // MARK: - Permission

    func requestMicrophonePermission() async -> Bool {
        await withCheckedContinuation { continuation in
            AVAudioApplication.requestRecordPermission { granted in
                continuation.resume(returning: granted)
            }
        }
    }

    // MARK: - Audio Session

    private func configureAudioSession() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker, .allowBluetooth])
        try session.setActive(true, options: .notifyOthersOnDeactivation)
    }

    // MARK: - Start Recording

    func startRecording() throws {
        let permissionStatus = AVAudioApplication.shared.recordPermission
        switch permissionStatus {
        case .undetermined:
            throw RecordingError.permissionNotDetermined
        case .denied:
            DispatchQueue.main.async { self.permissionDenied = true }
            throw RecordingError.permissionDenied
        case .granted:
            break
        @unknown default:
            break
        }

        try configureAudioSession()
        collectedPCMData = Data()
        waveformLock.lock()
        rawWaveformSamples = []
        waveformLock.unlock()
        waveformData = []

        let engine = AVAudioEngine()
        let input = engine.inputNode
        let hardwareFormat = input.outputFormat(forBus: 0)

        guard let targetFmt = targetFloatFormat else { return }

        // 创建格式转换器
        guard let converter = AVAudioConverter(from: hardwareFormat, to: targetFmt) else {
            print("[AudioRecorder] 无法创建转换器")
            return
        }
        audioConverter = converter

        // 准备文件路径
        recordingURL = AudioFileManager.shared.createTempFileURL(extension: "wav")

        // 安装 tap
        input.installTap(onBus: 0, bufferSize: 4096, format: hardwareFormat) { [weak self] buffer, _ in
            self?.processBuffer(buffer, converter: converter, targetFormat: targetFmt)
        }

        engine.prepare()
        try engine.start()

        audioEngine = engine
        inputNode = input
        DispatchQueue.main.async { self.isRecording = true }
    }

    func requestAndStartRecording() async throws {
        let granted = await requestMicrophonePermission()
        guard granted else {
            await MainActor.run { permissionDenied = true }
            throw RecordingError.permissionDenied
        }
        try startRecording()
    }

    // MARK: - Process Buffer（实时线程，只做转换和推送，不写文件）

    private func processBuffer(_ buffer: AVAudioPCMBuffer, converter: AVAudioConverter, targetFormat: AVAudioFormat) {
        let ratio = targetFormat.sampleRate / buffer.format.sampleRate
        let outputFrameCount = AVAudioFrameCount(Double(buffer.frameLength) * ratio)
        guard outputFrameCount > 0 else { return }

        guard let convertedBuffer = AVAudioPCMBuffer(pcmFormat: targetFormat, frameCapacity: outputFrameCount) else { return }

        var error: NSError?
        var consumed = false
        converter.convert(to: convertedBuffer, error: &error) { _, outStatus in
            if consumed {
                outStatus.pointee = .noDataNow
                return nil
            }
            consumed = true
            outStatus.pointee = .haveData
            return buffer
        }

        guard convertedBuffer.frameLength > 0, let channelData = convertedBuffer.floatChannelData?[0] else { return }
        let frameCount = Int(convertedBuffer.frameLength)

        // 波形可视化 — 同步收集到 rawWaveformSamples（线程安全）
        var sum: Float = 0
        for i in 0..<frameCount { sum += abs(channelData[i]) }
        let avg = sum / Float(max(frameCount, 1))

        waveformLock.lock()
        rawWaveformSamples.append(avg)
        if rawWaveformSamples.count > 200 {
            rawWaveformSamples.removeFirst(rawWaveformSamples.count - 200)
        }
        waveformLock.unlock()

        // 同时更新 @Published（UI 用）
        DispatchQueue.main.async {
            self.waveformData.append(avg)
            if self.waveformData.count > 200 { self.waveformData.removeFirst(self.waveformData.count - 200) }
        }

        // Float32 → Int16 PCM
        var int16Data = Data(count: frameCount * 2)
        int16Data.withUnsafeMutableBytes { raw in
            let ptr = raw.bindMemory(to: Int16.self)
            for i in 0..<frameCount {
                ptr[i] = Int16(max(-1, min(1, channelData[i])) * 32767)
            }
        }

        // 收集用于保存文件
        collectedPCMData.append(int16Data)

        // 流式推送给讯飞
        onAudioBuffer?(int16Data)
    }

    // MARK: - Stop Recording

    /// 停止录音，返回 (文件URL, 波形数据)
    func stopRecording() -> URL? {
        inputNode?.removeTap(onBus: 0)
        audioEngine?.stop()
        audioEngine = nil
        audioConverter = nil

        // 从线程安全的 rawWaveformSamples 获取完整波形
        waveformLock.lock()
        let savedWaveform = rawWaveformSamples
        waveformLock.unlock()

        // 同步更新 @Published waveformData
        waveformData = savedWaveform
        isRecording = false

        // 将收集的 PCM 数据写为 WAV 文件
        if let url = recordingURL, !collectedPCMData.isEmpty {
            let wavData = createWAV(pcmData: collectedPCMData, sampleRate: Int(targetSampleRate), channels: 1, bitsPerSample: 16)
            try? wavData.write(to: url)
        }

        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        return recordingURL
    }

    // MARK: - WAV 文件生成

    private func createWAV(pcmData: Data, sampleRate: Int, channels: Int, bitsPerSample: Int) -> Data {
        let byteRate = sampleRate * channels * bitsPerSample / 8
        let blockAlign = channels * bitsPerSample / 8
        let dataSize = pcmData.count
        let fileSize = 36 + dataSize

        var wav = Data()
        wav.append(contentsOf: "RIFF".utf8)
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(fileSize).littleEndian) { Array($0) })
        wav.append(contentsOf: "WAVE".utf8)
        wav.append(contentsOf: "fmt ".utf8)
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(16).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt16(1).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt16(channels).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(sampleRate).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(byteRate).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt16(blockAlign).littleEndian) { Array($0) })
        wav.append(contentsOf: withUnsafeBytes(of: UInt16(bitsPerSample).littleEndian) { Array($0) })
        wav.append(contentsOf: "data".utf8)
        wav.append(contentsOf: withUnsafeBytes(of: UInt32(dataSize).littleEndian) { Array($0) })
        wav.append(pcmData)
        return wav
    }
}

// MARK: - 错误类型

enum RecordingError: LocalizedError {
    case permissionDenied
    case permissionNotDetermined

    var errorDescription: String? {
        switch self {
        case .permissionDenied: return "麦克风权限被拒绝，请在设置中开启"
        case .permissionNotDetermined: return "需要先授权麦克风权限"
        }
    }
}

import Foundation
import AVFoundation

/// 从音频文件中提取波形数据用于可视化
enum WaveformGenerator {

    /// 从音频 URL 提取指定数量的波形采样点
    /// - Parameters:
    ///   - url: 音频文件 URL
    ///   - samplesCount: 期望的采样点数量
    /// - Returns: 归一化的振幅数组 (0.0 ~ 1.0)
    static func generateWaveform(from url: URL, samplesCount: Int = 100) -> [Float] {
        guard let audioFile = try? AVAudioFile(forReading: url) else {
            print("[WaveformGenerator] 无法读取音频文件: \(url)")
            return []
        }

        let totalFrames = Int(audioFile.length)
        guard totalFrames > 0 else { return [] }

        let format = audioFile.processingFormat
        guard let buffer = AVAudioPCMBuffer(
            pcmFormat: format,
            frameCapacity: AVAudioFrameCount(totalFrames)
        ) else { return [] }

        do {
            try audioFile.read(into: buffer)
        } catch {
            print("[WaveformGenerator] 读取失败: \(error)")
            return []
        }

        guard let channelData = buffer.floatChannelData?[0] else { return [] }

        let framesPerSample = max(totalFrames / samplesCount, 1)
        var samples: [Float] = []
        var maxAmplitude: Float = 0

        for i in 0..<samplesCount {
            let start = i * framesPerSample
            let end = min(start + framesPerSample, totalFrames)
            guard start < totalFrames else { break }

            var sum: Float = 0
            for j in start..<end {
                sum += abs(channelData[j])
            }
            let average = sum / Float(end - start)
            maxAmplitude = max(maxAmplitude, average)
            samples.append(average)
        }

        // 归一化到 0.0 ~ 1.0
        guard maxAmplitude > 0 else { return samples }
        return samples.map { $0 / maxAmplitude }
    }
}

import Foundation
import AVFoundation
import Combine

/// 音频播放管理器
final class AudioPlayer: ObservableObject {

    @Published var isPlaying = false
    @Published var currentTime: TimeInterval = 0
    @Published var duration: TimeInterval = 0
    @Published var waveformData: [Float] = []

    private var player: AVAudioPlayer?
    private var timer: Timer?

    // MARK: - Playback Controls

    func play(url: URL) {
        stop()

        do {
            player = try AVAudioPlayer(contentsOf: url)
            player?.prepareToPlay()
            duration = player?.duration ?? 0
            player?.play()
            isPlaying = true
            startProgressTimer()

            // 预加载波形数据
            waveformData = WaveformGenerator.generateWaveform(from: url, samplesCount: 100)
        } catch {
            print("[AudioPlayer] 播放失败: \(error.localizedDescription)")
        }
    }

    func pause() {
        player?.pause()
        isPlaying = false
        stopProgressTimer()
    }

    func seek(to time: TimeInterval) {
        player?.currentTime = time
        currentTime = time
    }

    func stop() {
        player?.stop()
        player = nil
        isPlaying = false
        currentTime = 0
        stopProgressTimer()
    }

    // MARK: - Progress Timer

    private func startProgressTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self = self, let player = self.player else { return }
            self.currentTime = player.currentTime
            if !player.isPlaying {
                self.isPlaying = false
                self.stopProgressTimer()
            }
        }
    }

    private func stopProgressTimer() {
        timer?.invalidate()
        timer = nil
    }

    deinit {
        stopProgressTimer()
    }
}

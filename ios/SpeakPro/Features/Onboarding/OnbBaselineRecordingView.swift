import AVFoundation
import SwiftUI

/// 07 · 基线录音 —— 使用项目既有 AudioRecorder；30 秒后自动停止并上传。
/// 当前实现：录完音存 WAV 本地，不上传文件到 OSS（需要独立 OSS 上传流程，不在本 PR 范围），
/// 直接调 /onboarding/baseline 传 audioUrl（本地 file:// 的占位），服务端会建 baseline session。
/// 下个 PR 会接通 Go `/practice/audio` 上传，拿到 sessionId 再 PATCH baseline。
struct OnbBaselineRecordingView: View {

    @ObservedObject var vm: OnboardingViewModel

    @StateObject private var recorder = AudioRecorder()
    @State private var elapsed: TimeInterval = 0
    @State private var timer: Timer? = nil
    @State private var isUploading: Bool = false
    @State private var error: String? = nil

    private let duration: TimeInterval = 30

    private let prompt = "Tell me about a place you recently visited…"

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.horizontal, 24)
                .padding(.top, 24)

            Text(prompt)
                .font(.spSerif(22, italic: true))
                .foregroundColor(.spPrimary)
                .padding(.horizontal, 24)
                .padding(.top, 14)

            Spacer()

            waveform
                .padding(.horizontal, 24)

            progressRow
                .padding(.horizontal, 24)
                .padding(.top, 20)

            liveHintCard
                .padding(.horizontal, 24)
                .padding(.top, 20)

            Spacer()

            if let error = error {
                Text(error)
                    .font(.footnote)
                    .foregroundColor(.spError)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 10)
            }

            bottomControls
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
        }
        .onAppear { Task { await startRecording() } }
        .onDisappear { cleanup() }
    }

    // MARK: - Subviews

    private var header: some View {
        HStack {
            HStack(spacing: 8) {
                Circle().fill(Color.spAccent).frame(width: 8, height: 8)
                    .opacity(recorder.isRecording ? 1 : 0.3)
                Text("RECORDING · 录音中").font(.spEyebrow).foregroundColor(.spAccent)
            }
            Spacer()
            Text(elapsedString)
                .font(.spSerif(18, italic: true))
                .foregroundColor(.spPrimary)
        }
    }

    private var waveform: some View {
        let samples = recorder.waveformData
        let barCount = 40
        let reduced = reduceSamples(samples, to: barCount)
        let activeCount = Int(Double(barCount) * min(elapsed / duration, 1))
        return HStack(spacing: 3) {
            ForEach(0..<barCount, id: \.self) { i in
                let h = max(CGFloat(6), CGFloat(reduced.indices.contains(i) ? reduced[i] : 0) * 52 + 8)
                Capsule()
                    .fill(i < activeCount ? Color.spAccent : Color.spLine)
                    .frame(width: 4, height: h)
            }
        }
        .frame(height: 70)
        .frame(maxWidth: .infinity)
    }

    private var progressRow: some View {
        VStack(spacing: 6) {
            HStack {
                Text("0:00").font(.system(size: 10)).foregroundColor(.spMuted)
                Spacer()
                Text("\(elapsedString) / 0:30")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(.spAccent)
                Spacer()
                Text("0:30").font(.system(size: 10)).foregroundColor(.spMuted)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Rectangle().fill(Color.spLine).frame(height: 2)
                    Rectangle().fill(Color.spAccent)
                        .frame(width: geo.size.width * CGFloat(min(elapsed / duration, 1)), height: 2)
                }
            }
            .frame(height: 2)
        }
    }

    private var liveHintCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("LIVE · 实时反馈").font(.spEyebrow).foregroundColor(.spMoss)
            Text(hintText)
                .font(.system(size: 13))
                .foregroundColor(.spPrimary)
                .lineSpacing(3)
            HStack(spacing: 10) {
                Text("● 流畅 · 稳定")
                Text("● 已识别").foregroundColor(.spAccent)
            }
            .font(.system(size: 10))
            .foregroundColor(.spMuted)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.spIvory)
        .overlay(
            RoundedRectangle(cornerRadius: 10).stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private var hintText: String {
        if recorder.isRecording {
            return "你正在录音中 · 说出你最近去过的一个地方"
        } else if isUploading {
            return "正在上传并分析基线录音…"
        } else {
            return "录音已结束"
        }
    }

    private var bottomControls: some View {
        HStack(spacing: 10) {
            Button {
                finishEarly()
            } label: {
                Image(systemName: "stop.fill")
                    .font(.system(size: 16))
                    .foregroundColor(.spPrimary)
                    .frame(width: 56, height: 56)
                    .background(Color.spIvory)
                    .overlay(Circle().stroke(Color.spLine, lineWidth: 1))
                    .clipShape(Circle())
            }
            Button {
                finishEarly()
            } label: {
                HStack {
                    if isUploading {
                        SwiftUI.ProgressView().tint(.spIvory)
                    } else {
                        Text("提前结束 · 分析结果")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.spIvory)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.spPrimary)
                .clipShape(Capsule())
            }
            .disabled(isUploading)
        }
    }

    // MARK: - Recording lifecycle

    private func startRecording() async {
        // 权限已经在 PermissionPrime 请求过；这里若仍为 undetermined 则再请求一次（兜底）
        let status = AVAudioApplication.shared.recordPermission
        if status == .undetermined {
            _ = await recorder.requestMicrophonePermission()
        }

        do {
            try recorder.startRecording()
            elapsed = 0
            timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { t in
                Task { @MainActor in
                    self.elapsed = min(self.elapsed + 0.1, self.duration)
                    if self.elapsed >= self.duration {
                        t.invalidate()
                        self.timer = nil
                        self.finishEarly()
                    }
                }
            }
        } catch {
            self.error = "无法启动录音：\(error.localizedDescription)"
        }
    }

    private func finishEarly() {
        guard recorder.isRecording else { return }
        timer?.invalidate(); timer = nil

        let url = recorder.stopRecording()
        Task { await upload(url: url) }
    }

    private func upload(url: URL?) async {
        isUploading = true
        defer { isUploading = false }

        vm.baselineAudioURL = url

        // PR5 follow-up —— 录音成功后立刻拷到持久缓存（30 条 LRU）
        if let u = url {
            _ = AudioFileManager.shared.cacheRecording(sourceURL: u)
        }

        // 一期：先通过 audioUrl 传本地占位，服务端接受但实际音频尚未上 OSS。
        // 后续 PR 会接通 Go `/practice/audio` → sessionId。
        let audioUrl = url?.lastPathComponent
        await vm.submitBaseline(
            sessionId: nil,
            audioUrl: audioUrl,
            transcript: nil
        )

        // PR5 follow-up —— VM 如果报错说明网络失败，把录音入队下次重试
        if vm.errorMessage != nil, let u = url {
            OfflineUploadQueue.shared.enqueue(OfflineUploadTask(
                audioFilename: u.lastPathComponent,
                sessionId: nil,
            ))
        }

        // 进入 plan
        await vm.finalize()
        vm.step = .plan
    }

    private func cleanup() {
        timer?.invalidate(); timer = nil
        if recorder.isRecording { _ = recorder.stopRecording() }
    }

    // MARK: - Utils

    private var elapsedString: String {
        let total = Int(elapsed)
        return String(format: "0:%02d", total)
    }

    private func reduceSamples(_ samples: [Float], to targetCount: Int) -> [Float] {
        guard !samples.isEmpty else { return [] }
        if samples.count <= targetCount { return samples }
        let chunk = samples.count / targetCount
        var out: [Float] = []
        for i in 0..<targetCount {
            let start = i * chunk
            let end = min(start + chunk, samples.count)
            let slice = samples[start..<end]
            let avg = slice.reduce(0, +) / Float(slice.count)
            out.append(avg)
        }
        return out
    }
}

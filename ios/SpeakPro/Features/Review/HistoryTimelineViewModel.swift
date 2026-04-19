import AVFoundation
import Foundation
import SwiftUI

/// 按天分组的时间线数据结构
struct HistoryGroup {
    let dateLabel: String
    let items: [PracticeSessionListItem]
}

/// 历史时间线的视图模型。
/// - 初次进入：拉 /practice/sessions
/// - 按天分组（本地聚合）
/// - 点播放按钮：调 /practice/sessions/:id/audio 拿 URL（fallback 未签名时直接返原 URL），
///   用 AVPlayer 播放远端或本地 URL
@MainActor
final class HistoryTimelineViewModel: ObservableObject {

    @Published var groups: [HistoryGroup] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    @Published private(set) var currentPlayingId: String? = nil
    @Published private(set) var resolvingId: String? = nil

    private var player: AVPlayer?
    private var statusObserver: NSKeyValueObservation?

    func load() async {
        isLoading = true; defer { isLoading = false }
        do {
            let sessions = try await APIClient.shared.getSessions()
            groups = Self.groupByDay(sessions)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func isPlayingNow(id: String) -> Bool {
        currentPlayingId == id && player?.timeControlStatus == .playing
    }

    func isResolving(id: String) -> Bool { resolvingId == id }

    /// 点击播放按钮：正在播这条 → 暂停；否则拉 URL 并播放
    func togglePlay(for item: PracticeSessionListItem) async {
        if currentPlayingId == item.id {
            if player?.timeControlStatus == .playing {
                player?.pause()
            } else {
                player?.play()
            }
            // 触发一次 objectWillChange（timeControlStatus 非 Published）
            objectWillChange.send()
            return
        }

        resolvingId = item.id
        defer { resolvingId = nil }

        // 已有 audioUrl → 直接播；否则问后端（签名 URL / fallback）
        var urlString = item.audioUrl
        if urlString == nil || urlString?.isEmpty == true {
            do {
                let resp = try await APIClient.shared.getSessionAudio(id: item.id)
                urlString = resp.audioUrl
            } catch {
                errorMessage = error.localizedDescription
                return
            }
        }

        guard let s = urlString, let url = resolveURL(s) else {
            errorMessage = "该录音暂不可用"
            return
        }

        await startPlayback(url: url, id: item.id)
    }

    func stop() {
        player?.pause()
        player = nil
        currentPlayingId = nil
    }

    // MARK: - Private

    private func startPlayback(url: URL, id: String) async {
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)

        let item = AVPlayerItem(url: url)
        let newPlayer = AVPlayer(playerItem: item)
        self.player = newPlayer
        self.currentPlayingId = id

        // 播放完毕回调
        NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: item,
            queue: .main,
        ) { [weak self] _ in
            Task { @MainActor in
                self?.currentPlayingId = nil
            }
        }

        newPlayer.play()
        objectWillChange.send()
    }

    private func resolveURL(_ s: String) -> URL? {
        if s.hasPrefix("http") { return URL(string: s) }
        // 本地占位文件（onboarding baseline 给的文件名）—— 解析到 Documents/Audio
        if let dir = FileManager.default.urls(
            for: .documentDirectory, in: .userDomainMask,
        ).first {
            return dir.appendingPathComponent(s)
        }
        return nil
    }

    // MARK: - Grouping

    private static func groupByDay(
        _ sessions: [PracticeSessionListItem],
    ) -> [HistoryGroup] {
        let cal = Calendar(identifier: .gregorian)
        let today = cal.startOfDay(for: Date())
        let yesterday = cal.date(byAdding: .day, value: -1, to: today)!

        let grouped = Dictionary(grouping: sessions) { item -> Date in
            cal.startOfDay(for: item.createdAt)
        }

        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.dateFormat = "M 月 d 日 EEEE"

        let sortedKeys = grouped.keys.sorted(by: >)
        return sortedKeys.map { day -> HistoryGroup in
            let label: String
            if cal.isDate(day, inSameDayAs: today) {
                label = "今天 · " + f.string(from: day)
            } else if cal.isDate(day, inSameDayAs: yesterday) {
                label = "昨天 · " + f.string(from: day)
            } else {
                label = f.string(from: day)
            }
            let items = (grouped[day] ?? []).sorted { $0.createdAt > $1.createdAt }
            return HistoryGroup(dateLabel: label, items: items)
        }
    }
}

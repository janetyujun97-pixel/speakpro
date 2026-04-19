import AVKit
import SwiftUI

/// 历史时间线 —— 按天分组 + 每条带播放按钮（AVPlayer 直接播 OSS/本地 URL）。
struct HistoryTimelineView: View {

    @StateObject private var vm = HistoryTimelineViewModel()

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()
            Group {
                if vm.isLoading && vm.groups.isEmpty {
                    SwiftUI.ProgressView()
                } else if vm.groups.isEmpty {
                    emptyState
                } else {
                    list
                }
            }
        }
        .navigationTitle("历史回听")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.load() }
        .refreshable { await vm.load() }
    }

    // MARK: - Subviews

    private var list: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 20) {
                ForEach(vm.groups, id: \.dateLabel) { group in
                    Section {
                        VStack(spacing: 0) {
                            ForEach(Array(group.items.enumerated()), id: \.element.id) { idx, item in
                                HistoryRow(item: item, vm: vm)
                                if idx < group.items.count - 1 {
                                    Divider().foregroundColor(.spLine)
                                        .padding(.leading, 16)
                                }
                            }
                        }
                        .background(Color.spIvory)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.spLine, lineWidth: 1)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    } header: {
                        Text(group.dateLabel)
                            .font(.spEyebrow)
                            .foregroundColor(.spMuted)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 20)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "waveform.path")
                .font(.system(size: 36))
                .foregroundColor(.spMuted)
            Text("还没有练习记录")
                .font(.spBodyMedium)
                .foregroundColor(.spMuted)
        }
    }
}

// MARK: - Row

private struct HistoryRow: View {
    let item: PracticeSessionListItem
    @ObservedObject var vm: HistoryTimelineViewModel

    var body: some View {
        HStack(spacing: 14) {
            // 左：播放按钮
            Button {
                Task { await vm.togglePlay(for: item) }
            } label: {
                ZStack {
                    Circle()
                        .fill(isCurrent ? Color.spAccent : Color.spAccentSoft)
                        .frame(width: 36, height: 36)
                    Image(systemName: vm.isPlayingNow(id: item.id) ? "pause.fill" : "play.fill")
                        .font(.system(size: 13))
                        .foregroundColor(isCurrent ? .spIvory : .spAccent)
                }
            }
            .buttonStyle(.plain)
            .disabled(item.audioUrl == nil && !vm.isResolving(id: item.id))

            // 中：题目摘要 + 模式
            VStack(alignment: .leading, spacing: 2) {
                Text(item.question?.promptText ?? item.mode.localizedLabel)
                    .font(.spBodyMedium)
                    .foregroundColor(.spPrimary)
                    .lineLimit(1)
                HStack(spacing: 8) {
                    Text(item.mode.localizedLabel)
                        .font(.spCaption)
                        .foregroundColor(.spMuted)
                    if let dur = item.durationSec, dur > 0 {
                        Text("· \(formatDuration(dur))")
                            .font(.spCaption)
                            .foregroundColor(.spMuted)
                    }
                }
            }

            Spacer()

            // 右：得分
            if let score = item.overallScore {
                Text(String(format: "%.1f", score))
                    .font(.spSerif(18))
                    .foregroundColor(scoreColor(score))
            }
        }
        .padding(14)
    }

    private var isCurrent: Bool { vm.currentPlayingId == item.id }

    private func scoreColor(_ s: Double) -> Color {
        switch s {
        case 80...:   return .spMoss
        case 60..<80: return .spPrimary
        default:      return .spAccent
        }
    }

    private func formatDuration(_ sec: Int) -> String {
        let m = sec / 60, s = sec % 60
        return m > 0 ? String(format: "%d 分 %02d 秒", m, s) : String(format: "%d 秒", s)
    }
}

private extension String {
    /// 把后端 mode 字符串转中文 label
    var localizedLabel: String {
        switch self {
        case "conversation": return "AI 对话"
        case "read_aloud":   return "朗读"
        case "follow_read":  return "跟读"
        case "mock_exam":    return "模考"
        case "baseline":     return "基线测试"
        default:             return self
        }
    }
}

#Preview {
    NavigationStack { HistoryTimelineView() }
}

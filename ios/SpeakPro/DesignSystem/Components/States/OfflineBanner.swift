import SwiftUI

/// 顶部深色 offline sticky bar —— 挂在 RootView 最外层，随 NetworkMonitor 状态显隐。
struct OfflineBanner: View {
    @ObservedObject var monitor: NetworkMonitor

    var body: some View {
        if !monitor.isConnected {
            HStack(spacing: 8) {
                Image(systemName: "wifi.slash")
                    .foregroundColor(.spAccentWarm)
                    .font(.system(size: 12))
                Text("OFFLINE · 无网络连接")
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(1)
                    .foregroundColor(.spIvory)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(Color.spPrimary)
            .transition(.move(edge: .top).combined(with: .opacity))
        }
    }
}

/// 完整 Offline 页 —— 展示功能矩阵 + 已缓存清单；在无网络且无缓存时由列表页切换到此。
struct OfflineFullStateView: View {

    struct CachedItem: Identifiable {
        let id = UUID()
        let title: String
        let subtitle: String
        let type: String     // "朗读" / "AI 对话" / "跟读"
        let size: String     // "3.2 MB"
    }

    struct Capability {
        let label: String
        let available: Bool
    }

    let cachedItems: [CachedItem]
    var capabilities: [Capability] = [
        .init(label: "已下载的朗读 / 跟读", available: true),
        .init(label: "回听历史录音", available: true),
        .init(label: "错题本复习", available: true),
        .init(label: "AI 对话（需联网）", available: false),
        .init(label: "模考（需联网评分）", available: false),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 8)

                // 头部双行 serif 标题
                VStack(alignment: .leading, spacing: -2) {
                    Text("No signal,")
                        .font(.spSerif(30))
                        .foregroundColor(.spPrimary)
                    Text("some rehearsal.")
                        .font(.spSerif(30, italic: true))
                        .foregroundColor(.spAccent)
                }
                .padding(.horizontal, 24)
                .padding(.top, 16)

                (
                    Text("你还有 ")
                    + Text("\(cachedItems.count) 段").foregroundColor(.spPrimary).bold()
                    + Text(" 已下载的材料可以继续练。评分会在联网后")
                    + Text("自动补齐").foregroundColor(.spPrimary).bold()
                    + Text("。")
                )
                .font(.spBodyMedium)
                .foregroundColor(.spMuted)
                .lineSpacing(3)
                .padding(.horizontal, 24)
                .padding(.top, 12)

                // Capabilities card
                VStack(alignment: .leading, spacing: 10) {
                    Text("AVAILABLE · 离线可用")
                        .font(.spEyebrow)
                        .foregroundColor(.spMuted)

                    VStack(spacing: 0) {
                        ForEach(Array(capabilities.enumerated()), id: \.offset) { idx, cap in
                            HStack(spacing: 10) {
                                if cap.available {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.spMoss)
                                        .font(.system(size: 12, weight: .bold))
                                } else {
                                    Image(systemName: "xmark")
                                        .foregroundColor(.spMuted)
                                        .font(.system(size: 12, weight: .medium))
                                }
                                Text(cap.label)
                                    .font(.spBodySmall)
                                    .foregroundColor(cap.available ? .spPrimary : .spMuted)
                                Spacer()
                            }
                            .padding(.vertical, 8)
                            if idx < capabilities.count - 1 {
                                Rectangle().fill(Color.spLine).frame(height: 1)
                            }
                        }
                    }
                    .padding(16)
                    .background(Color.spIvory)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(Color.spLine, lineWidth: 1),
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                }
                .padding(.horizontal, 24)
                .padding(.top, 22)

                // Cached list
                if !cachedItems.isEmpty {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("DOWNLOADED · 已下载 \(cachedItems.count) 段")
                            .font(.spEyebrow)
                            .foregroundColor(.spMuted)

                        VStack(spacing: 0) {
                            ForEach(Array(cachedItems.enumerated()), id: \.element.id) { idx, item in
                                HStack(spacing: 12) {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(item.title)
                                            .font(.spBodyMedium)
                                            .foregroundColor(.spPrimary)
                                        Text(item.subtitle)
                                            .font(.spCaption)
                                            .foregroundColor(.spMuted)
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 2) {
                                        Text(item.type)
                                            .font(.spCaption)
                                            .foregroundColor(.spAccent)
                                        Text(item.size)
                                            .font(.spCaption)
                                            .foregroundColor(.spMuted)
                                    }
                                }
                                .padding(.vertical, 13)
                                if idx < cachedItems.count - 1 {
                                    Rectangle().fill(Color.spLine).frame(height: 1)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 20)
                }

                Spacer(minLength: 24)
            }
        }
        .background(Color.spBackground)
    }
}

#Preview("Banner") {
    VStack(spacing: 0) {
        OfflineBanner(monitor: NetworkMonitor.shared)
        Spacer()
    }
}

#Preview("Full") {
    OfflineFullStateView(cachedItems: [
        .init(title: "Passage 17", subtitle: "On focus, as a craft", type: "朗读", size: "3.2 MB"),
        .init(title: "Part 1 · Hometown", subtitle: "IELTS interview", type: "AI 对话", size: "1.8 MB"),
    ])
}

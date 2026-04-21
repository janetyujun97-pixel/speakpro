import SwiftUI

/// 练习 Tab —— editorial 风格
///
/// 结构对应 Android `features/practice/PracticeListScreen.kt`：
///   Masthead → Hero headline → Quick tiles 2×2 → Category tabs →
///   Scene rows (编号 01..05) → Dark recommend card
struct PracticeListView: View {

    @State private var tab: CategoryTab = .scene
    @State private var destination: PracticeMode?

    private let quickTiles: [QuickTile] = [
        QuickTile(title: "朗读", en: "Reading",   last: "4h ago",    streak: 3,    mode: .readAloud),
        QuickTile(title: "跟读", en: "Shadow",    last: "Yesterday", streak: nil,  mode: .followRead),
        // 听写/复述 暂无对应页 —— 暂时分别指向跟读/朗读，后续补专属实现
        QuickTile(title: "听写", en: "Dictation", last: "—",         streak: nil,  mode: .followRead),
        QuickTile(title: "复述", en: "Retell",    last: "2d ago",    streak: nil,  mode: .readAloud),
    ]

    private let scenes: [Scene] = [
        Scene(tag: "IELTS · Part 1", title: "日常话题问答",  en: "Daily interview",        count: 24, lvl: "基础", color: .spAccent),
        Scene(tag: "IELTS · Part 2", title: "Cue Card 独白", en: "Long turn · 2 min",      count: 18, lvl: "中级", color: .spPrimary),
        Scene(tag: "IELTS · Part 3", title: "深度讨论",     en: "Two-way discussion",     count: 16, lvl: "进阶", color: .spMoss),
        Scene(tag: "面试 · Interview", title: "英文面试",   en: "Job interview",          count: 12, lvl: "中级", color: Color(red: 0x8A/255, green: 0x5A/255, blue: 0x2B/255)),
        Scene(tag: "商务 · Business", title: "会议与演讲",  en: "Meetings & pitches",     count:  9, lvl: "进阶", color: Color(red: 0x5A/255, green: 0x4A/255, blue: 0x8A/255)),
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    // Masthead
                    HStack {
                        Eyebrow("PRACTICE · 练习")
                        Spacer()
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 18))
                            .foregroundColor(.spMuted)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 12)
                    .padding(.bottom, 16)

                    // Hero headline
                    VStack(alignment: .leading, spacing: 0) {
                        Text("Pick your")
                            .font(Font.custom("Fraunces", size: 32))
                            .foregroundColor(.spPrimary)
                        Text("rhythm today.")
                            .font(Font.custom("Fraunces-Italic", size: 32))
                            .foregroundColor(.spAccent)
                    }
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 24)

                    // Quick tiles 2×2
                    VStack(alignment: .leading, spacing: 0) {
                        Eyebrow("QUICK · 快练 15 分钟")
                        Spacer().frame(height: 10)
                        VStack(spacing: 8) {
                            ForEach(0..<(quickTiles.count / 2), id: \.self) { row in
                                HStack(spacing: 8) {
                                    ForEach(0..<2, id: \.self) { col in
                                        let t = quickTiles[row * 2 + col]
                                        QuickTileCard(tile: t)
                                            .onTapGesture { destination = t.mode }
                                            .frame(maxWidth: .infinity)
                                    }
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 22)

                    // Category tabs
                    HStack(spacing: 20) {
                        ForEach(CategoryTab.allCases, id: \.self) { t in
                            VStack(spacing: 0) {
                                Text(t.label)
                                    .font(Font.custom("Inter", size: 13).weight(tab == t ? .semibold : .regular))
                                    .foregroundColor(tab == t ? .spPrimary : .spMuted)
                                    .padding(.vertical, 10)
                                Rectangle()
                                    .fill(tab == t ? Color.spPrimary : Color.clear)
                                    .frame(height: 2)
                            }
                            .contentShape(Rectangle())
                            .onTapGesture { tab = t }
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 24)

                    Rectangle().fill(Color.spLine).frame(height: 1)
                        .padding(.horizontal, 24)

                    // Scene rows
                    VStack(spacing: 0) {
                        ForEach(Array(scenes.enumerated()), id: \.offset) { idx, s in
                            SceneRow(num: idx + 1, scene: s)
                                .onTapGesture { destination = .conversation }
                        }
                    }
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 22)

                    // Dark recommend card
                    HStack(alignment: .center, spacing: 12) {
                        Text("✦")
                            .font(Font.custom("Fraunces-Italic", size: 34))
                            .foregroundColor(Color(red: 0xD9/255, green: 0x73/255, blue: 0x4A/255))
                        VStack(alignment: .leading, spacing: 4) {
                            Eyebrow("RECOMMENDED · 推荐", color: .spIvory.opacity(0.55))
                            Text("今天建议练 Part 3 深度讨论")
                                .font(Font.custom("Fraunces", size: 16))
                                .foregroundColor(.spIvory)
                            Text("· 基于昨天的评分，Coherence 有提升空间")
                                .font(Font.custom("Inter", size: 11))
                                .foregroundColor(.spIvory.opacity(0.55))
                        }
                        Spacer()
                        Image(systemName: "arrow.right")
                            .font(.system(size: 13))
                            .foregroundColor(.spIvory)
                    }
                    .padding(16)
                    .background(Color.spPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .contentShape(Rectangle())
                    .onTapGesture { destination = .conversation }
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 24)
                }
            }
            .background(Color.spBackground)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(item: $destination) { mode in
                switch mode {
                case .conversation: ConversationView()
                case .readAloud:    ReadAloudView()
                case .followRead:   FollowReadView()
                case .mockExam:     MockExamView()
                }
            }
        }
    }
}

// MARK: - 数据模型

private enum CategoryTab: String, CaseIterable {
    case scene, level, topic
    var label: String {
        switch self {
        case .scene: return "场景分类"
        case .level: return "按难度"
        case .topic: return "话题"
        }
    }
}

private struct QuickTile: Identifiable {
    let id = UUID()
    let title: String
    let en: String
    let last: String
    let streak: Int?
    let mode: PracticeMode
}

private struct Scene: Identifiable {
    let id = UUID()
    let tag: String
    let title: String
    let en: String
    let count: Int
    let lvl: String
    let color: Color
}

// MARK: - 组件

private struct QuickTileCard: View {
    let tile: QuickTile

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                Text(tile.title)
                    .font(Font.custom("Fraunces", size: 20))
                    .foregroundColor(.spPrimary)
                Spacer()
                if let s = tile.streak {
                    Text("\(s)D")
                        .font(Font.custom("Inter", size: 9).weight(.semibold))
                        .tracking(1)
                        .foregroundColor(.spIvory)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.spAccent)
                        .clipShape(RoundedRectangle(cornerRadius: 2))
                }
            }
            Spacer().frame(height: 2)
            Text(tile.en)
                .font(Font.custom("Fraunces-Italic", size: 10))
                .foregroundColor(.spMuted)
            Spacer().frame(height: 16)
            Text("上次 · \(tile.last)")
                .font(Font.custom("Inter", size: 10))
                .tracking(0.5)
                .foregroundColor(.spMuted)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.spIvory)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}

private struct SceneRow: View {
    let num: Int
    let scene: Scene

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 14) {
                Text(String(format: "%02d", num))
                    .font(Font.custom("Fraunces-Italic", size: 26))
                    .foregroundColor(.spMuted)
                    .frame(width: 32, alignment: .leading)

                VStack(alignment: .leading, spacing: 0) {
                    Eyebrow(scene.tag, color: scene.color)
                    Spacer().frame(height: 6)
                    Text(scene.title)
                        .font(Font.custom("Fraunces", size: 18))
                        .foregroundColor(.spPrimary)
                    Spacer().frame(height: 3)
                    Text(scene.en)
                        .font(Font.custom("Fraunces-Italic", size: 11))
                        .foregroundColor(.spMuted)
                    Spacer().frame(height: 8)
                    HStack(spacing: 10) {
                        Text("● \(scene.count) 题")
                            .font(Font.custom("Inter", size: 10))
                            .tracking(0.5)
                            .foregroundColor(.spMuted)
                        Text("● \(scene.lvl)")
                            .font(Font.custom("Inter", size: 10))
                            .tracking(0.5)
                            .foregroundColor(.spMuted)
                    }
                }

                Spacer()

                Image(systemName: "arrow.right")
                    .font(.system(size: 12))
                    .foregroundColor(.spMuted)
            }
            .padding(.vertical, 18)
            .contentShape(Rectangle())

            Rectangle().fill(Color.spLine).frame(height: 1)
        }
    }
}

// Eyebrow 与 HomeView 保持同样视觉（本页声明一份独立的 private 版本避免跨文件可见性）
private struct Eyebrow: View {
    let text: String
    var color: Color = .spMuted

    init(_ text: String, color: Color = .spMuted) {
        self.text = text
        self.color = color
    }

    var body: some View {
        Text(text.uppercased())
            .font(Font.custom("Inter", size: 10).weight(.semibold))
            .tracking(2.2)
            .foregroundColor(color)
    }
}

// MARK: - 保留枚举 (其他模块如 HomeView 的 navigationDestination 引用)

enum PracticeMode: String, CaseIterable, Identifiable, Hashable {
    case conversation
    case readAloud
    case followRead
    case mockExam

    var id: String { rawValue }

    var title: String {
        switch self {
        case .conversation: return "AI 对话"
        case .readAloud:    return "朗读"
        case .followRead:   return "跟读"
        case .mockExam:     return "模考"
        }
    }

    var subtitle: String {
        switch self {
        case .conversation: return "模拟真实口试，实时评分与追问"
        case .readAloud:    return "评测发音、流利度与语调"
        case .followRead:   return "对照标准发音逐句纠正"
        case .mockExam:     return "Part 1·2·3 全真流程 · 11 分钟"
        }
    }

    var iconName: String {
        switch self {
        case .conversation: return "mic.fill"
        case .readAloud:    return "book.fill"
        case .followRead:   return "waveform"
        case .mockExam:     return "timer"
        }
    }
}

#Preview {
    PracticeListView()
}

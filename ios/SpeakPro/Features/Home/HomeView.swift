import SwiftUI

/// 首页 —— editorial 杂志风格
///
/// 结构对照 Android `features/home/HomeScreen.kt`：
///   Masthead → Hero headline → Stats strip → Mock exam hero（3 变体）→
///   Practice list (02/03/04) → Today's recommendation → Assignments → Quote footer
///
/// dev 彩蛋：连点 masthead 的 "No. XXX" 3 下打开风格切换 sheet。
struct HomeView: View {

    @EnvironmentObject private var coordinator: AppCoordinator
    @StateObject private var viewModel = HomeViewModel()
    @StateObject private var stylePref = HomeStylePreference.shared
    @State private var navigationPath = NavigationPath()
    @State private var showStyleSheet = false

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Masthead(
                        edition: editionNumber(),
                        onTripleTap: { showStyleSheet = true }
                    )

                    if !stylePref.current.available {
                        UnavailableHint(style: stylePref.current)
                            .padding(.top, 8)
                    }

                    HeroHeadline()
                    StatsStrip(
                        streakDays: viewModel.streakDays,
                        todayPercent: Int(viewModel.todayProgress * 100),
                        targetScore: 7.0
                    )

                    MockExamFeature(
                        variant: effectiveStyle,
                        onTap: { navigationPath.append(PracticeMode.mockExam) }
                    )
                    .padding(.top, 32)

                    PracticeListSection(onTap: { mode in
                        navigationPath.append(mode)
                    })
                    .padding(.top, 32)

                    TodaysRecommendation(onTap: {
                        navigationPath.append(PracticeMode.conversation)
                    })
                    .padding(.top, 36)

                    AssignmentsSection(
                        pending: viewModel.pendingHomework,
                        onTap: {
                            NotificationCenter.default.post(name: .switchToPendingHomework, object: nil)
                            coordinator.selectedTab = .homework
                        }
                    )
                    .padding(.top, 32)

                    QuoteFooter(edition: editionNumber())
                        .padding(.top, 40)
                        .padding(.bottom, 40)
                }
            }
            .background(Color.spBackground)
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: PracticeMode.self) { mode in
                switch mode {
                case .conversation: ConversationView()
                case .readAloud:    ReadAloudView()
                case .followRead:   FollowReadView()
                case .mockExam:     MockExamView()
                }
            }
            .task { await viewModel.fetchHomeData() }
            .sheet(isPresented: $showStyleSheet) {
                HomeStyleSheet(
                    current: stylePref.current,
                    onPick: { s in
                        stylePref.set(s)
                        showStyleSheet = false
                    }
                )
                .presentationDetents([.medium, .large])
                .presentationBackground(Color.spBackground)
            }
        }
    }

    /// 未实现风格回退到 editorial_full 渲染
    private var effectiveStyle: HomeStyle {
        stylePref.current.available ? stylePref.current : .editorialFull
    }
}

// MARK: - Masthead

private struct Masthead: View {
    let edition: String
    let onTripleTap: () -> Void

    @State private var tapCount = 0
    @State private var lastTap: Date = .distantPast

    var body: some View {
        HStack(alignment: .center) {
            Text("SPEAKPRO · NO. \(edition)")
                .font(Font.custom("Inter", size: 10).weight(.semibold))
                .tracking(2.2)
                .foregroundColor(.spMuted)
                .contentShape(Rectangle())
                .onTapGesture { handleTap() }

            Spacer()

            ZStack {
                Circle()
                    .fill(Color.spAccentSoft)
                Circle()
                    .strokeBorder(Color.spAccent.opacity(0.13), lineWidth: 1)
                Image(systemName: "flame.fill")
                    .font(.system(size: 14))
                    .foregroundColor(.spAccent)
            }
            .frame(width: 36, height: 36)
        }
        .padding(.horizontal, 24)
        .padding(.top, 14)
    }

    private func handleTap() {
        let now = Date()
        if now.timeIntervalSince(lastTap) > 1.5 { tapCount = 0 }
        tapCount += 1
        lastTap = now
        if tapCount >= 3 {
            tapCount = 0
            onTripleTap()
        }
    }
}

private func editionNumber() -> String {
    let day = Calendar.current.ordinality(of: .day, in: .year, for: Date()) ?? 1
    return String(format: "%03d", day)
}

// MARK: - Hero headline

private struct HeroHeadline: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            (
                Text("你好，同学。\n")
                    .font(Font.custom("Fraunces", size: 40))
                    .foregroundColor(.spPrimary)
                + Text("今日宜")
                    .font(Font.custom("Fraunces-Italic", size: 40))
                    .foregroundColor(.spMuted)
                + Text(" 开口。")
                    .font(Font.custom("Fraunces-Italic", size: 40))
                    .foregroundColor(.spAccent)
            )
            .lineSpacing(4)

            Text(subtitleLine())
                .font(Font.custom("Inter", size: 13))
                .tracking(0.3)
                .foregroundColor(.spMuted)
        }
        .padding(.horizontal, 24)
        .padding(.top, 16)
    }

    private func subtitleLine() -> String {
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US")
        df.dateFormat = "EEEE · MMMM d"
        let today = Date()
        let enDate = df.string(from: today)

        let cal = Calendar.current
        let exam = cal.date(byAdding: .month, value: 1, to: today) ?? today
        let days = cal.dateComponents([.day], from: today, to: exam).day ?? 0
        return "\(enDate)  ·  距考试 \(max(0, days)) 天"
    }
}

// MARK: - Stats strip

private struct StatsStrip: View {
    let streakDays: Int
    let todayPercent: Int
    let targetScore: Double

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .bottom) {
                StatColumn(label: "连续", value: "\(streakDays)", unit: "天", accent: false)
                Spacer()
                StatColumn(label: "今日", value: "\(todayPercent)", unit: "%", accent: false)
                Spacer()
                StatColumn(label: "目标分", value: String(format: "%.1f", targetScore), unit: nil, accent: true)
            }
            .padding(.top, 26)

            Rectangle()
                .fill(Color.spLine)
                .frame(height: 1)
                .padding(.top, 18)
        }
        .padding(.horizontal, 24)
    }
}

private struct StatColumn: View {
    let label: String
    let value: String
    let unit: String?
    let accent: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Eyebrow(label)
            HStack(alignment: .bottom, spacing: 6) {
                Text(value)
                    .font(Font.custom("Fraunces", size: 44))
                    .tracking(-1)
                    .foregroundColor(accent ? .spAccent : .spPrimary)
                if let u = unit {
                    Text(u)
                        .font(Font.custom("Inter", size: 13))
                        .foregroundColor(.spMuted)
                        .padding(.bottom, 4)
                }
            }
        }
    }
}

// MARK: - Mock Exam feature

private struct MockExamFeature: View {
    let variant: HomeStyle
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Eyebrow("专题 · FEATURED")
                Spacer()
                Text(rightTag)
                    .font(Font.custom("Inter", size: 11))
                    .tracking(1)
                    .foregroundColor(.spMuted)
            }

            switch variant {
            case .editorialTicket:  MockTicket(onTap: onTap)
            case .editorialDiagram: MockDiagram(onTap: onTap)
            default:                MockFull(onTap: onTap)
            }
        }
        .padding(.horizontal, 24)
    }

    private var rightTag: String {
        switch variant {
        case .editorialTicket:  return "TICKET"
        case .editorialDiagram: return "FIG. 01"
        default:                return "01 / 04"
        }
    }
}

/// 深墨底大卡 —— 对应 Android `MockFull`
private struct MockFull: View {
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Eyebrow("模考 · MOCK EXAM", color: .spIvory.opacity(0.55))
                Spacer()
                Text("NEW")
                    .font(Font.custom("Inter", size: 10))
                    .tracking(2)
                    .foregroundColor(.spIvory.opacity(0.55))
            }

            Spacer().frame(height: 18)

            (
                Text("完整模考\n")
                    .font(Font.custom("Fraunces", size: 30))
                    .foregroundColor(.spIvory)
                + Text("多维诊断报告")
                    .font(Font.custom("Fraunces-Italic", size: 30))
                    .foregroundColor(.spAccent)
            )
            .lineSpacing(4)
            .tracking(-0.5)

            Spacer().frame(height: 16)

            Text("Part 1 · 2 · 3 全真流程 · 11 分钟\n完成后自动生成 6 维度评分与改进建议")
                .font(Font.custom("Inter", size: 13))
                .lineSpacing(4)
                .foregroundColor(.spIvory.opacity(0.65))

            Spacer().frame(height: 20)
            Rectangle().fill(Color.spIvory.opacity(0.15)).frame(height: 1)
            Spacer().frame(height: 16)

            HStack(alignment: .center, spacing: 20) {
                DarkStat(label: "已完成", value: "8", accent: false)
                DarkStat(label: "最高分", value: "6.5", accent: true)
                DarkStat(label: "平均", value: "5.8", accent: false)
                Spacer()
                HStack(spacing: 6) {
                    Text("开始模考")
                        .font(Font.custom("Inter", size: 13).weight(.semibold))
                        .foregroundColor(.spPrimary)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 11))
                        .foregroundColor(.spPrimary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.spIvory)
                .clipShape(Capsule())
            }
        }
        .padding(22)
        .background(Color.spPrimary)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
    }
}

/// 票根样式 —— 左 sienna 大数字 + 右正文
private struct MockTicket: View {
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            // 左票根
            VStack(alignment: .leading) {
                Eyebrow("MOCK", color: .spIvory.opacity(0.7))
                Spacer()
                VStack(alignment: .leading, spacing: 2) {
                    Text("11")
                        .font(Font.custom("Fraunces", size: 52))
                        .tracking(-1)
                        .foregroundColor(.spIvory)
                    Text("minutes")
                        .font(Font.custom("Inter", size: 11))
                        .foregroundColor(.spIvory.opacity(0.8))
                }
            }
            .frame(width: 96, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 20)
            .background(Color.spAccent)

            // 右正文
            VStack(alignment: .leading, spacing: 0) {
                Eyebrow("完整模考 · MOCK EXAM")
                Spacer().frame(height: 8)
                Text("IELTS Speaking\nFull Test")
                    .font(Font.custom("Fraunces", size: 22))
                    .tracking(-0.3)
                    .foregroundColor(.spPrimary)
                    .lineSpacing(3)
                Spacer().frame(height: 10)
                Text("Part 1 · 2 · 3 / 6 维度评分报告")
                    .font(Font.custom("Inter", size: 12))
                    .foregroundColor(.spMuted)
                    .lineSpacing(4)
                Spacer().frame(height: 14)
                HStack {
                    Text("NEXT AVAILABLE · NOW")
                        .font(Font.custom("Inter", size: 11))
                        .tracking(1)
                        .foregroundColor(.spMuted)
                    Spacer()
                    Text("入场")
                        .font(Font.custom("Inter", size: 12).weight(.semibold))
                        .foregroundColor(.spAccent)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 11))
                        .foregroundColor(.spAccent)
                }
            }
            .padding(.leading, 22)
            .padding(.trailing, 18)
            .padding(.vertical, 18)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color.spIvory)
        .overlay(
            RoundedRectangle(cornerRadius: 6)
                .stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
    }
}

/// 示意图样式 —— P1 / P2 / P3 三档 schematic
private struct MockDiagram: View {
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Eyebrow("模考 · MOCK EXAM")
                Spacer()
                Text("FIG. 01")
                    .font(Font.custom("Inter", size: 10))
                    .tracking(2)
                    .foregroundColor(.spMuted)
            }
            Spacer().frame(height: 14)

            (
                Text("11-minute full mock\n")
                    .font(Font.custom("Fraunces", size: 26))
                    .foregroundColor(.spPrimary)
                + Text("with 6-axis report.")
                    .font(Font.custom("Fraunces-Italic", size: 26))
                    .foregroundColor(.spAccent)
            )
            .lineSpacing(4)

            Spacer().frame(height: 22)

            HStack(alignment: .center, spacing: 0) {
                DiagramStage(label: "P1", duration: "4–5 min", weight: 1, highlighted: false)
                Rectangle().fill(Color.spLine).frame(width: 8, height: 1)
                DiagramStage(label: "P2", duration: "3–4 min", weight: 1.6, highlighted: true)
                Rectangle().fill(Color.spLine).frame(width: 8, height: 1)
                DiagramStage(label: "P3", duration: "4–5 min", weight: 1, highlighted: false)
            }

            Spacer().frame(height: 16)
            HStack {
                (
                    Text("上次 ")
                        .foregroundColor(.spMuted)
                    + Text("6.0")
                        .font(Font.custom("Inter", size: 12).weight(.semibold))
                        .foregroundColor(.spPrimary)
                    + Text(" · 提升空间 1.0")
                        .foregroundColor(.spMuted)
                )
                .font(Font.custom("Inter", size: 12))

                Spacer()
                HStack(spacing: 6) {
                    Text("Begin")
                        .font(Font.custom("Inter", size: 12).weight(.semibold))
                        .foregroundColor(.spIvory)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 10))
                        .foregroundColor(.spIvory)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 9)
                .background(Color.spPrimary)
                .clipShape(Capsule())
                .onTapGesture(perform: onTap)
            }
        }
        .padding(22)
        .background(Color.spIvory)
        .overlay(
            RoundedRectangle(cornerRadius: 6)
                .stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 6))
    }
}

private struct DiagramStage: View {
    let label: String
    let duration: String
    let weight: CGFloat
    let highlighted: Bool

    var body: some View {
        VStack(spacing: 2) {
            Text(label)
                .font(Font.custom("Fraunces", size: 14))
                .foregroundColor(highlighted ? .spAccent : .spPrimary)
            Text(duration)
                .font(Font.custom("Inter", size: 10))
                .foregroundColor(.spMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .padding(.horizontal, 6)
        .background(highlighted ? Color.spAccentSoft : Color.clear)
        .overlay(
            RoundedRectangle(cornerRadius: 3)
                .stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 3))
        .layoutPriority(weight)
    }
}

private struct DarkStat: View {
    let label: String
    let value: String
    let accent: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(Font.custom("Inter", size: 10))
                .tracking(1.8)
                .foregroundColor(.spIvory.opacity(0.55))
            Text(value)
                .font(Font.custom("Fraunces", size: 20))
                .foregroundColor(accent ? .spAccent : .spIvory)
        }
    }
}

// MARK: - Practice list

private struct PracticeListSection: View {
    let onTap: (PracticeMode) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Eyebrow("其他练习模块")
            Spacer().frame(height: 14)
            Rectangle().fill(Color.spLine).frame(height: 1)

            PracticeRow(num: "02", title: "AI 对话", en: "Dialogue with AI Examiner",
                        desc: "模拟真实口试，实时评分与追问",
                        systemIcon: "mic.fill") { onTap(.conversation) }
            PracticeRow(num: "03", title: "朗读", en: "Reading Aloud",
                        desc: "评测发音、流利度与语调",
                        systemIcon: "book.fill") { onTap(.readAloud) }
            PracticeRow(num: "04", title: "跟读", en: "Shadowing",
                        desc: "对照标准发音逐句纠正",
                        systemIcon: "waveform") { onTap(.followRead) }
        }
        .padding(.horizontal, 24)
    }
}

private struct PracticeRow: View {
    let num: String
    let title: String
    let en: String
    let desc: String
    let systemIcon: String
    let onTap: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 0) {
                Text(num)
                    .font(Font.custom("Fraunces", size: 22))
                    .foregroundColor(.spMuted)
                    .frame(width: 32, alignment: .leading)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(alignment: .lastTextBaseline, spacing: 10) {
                        Text(title)
                            .font(Font.custom("Inter", size: 16).weight(.semibold))
                            .foregroundColor(.spPrimary)
                        Text(en)
                            .font(Font.custom("Fraunces-Italic", size: 13))
                            .foregroundColor(.spMuted)
                    }
                    Text(desc)
                        .font(Font.custom("Inter", size: 12))
                        .foregroundColor(.spMuted)
                }
                Spacer()
                Image(systemName: systemIcon)
                    .font(.system(size: 16))
                    .foregroundColor(.spPrimary.opacity(0.7))
                    .padding(.trailing, 10)
                Image(systemName: "arrow.right")
                    .font(.system(size: 13))
                    .foregroundColor(.spMuted)
            }
            .padding(.vertical, 16)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)

            Rectangle().fill(Color.spLine).frame(height: 1)
        }
    }
}

// MARK: - Today's recommendation

private struct TodaysRecommendation: View {
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Eyebrow("今日推荐")
            Spacer().frame(height: 14)

            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(alignment: .center, spacing: 8) {
                        Text("IELTS · PART 2")
                            .font(Font.custom("Inter", size: 10))
                            .tracking(1.5)
                            .foregroundColor(.spMoss)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .overlay(
                                RoundedRectangle(cornerRadius: 2)
                                    .stroke(Color.spMoss, lineWidth: 1)
                            )
                        Text("约 4 分钟")
                            .font(Font.custom("Inter", size: 11))
                            .foregroundColor(.spMuted)
                    }
                    Spacer().frame(height: 8)
                    Text("Describe a skill you\nwould like to learn.")
                        .font(Font.custom("Fraunces", size: 20))
                        .foregroundColor(.spPrimary)
                        .lineSpacing(6)
                    Spacer().frame(height: 10)
                    Text("高频话题 · 上次你在 \"structure\" 上扣分较多")
                        .font(Font.custom("Inter", size: 13))
                        .foregroundColor(.spMuted)
                        .lineSpacing(6)
                }

                ZStack {
                    Circle().fill(Color.spPrimary)
                    Image(systemName: "arrow.right")
                        .font(.system(size: 14))
                        .foregroundColor(.spIvory)
                }
                .frame(width: 44, height: 44)
                .onTapGesture(perform: onTap)
            }
            .padding(20)
            .background(Color.spIvory)
            .overlay(
                RoundedRectangle(cornerRadius: 4)
                    .stroke(Color.spLine, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 4))
        }
        .padding(.horizontal, 24)
    }
}

// MARK: - Assignments

private struct AssignmentsSection: View {
    let pending: [String]
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .bottom) {
                Eyebrow("待完成作业")
                Spacer()
                Text(pending.isEmpty ? "暂无" : "共 \(pending.count) 项")
                    .font(Font.custom("Inter", size: 11))
                    .foregroundColor(.spMuted)
            }
            Spacer().frame(height: 10)

            if pending.isEmpty {
                Text("暂无待完成作业")
                    .font(Font.custom("Fraunces-Italic", size: 14))
                    .foregroundColor(.spMuted)
                    .padding(.vertical, 16)
            } else {
                ForEach(Array(pending.enumerated()), id: \.offset) { idx, title in
                    AssignmentRow(
                        date: monthDayLabel(offsetDays: idx),
                        teacher: "老师",
                        title: title,
                        due: idx == 0 ? "明天 22:00" : "本周日",
                        onTap: onTap
                    )
                }
            }
        }
        .padding(.horizontal, 24)
    }

    private func monthDayLabel(offsetDays: Int) -> String {
        let cal = Calendar.current
        let d = cal.date(byAdding: .day, value: offsetDays + 1, to: Date()) ?? Date()
        let comp = cal.dateComponents([.month, .day], from: d)
        return String(format: "%02d·%02d", comp.month ?? 1, comp.day ?? 1)
    }
}

private struct AssignmentRow: View {
    let date: String
    let teacher: String
    let title: String
    let due: String
    let onTap: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 14) {
                Text(date)
                    .font(Font.custom("Fraunces", size: 16))
                    .tracking(-0.3)
                    .foregroundColor(.spPrimary)
                    .frame(width: 52, alignment: .leading)
                Rectangle().fill(Color.spLine).frame(width: 1, height: 28)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(Font.custom("Inter", size: 14))
                        .foregroundColor(.spPrimary)
                        .lineLimit(1)
                    Text("\(teacher) · 截止 \(due)")
                        .font(Font.custom("Inter", size: 11))
                        .foregroundColor(.spMuted)
                }
                Spacer()
            }
            .padding(.vertical, 14)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)

            Rectangle().fill(Color.spLine).frame(height: 1)
        }
    }
}

// MARK: - Quote footer

private struct QuoteFooter: View {
    let edition: String

    var body: some View {
        VStack(spacing: 8) {
            Text("\"The only way to learn a language is to fall\nin love with it.\"")
                .font(Font.custom("Fraunces-Italic", size: 13))
                .foregroundColor(.spMuted)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
            Text("— № \(edition) —")
                .font(Font.custom("Inter", size: 10))
                .tracking(2)
                .foregroundColor(.spMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 24)
    }
}

// MARK: - Style sheet & helpers

private struct HomeStyleSheet: View {
    let current: HomeStyle
    let onPick: (HomeStyle) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Spacer().frame(height: 8)
            Eyebrow("首页风格 · HOME STYLE")
            Spacer().frame(height: 14)
            Text("dev · 连点三下顶部 No.XXX 即可再次打开")
                .font(Font.custom("Fraunces-Italic", size: 11))
                .foregroundColor(.spMuted)
            Spacer().frame(height: 16)

            ForEach(HomeStyle.allCases) { style in
                StyleRow(style: style, selected: style == current, onTap: { onPick(style) })
            }
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 24)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct StyleRow: View {
    let style: HomeStyle
    let selected: Bool
    let onTap: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 0) {
                VStack(alignment: .leading, spacing: 2) {
                    HStack(spacing: 8) {
                        Text(style.label)
                            .font(Font.custom("Inter", size: 15).weight(.semibold))
                            .foregroundColor(.spPrimary)
                        if !style.available {
                            Text("占位")
                                .font(Font.custom("Inter", size: 10))
                                .foregroundColor(.spMuted)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 1)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 2)
                                        .stroke(Color.spLine, lineWidth: 1)
                                )
                        }
                    }
                    Text(style.subtitle)
                        .font(Font.custom("Inter", size: 12))
                        .foregroundColor(.spMuted)
                }
                Spacer()
                if selected {
                    ZStack {
                        Circle().fill(Color.spAccent)
                        Image(systemName: "checkmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.spIvory)
                    }
                    .frame(width: 22, height: 22)
                } else {
                    Circle()
                        .strokeBorder(Color.spLine, lineWidth: 1)
                        .frame(width: 22, height: 22)
                }
            }
            .padding(.vertical, 14)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)
            Rectangle().fill(Color.spLine).frame(height: 1)
        }
    }
}

private struct UnavailableHint: View {
    let style: HomeStyle

    var body: some View {
        Text("「\(style.label)」UI 待开发，暂用编辑风 · 完整卡 占位")
            .font(Font.custom("Inter", size: 11))
            .foregroundColor(.spAccent)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.spAccentSoft)
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .padding(.horizontal, 24)
    }
}

// MARK: - Eyebrow

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

#Preview {
    HomeView()
        .environmentObject(AppCoordinator())
}

import SwiftUI

/// 进度 Tab —— editorial 风格（AppCoordinator 里仍用 `ProgressView()` 引用）
///
/// 结构对照 Android `features/progress/ProgressScreen.kt`：
///   Editorial hero → 3 stat cards → 14-day trend → 6-dim bars →
///   Activity heatmap → Next milestone dark card → Review entries
struct ProgressView: View {

    @StateObject private var viewModel = ProgressViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Hero()
                    StatCardsRow(
                        totalSessions: viewModel.stats.totalSessions,
                        avgScore: averageScore,
                        streakDays: viewModel.stats.streakDays
                    )
                    .padding(.top, 24)

                    TrendChartSection(points: viewModel.scoreHistory)
                        .padding(.top, 32)

                    SixDimensionSection(scores: viewModel.dimensionScores)
                        .padding(.top, 32)

                    ActivityLedger(streak: viewModel.stats.streakDays)
                        .padding(.top, 32)

                    NextMilestone(avgScore: averageScore)
                        .padding(.top, 28)

                    ReviewEntries()
                        .padding(.top, 32)
                        .padding(.bottom, 32)
                }
            }
            .background(Color.spBackground)
            .toolbar(.hidden, for: .navigationBar)
            .task { await viewModel.fetchProgress() }
        }
    }

    private var averageScore: Double {
        guard !viewModel.scoreHistory.isEmpty else { return 0 }
        let sum = viewModel.scoreHistory.reduce(0, +)
        return sum / Double(viewModel.scoreHistory.count)
    }
}

// MARK: - Hero

private struct Hero: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Eyebrow("PROGRESS · 进度")
                Spacer()
                Text("EDITION № \(editionNumber())")
                    .font(Font.custom("Inter", size: 11))
                    .foregroundColor(.spMuted)
            }
            Spacer().frame(height: 14)

            (
                Text("From 5.0 to 6.5\n")
                    .font(Font.custom("Fraunces", size: 32))
                    .foregroundColor(.spPrimary)
                + Text("— in 3 weeks.")
                    .font(Font.custom("Fraunces-Italic", size: 32))
                    .foregroundColor(.spAccent)
            )
            .lineSpacing(4)

            Spacer().frame(height: 12)
            Text("跟踪你每天的练习、评分变化与薄弱维度。")
                .font(Font.custom("Inter", size: 13))
                .foregroundColor(.spMuted)
        }
        .padding(.horizontal, 24)
        .padding(.top, 16)
    }

    private func editionNumber() -> String {
        let day = Calendar.current.ordinality(of: .day, in: .year, for: Date()) ?? 1
        return String(format: "%03d", day)
    }
}

// MARK: - Stat cards

private struct StatCardsRow: View {
    let totalSessions: Int
    let avgScore: Double
    let streakDays: Int

    var body: some View {
        HStack(spacing: 10) {
            StatCard(label: "练习次数", value: "\(totalSessions)", unit: "次", accent: false)
            StatCard(label: "平均分", value: String(format: "%.1f", avgScore), unit: nil, accent: true)
            StatCard(label: "连续", value: "\(streakDays)", unit: "天", accent: false)
        }
        .padding(.horizontal, 24)
    }
}

private struct StatCard: View {
    let label: String
    let value: String
    let unit: String?
    let accent: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Eyebrow(label)
            HStack(alignment: .bottom, spacing: 4) {
                Text(value)
                    .font(Font.custom("Fraunces", size: 32))
                    .tracking(-0.5)
                    .foregroundColor(accent ? .spAccent : .spPrimary)
                if let u = unit {
                    Text(u)
                        .font(Font.custom("Inter", size: 12))
                        .foregroundColor(.spMuted)
                        .padding(.bottom, 4)
                }
            }
        }
        .padding(.horizontal, 14)
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

// MARK: - Trend chart (14-day)

private struct TrendChartSection: View {
    let points: [Double]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Eyebrow("14-DAY TREND · 近 14 天")
            Spacer().frame(height: 14)

            ZStack(alignment: .topLeading) {
                Rectangle().fill(Color.spIvory)
                // 内边距
                TrendCanvas(points: normalized)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 18)
            }
            .frame(height: 140)
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(Color.spLine, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10))

            Spacer().frame(height: 8)
            HStack {
                Text("今日 \(points.last.map { String(format: "%.1f", $0) } ?? "—")")
                    .font(Font.custom("Inter", size: 11))
                    .foregroundColor(.spMuted)
                Spacer()
                Text("MAX \(points.max().map { String(format: "%.1f", $0) } ?? "—")")
                    .font(Font.custom("Inter", size: 11))
                    .foregroundColor(.spMuted)
            }
        }
        .padding(.horizontal, 24)
    }

    /// 取最近 14 个点，长度不足时前面用第一个补齐
    private var normalized: [Double] {
        let tail = Array(points.suffix(14))
        if tail.count >= 14 { return tail }
        let pad = Array(repeating: tail.first ?? 0, count: 14 - tail.count)
        return pad + tail
    }
}

private struct TrendCanvas: View {
    let points: [Double]

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let maxV = (points.max() ?? 1) + 1
            let minV = max((points.min() ?? 0) - 1, 0)
            let range = max(maxV - minV, 0.001)

            // 基线
            Path { p in
                p.move(to: CGPoint(x: 0, y: h))
                p.addLine(to: CGPoint(x: w, y: h))
            }
            .stroke(Color.spLine, lineWidth: 1)

            // 填充区域
            Path { p in
                guard !points.isEmpty else { return }
                for (i, v) in points.enumerated() {
                    let x = w * CGFloat(i) / CGFloat(max(points.count - 1, 1))
                    let y = h - h * CGFloat((v - minV) / range)
                    if i == 0 { p.move(to: CGPoint(x: x, y: y)) }
                    else { p.addLine(to: CGPoint(x: x, y: y)) }
                }
                p.addLine(to: CGPoint(x: w, y: h))
                p.addLine(to: CGPoint(x: 0, y: h))
                p.closeSubpath()
            }
            .fill(Color.spAccent.opacity(0.12))

            // 折线
            Path { p in
                guard !points.isEmpty else { return }
                for (i, v) in points.enumerated() {
                    let x = w * CGFloat(i) / CGFloat(max(points.count - 1, 1))
                    let y = h - h * CGFloat((v - minV) / range)
                    if i == 0 { p.move(to: CGPoint(x: x, y: y)) }
                    else { p.addLine(to: CGPoint(x: x, y: y)) }
                }
            }
            .stroke(Color.spAccent, style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))

            // 末点高亮
            if let last = points.last {
                let x = w
                let y = h - h * CGFloat((last - minV) / range)
                Circle()
                    .strokeBorder(Color.spAccent.opacity(0.3), lineWidth: 4)
                    .background(Circle().fill(Color.spAccent))
                    .frame(width: 10, height: 10)
                    .position(x: x, y: y)
            }
        }
    }
}

// MARK: - 6 dimension bars

private struct SixDimensionSection: View {
    let scores: [String: Double]

    private let order = ["发音", "语法", "流利度", "词汇", "连贯性", "总体"]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Eyebrow("SIX AXES · 六维度")
            Spacer().frame(height: 14)

            VStack(spacing: 12) {
                ForEach(order, id: \.self) { key in
                    let raw = scores[key] ?? scores["总体"] ?? averageOfKnown()
                    DimensionBar(label: key, value: raw)
                }
            }
        }
        .padding(.horizontal, 24)
    }

    private func averageOfKnown() -> Double {
        let vals = scores.values
        guard !vals.isEmpty else { return 0 }
        return vals.reduce(0, +) / Double(vals.count)
    }
}

private struct DimensionBar: View {
    let label: String
    /// value 范围 0.0-1.0
    let value: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label)
                    .font(Font.custom("Inter", size: 12))
                    .foregroundColor(.spPrimary)
                Spacer()
                Text(String(format: "%.0f", value * 100))
                    .font(Font.custom("Fraunces-Italic", size: 13))
                    .foregroundColor(.spMuted)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Rectangle().fill(Color.spLine)
                    Rectangle()
                        .fill(value >= 0.7 ? Color.spMoss : Color.spAccent)
                        .frame(width: geo.size.width * CGFloat(min(max(value, 0), 1)))
                }
                .clipShape(Capsule())
            }
            .frame(height: 4)
        }
    }
}

// MARK: - Activity heatmap 4×7

private struct ActivityLedger: View {
    let streak: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Eyebrow("ACTIVITY · 近 4 周")
            Spacer().frame(height: 14)

            VStack(spacing: 6) {
                ForEach(0..<4, id: \.self) { row in
                    HStack(spacing: 6) {
                        ForEach(0..<7, id: \.self) { col in
                            let idx = row * 7 + col
                            let active = idx < streak
                            Rectangle()
                                .fill(active ? Color.spAccent.opacity(0.85) : Color.spLine)
                                .frame(height: 18)
                                .clipShape(RoundedRectangle(cornerRadius: 2))
                                .frame(maxWidth: .infinity)
                        }
                    }
                }
            }

            Spacer().frame(height: 10)
            HStack {
                Text("连续 \(streak) 天")
                    .font(Font.custom("Inter", size: 11))
                    .foregroundColor(.spMuted)
                Spacer()
                HStack(spacing: 4) {
                    Text("少").font(Font.custom("Inter", size: 10)).foregroundColor(.spMuted)
                    ForEach([0.15, 0.35, 0.6, 0.85], id: \.self) { a in
                        Rectangle()
                            .fill(Color.spAccent.opacity(a))
                            .frame(width: 10, height: 10)
                            .clipShape(RoundedRectangle(cornerRadius: 2))
                    }
                    Text("多").font(Font.custom("Inter", size: 10)).foregroundColor(.spMuted)
                }
            }
        }
        .padding(.horizontal, 24)
    }
}

// MARK: - Next milestone dark card

private struct NextMilestone: View {
    let avgScore: Double

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Eyebrow("NEXT · 下一里程碑", color: .spIvory.opacity(0.55))
                Text("BAND 7.0")
                    .font(Font.custom("Fraunces", size: 26))
                    .foregroundColor(.spIvory)
                let gap = max(7.0 - avgScore, 0)
                Text("距目标还差 \(String(format: "%.1f", gap)) 分")
                    .font(Font.custom("Inter", size: 12))
                    .foregroundColor(.spIvory.opacity(0.55))
            }
            Spacer()
            ZStack {
                Circle().strokeBorder(Color.spIvory.opacity(0.4), lineWidth: 1)
                Image(systemName: "arrow.right")
                    .font(.system(size: 14))
                    .foregroundColor(.spIvory)
            }
            .frame(width: 40, height: 40)
        }
        .padding(20)
        .background(Color.spPrimary)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, 24)
    }
}

// MARK: - Review entries (历史回听 / 错题本 / 通知)

private struct ReviewEntries: View {
    var body: some View {
        VStack(spacing: 0) {
            Eyebrow("REVIEW · 回顾")
                .frame(maxWidth: .infinity, alignment: .leading)
            Spacer().frame(height: 14)

            NavigationLink { HistoryTimelineView() } label: {
                entry(num: "A", title: "历史回听", desc: "按天查看练习记录并回听",
                      icon: "waveform")
            }
            NavigationLink { NotebookView() } label: {
                entry(num: "B", title: "错题本 / 生词本", desc: "低分单词 + 间隔复习",
                      icon: "book")
            }
            NavigationLink { NotificationsView() } label: {
                entry(num: "C", title: "通知中心", desc: "作业 / 批改 / 提醒",
                      icon: "bell")
            }
        }
        .padding(.horizontal, 24)
    }

    private func entry(num: String, title: String, desc: String, icon: String) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 14) {
                Text(num)
                    .font(Font.custom("Fraunces-Italic", size: 22))
                    .foregroundColor(.spMuted)
                    .frame(width: 24, alignment: .leading)
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(Font.custom("Inter", size: 15).weight(.semibold))
                        .foregroundColor(.spPrimary)
                    Text(desc)
                        .font(Font.custom("Inter", size: 12))
                        .foregroundColor(.spMuted)
                }
                Spacer()
                Image(systemName: icon)
                    .font(.system(size: 14))
                    .foregroundColor(.spPrimary.opacity(0.7))
                Image(systemName: "arrow.right")
                    .font(.system(size: 12))
                    .foregroundColor(.spMuted)
            }
            .padding(.vertical, 16)
            Rectangle().fill(Color.spLine).frame(height: 1)
        }
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
    ProgressView()
}

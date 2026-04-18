import SwiftUI

/// 03 · 目标分 — IELTS Band 刻度 4.0–9.0 / TOEFL 口语 0–30。
/// 默认 IELTS 样式；若选 TOEFL 则快选 chip 替换成 20/23/26/28。
struct OnbTargetView: View {

    @ObservedObject var vm: OnboardingViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 16) {
                Text("YOUR TARGET · 02").font(.spEyebrow).foregroundColor(.spMuted)
                VStack(alignment: .leading, spacing: -4) {
                    Text("Aim for").font(.spSerif(34)).foregroundColor(.spPrimary)
                    HStack(alignment: .lastTextBaseline, spacing: 10) {
                        Text(displayTarget)
                            .font(.spSerif(68, italic: true))
                            .foregroundColor(.spAccent)
                        Text(unitLabel)
                            .font(.system(size: 13))
                            .foregroundColor(.spMuted)
                    }
                }
                Text(adviceText)
                    .font(.system(size: 13))
                    .foregroundColor(.spMuted)
                    .frame(maxWidth: 280, alignment: .leading)
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)

            scale
                .padding(.horizontal, 24)
                .padding(.top, 44)

            quickChips
                .padding(.horizontal, 24)
                .padding(.top, 34)

            adviceCard
                .padding(.horizontal, 24)
                .padding(.top, 22)

            Spacer()

            OnbPrimaryButton(
                title: "继续 · 设置考期",
                enabled: vm.targetScore != nil && vm.isTargetValid(vm.targetScore ?? 0)
            ) {
                Task {
                    await vm.patchCurrent()
                    vm.next()
                }
            }
            .padding(24)
        }
    }

    // MARK: - Display

    private var displayTarget: String {
        guard let t = vm.targetScore else { return "—" }
        if vm.examType == .toefl {
            return String(format: "%.0f", t)
        }
        return String(format: "%.1f", t)
    }

    private var unitLabel: String {
        vm.examType == .toefl ? "SPEAKING" : "BAND"
    }

    private var adviceText: String {
        if vm.examType == .toefl {
            return "目标越具体，计划越可落地。口语 26 对应多数研究生项目最低要求。"
        }
        return "目标越具体，计划越可落地。Band 7.0 对应多数研究生申请的最低要求。"
    }

    // MARK: - Scale

    private var scale: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let min = scaleMin
            let max = scaleMax
            let stops = stopsCount
            let ticks = (0..<stops).map { i -> Double in
                min + (max - min) * Double(i) / Double(stops - 1)
            }

            ZStack(alignment: .leading) {
                ForEach(ticks, id: \.self) { v in
                    let x = (v - min) / (max - min) * w
                    let major = isMajor(v)
                    let isTarget = (vm.targetScore.map { abs($0 - v) < 1e-3 }) ?? false
                    Path { p in
                        let top: CGFloat = major ? (isTarget ? 0 : 10) : 14
                        let bottom: CGFloat = 32
                        p.move(to: CGPoint(x: x, y: top))
                        p.addLine(to: CGPoint(x: x, y: bottom))
                    }
                    .stroke(
                        isTarget ? Color.spAccent : Color.spLine,
                        lineWidth: isTarget ? 3 : 1
                    )
                    if major {
                        Text(String(format: v.truncatingRemainder(dividingBy: 1) == 0 ? "%.0f" : "%.1f", v))
                            .font(.spSerif(11))
                            .foregroundColor(isTarget ? .spAccent : .spMuted)
                            .position(x: x, y: 46)
                    }
                }
            }
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { g in
                        let x = Swift.max(0, Swift.min(w, g.location.x))
                        let raw = min + (max - min) * Double(x / w)
                        vm.targetScore = (raw * 2).rounded() / 2 // IELTS 0.5 step
                        if vm.examType == .toefl {
                            vm.targetScore = (vm.targetScore ?? 0).rounded()
                        }
                    }
            )
        }
        .frame(height: 68)
    }

    private var scaleMin: Double { vm.examType == .toefl ? 0 : 4 }
    private var scaleMax: Double { vm.examType == .toefl ? 30 : 9 }
    private var stopsCount: Int { vm.examType == .toefl ? 16 : 11 } // TOEFL 0-30 步 2；IELTS 4-9 步 0.5

    private func isMajor(_ v: Double) -> Bool {
        if vm.examType == .toefl {
            return v.truncatingRemainder(dividingBy: 5) == 0
        }
        return v.truncatingRemainder(dividingBy: 1) == 0
    }

    // MARK: - Quick chips

    private var quickChips: some View {
        let chips: [(String, String, Double)] = {
            if vm.examType == .toefl {
                return [
                    ("20", "本科申请", 20),
                    ("23", "多数本科", 23),
                    ("26", "研究生申请", 26),
                    ("28", "名校研究生", 28),
                ]
            }
            return [
                ("6.0", "大专/本科申请", 6.0),
                ("6.5", "多数本科", 6.5),
                ("7.0", "研究生申请", 7.0),
                ("7.5", "名校研究生", 7.5),
                ("8.0+", "移民/学术", 8.0),
            ]
        }()

        return OnbFlowLayout(spacing: 8) {
            ForEach(chips, id: \.0) { (label, sub, value) in
                Button {
                    vm.targetScore = value
                } label: {
                    HStack(spacing: 6) {
                        Text(label).font(.spSerif(12, italic: false))
                        Text(sub).font(.system(size: 10)).opacity(0.7)
                    }
                    .foregroundColor(isSelected(value) ? .spIvory : .spPrimary)
                    .padding(.horizontal, 14).padding(.vertical, 8)
                    .background(isSelected(value) ? Color.spPrimary : Color.spIvory)
                    .overlay(
                        Capsule().stroke(
                            isSelected(value) ? Color.spPrimary : Color.spLine,
                            lineWidth: 1
                        )
                    )
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func isSelected(_ v: Double) -> Bool {
        guard let target = vm.targetScore else { return false }
        return abs(target - v) < 1e-3
    }

    // MARK: - Advice card

    private var adviceCard: some View {
        HStack(spacing: 12) {
            Rectangle().fill(Color.spAccent).frame(width: 4).clipShape(Capsule())
            VStack(alignment: .leading, spacing: 6) {
                Text("SPEAKPRO 建议").font(.spEyebrow).foregroundColor(.spMuted)
                Text("从基线到目标，平均需要 6–8 周规律练习 · 每日 15 分钟即可。")
                    .font(.system(size: 12))
                    .foregroundColor(.spPrimary)
                    .lineSpacing(3)
            }
            Spacer()
        }
        .padding(14)
        .background(Color.spIvory)
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

/// 简易流式布局 —— 按宽度自动换行
struct OnbFlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var rows: [[CGSize]] = [[]]
        var currentWidth: CGFloat = 0
        var totalHeight: CGFloat = 0
        var rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if currentWidth + size.width + spacing > maxWidth, !rows[rows.count - 1].isEmpty {
                totalHeight += rowHeight + spacing
                rowHeight = 0
                currentWidth = 0
                rows.append([])
            }
            rows[rows.count - 1].append(size)
            currentWidth += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        totalHeight += rowHeight
        let maxRowWidth = rows.map { row in row.reduce(CGFloat(0)) { $0 + $1.width + spacing } }.max() ?? 0
        return CGSize(width: maxRowWidth, height: totalHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for sub in subviews {
            let size = sub.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            sub.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

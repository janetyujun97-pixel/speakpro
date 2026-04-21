import SwiftUI

/// 作业 Tab —— editorial hero + 待完成/已完成 双 tab
///
/// 结构对照 Android `features/homework/HomeworkListScreen.kt`
struct HomeworkListView: View {

    @StateObject private var viewModel = HomeworkViewModel()

    private var todoList: [HomeworkAssignment] {
        viewModel.assignments.filter { !$0.isCompleted && !$0.isSubmitted }
    }
    private var doneList: [HomeworkAssignment] {
        viewModel.assignments.filter { $0.isCompleted || $0.isSubmitted }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    // Hero
                    VStack(alignment: .leading, spacing: 0) {
                        HStack {
                            Eyebrow("HOMEWORK · 作业")
                            Spacer()
                            Text("WEEK \(currentWeekOfYear())")
                                .font(Font.custom("Inter", size: 11))
                                .foregroundColor(.spMuted)
                        }
                        Spacer().frame(height: 14)

                        (
                            Text("\(todoList.count) ")
                                .font(Font.custom("Fraunces", size: 36))
                                .foregroundColor(.spPrimary)
                            + Text("due,")
                                .font(Font.custom("Fraunces-Italic", size: 36))
                                .foregroundColor(.spMuted)
                        )
                        .lineSpacing(4)

                        let urgent = todoList.filter { isUrgent($0) }.count
                        Text(urgent > 0 ? "\(urgent) overdue soon." : "all on track.")
                            .font(Font.custom("Fraunces-Italic", size: 36))
                            .foregroundColor(.spAccent)

                        Spacer().frame(height: 12)

                        let total = todoList.reduce(0) { $0 + estimatePoints($1) }
                        Text("共 \(total) 分 · 已完成 \(doneList.count)/\(viewModel.assignments.count)")
                            .font(Font.custom("Inter", size: 13))
                            .foregroundColor(.spMuted)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 16)
                    .padding(.bottom, 20)

                    // Tabs
                    HStack(spacing: 24) {
                        tabLabel("待完成 · \(todoList.count)", tab: .pending)
                        tabLabel("已完成 · \(doneList.count)", tab: .completed)
                        Spacer()
                    }
                    .padding(.horizontal, 24)

                    Rectangle().fill(Color.spLine).frame(height: 1)
                        .padding(.horizontal, 24)

                    // List
                    let list = viewModel.selectedTab == .pending ? todoList : doneList
                    if list.isEmpty {
                        Text(viewModel.selectedTab == .pending
                             ? "太棒了，没有待完成作业"
                             : "还没有已完成的作业")
                            .font(Font.custom("Fraunces-Italic", size: 14))
                            .foregroundColor(.spMuted)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 30)
                    } else {
                        VStack(spacing: 0) {
                            ForEach(list) { hw in
                                NavigationLink(destination: HomeworkDetailView(assignment: hw)) {
                                    if viewModel.selectedTab == .pending {
                                        TodoRow(hw: hw)
                                    } else {
                                        DoneRow(hw: hw)
                                    }
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 24)
                    }

                    Spacer().frame(height: 24)
                }
            }
            .background(Color.spBackground)
            .toolbar(.hidden, for: .navigationBar)
            .task { await viewModel.fetchAssignments() }
            .onReceive(NotificationCenter.default.publisher(for: .switchToPendingHomework)) { _ in
                viewModel.selectedTab = .pending
            }
        }
    }

    @ViewBuilder
    private func tabLabel(_ label: String, tab: HomeworkViewModel.HomeworkTab) -> some View {
        let on = viewModel.selectedTab == tab
        VStack(spacing: 0) {
            Text(label)
                .font(Font.custom("Inter", size: 13).weight(on ? .semibold : .regular))
                .foregroundColor(on ? .spPrimary : .spMuted)
                .padding(.vertical, 12)
            Rectangle().fill(on ? Color.spPrimary : Color.clear).frame(height: 2)
        }
        .contentShape(Rectangle())
        .onTapGesture { viewModel.selectedTab = tab }
    }
}

// MARK: - Rows

private struct TodoRow: View {
    let hw: HomeworkAssignment

    var body: some View {
        let typeLabel = guessType(hw)
        let urgent = isUrgent(hw)
        let accent = typeLabel == "模考"

        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center) {
                Text(typeLabel)
                    .font(Font.custom("Inter", size: 10).weight(.semibold))
                    .tracking(1.5)
                    .foregroundColor(accent ? .spAccent : .spPrimary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .overlay(
                        RoundedRectangle(cornerRadius: 2)
                            .stroke(accent ? Color.spAccent : Color.spPrimary, lineWidth: 1)
                    )

                if urgent {
                    Text("● URGENT")
                        .font(Font.custom("Inter", size: 10).weight(.semibold))
                        .tracking(1.5)
                        .foregroundColor(.spAccent)
                        .padding(.leading, 8)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 0) {
                    Text("+\(estimatePoints(hw))")
                        .font(Font.custom("Fraunces-Italic", size: 24))
                        .foregroundColor(.spMuted)
                    Text("POINTS")
                        .font(Font.custom("Inter", size: 9))
                        .tracking(1)
                        .foregroundColor(.spMuted)
                }
            }
            Spacer().frame(height: 10)
            Text(hw.title)
                .font(Font.custom("Fraunces", size: 18))
                .foregroundColor(.spPrimary)
                .lineSpacing(6)
                .frame(maxWidth: .infinity, alignment: .leading)
            Spacer().frame(height: 8)

            (
                Text("\(hw.teacherName) · 截止 ")
                    .foregroundColor(.spMuted)
                + Text(formatDue(hw.dueDate))
                    .font(Font.custom("Inter", size: 12).weight(.semibold))
                    .foregroundColor(urgent ? .spAccent : .spPrimary)
                + Text(" · \(estimateDuration(hw))")
                    .foregroundColor(.spMuted)
            )
            .font(Font.custom("Inter", size: 12))

            Spacer().frame(height: 12)

            HStack(spacing: 6) {
                Text("开始 \(typeLabel)")
                    .font(Font.custom("Inter", size: 12).weight(.semibold))
                    .foregroundColor(.spIvory)
                Image(systemName: "arrow.right")
                    .font(.system(size: 10))
                    .foregroundColor(.spIvory)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 9)
            .background(Color.spPrimary)
            .clipShape(Capsule())
        }
        .padding(.vertical, 18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(alignment: .bottom) {
            Rectangle().fill(Color.spLine).frame(height: 1)
        }
    }
}

private struct DoneRow: View {
    let hw: HomeworkAssignment

    var body: some View {
        let score = hw.submissions?.first(where: { $0.status == "graded" })?.teacherScore ?? 0

        HStack(alignment: .center, spacing: 14) {
            Text(formatShortDate(hw.dueDate))
                .font(Font.custom("Fraunces", size: 15))
                .foregroundColor(.spMuted)
                .frame(width: 48, alignment: .leading)
            Rectangle().fill(Color.spLine).frame(width: 1, height: 32)
            VStack(alignment: .leading, spacing: 2) {
                Text(guessType(hw).uppercased())
                    .font(Font.custom("Inter", size: 11))
                    .tracking(1)
                    .foregroundColor(.spMuted)
                Text(hw.title)
                    .font(Font.custom("Inter", size: 14).weight(.medium))
                    .foregroundColor(.spPrimary)
                    .lineLimit(1)
            }
            Spacer()
            Text(score > 0 ? String(format: "%.1f", score) : "—")
                .font(Font.custom("Fraunces", size: 20))
                .foregroundColor(score >= 6.5 ? .spMoss : .spPrimary)
        }
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(alignment: .bottom) {
            Rectangle().fill(Color.spLine).frame(height: 1)
        }
    }
}

// MARK: - Helpers (模型里没有明确 type/points/duration，按规则推断)

private func guessType(_ hw: HomeworkAssignment) -> String {
    let t = hw.title.lowercased()
    if hw.title.contains("模考") || t.contains("mock") { return "模考" }
    if hw.title.contains("朗读") || t.contains("reading") { return "朗读" }
    if hw.title.contains("跟读") || t.contains("shadow") { return "跟读" }
    return "AI 对话"
}

private func estimatePoints(_ hw: HomeworkAssignment) -> Int {
    let n = hw.questionIds?.count ?? 3
    return n * 10
}

private func estimateDuration(_ hw: HomeworkAssignment) -> String {
    switch guessType(hw) {
    case "模考": return "11 min"
    case "朗读": return "8 min"
    case "跟读": return "10 min"
    default:     return "15 min"
    }
}

private func formatDue(_ iso: String?) -> String {
    guard let iso = iso, !iso.isEmpty else { return "—" }
    let fmt = ISO8601DateFormatter()
    fmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    var d = fmt.date(from: iso)
    if d == nil {
        fmt.formatOptions = [.withInternetDateTime]
        d = fmt.date(from: iso)
    }
    guard let due = d else { return String(iso.prefix(10)) }
    let hours = Calendar.current.dateComponents([.hour], from: Date(), to: due).hour ?? 0
    if hours < 0 { return "已过期" }
    if hours < 24 {
        let tf = DateFormatter()
        tf.dateFormat = "HH:mm"
        return "明天 \(tf.string(from: due))"
    }
    if hours < 24 * 3 { return "本周日" }
    let mf = DateFormatter()
    mf.dateFormat = "MM·dd"
    return mf.string(from: due)
}

private func formatShortDate(_ iso: String?) -> String {
    guard let iso = iso, !iso.isEmpty else { return "—" }
    let fmt = ISO8601DateFormatter()
    fmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    var d = fmt.date(from: iso)
    if d == nil {
        fmt.formatOptions = [.withInternetDateTime]
        d = fmt.date(from: iso)
    }
    guard let date = d else { return "—" }
    let mf = DateFormatter()
    mf.dateFormat = "MM·dd"
    return mf.string(from: date)
}

private func isUrgent(_ hw: HomeworkAssignment) -> Bool {
    guard let iso = hw.dueDate else { return false }
    let fmt = ISO8601DateFormatter()
    fmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    var d = fmt.date(from: iso)
    if d == nil {
        fmt.formatOptions = [.withInternetDateTime]
        d = fmt.date(from: iso)
    }
    guard let due = d else { return false }
    let hours = Calendar.current.dateComponents([.hour], from: Date(), to: due).hour ?? 0
    return hours >= 0 && hours <= 48
}

private func currentWeekOfYear() -> Int {
    Calendar.current.component(.weekOfYear, from: Date())
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
    HomeworkListView()
}

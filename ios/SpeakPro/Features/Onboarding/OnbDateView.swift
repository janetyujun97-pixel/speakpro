import SwiftUI

/// 04 · 考期 — 当前月历 + 倒计天数。一期用 SwiftUI DatePicker 简化实现。
struct OnbDateView: View {

    @ObservedObject var vm: OnboardingViewModel

    @State private var displayMonth: Date = Date()

    private let calendar = Calendar(identifier: .gregorian)
    private let weekdayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "EEEE"
        f.locale = Locale(identifier: "en_US")
        return f
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 16) {
                Text("EXAM DATE · 03").font(.spEyebrow).foregroundColor(.spMuted)
                VStack(alignment: .leading, spacing: -4) {
                    Text("When is the").font(.spSerif(34)).foregroundColor(.spPrimary)
                    Text("real test?").font(.spSerif(34, italic: true)).foregroundColor(.spAccent)
                }
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)

            selectedHero
                .padding(.horizontal, 24)
                .padding(.top, 22)

            calendarGrid
                .padding(.horizontal, 24)
                .padding(.top, 20)

            Spacer()

            OnbPrimaryButton(
                title: "继续 · 水平自评",
                enabled: vm.examDate != nil
            ) {
                Task {
                    await vm.patchCurrent()
                    vm.next()
                }
            }
            .padding(24)
        }
    }

    // MARK: - Hero card

    private var selectedHero: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 6) {
                Text("SELECTED")
                    .font(.spEyebrow)
                    .foregroundColor(Color.spIvory.opacity(0.55))
                Text(heroDateLine)
                    .font(.spSerif(26))
                    .foregroundColor(.spIvory)
                Text(heroSubLine)
                    .font(.system(size: 11))
                    .foregroundColor(Color.spIvory.opacity(0.55))
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 0) {
                Text(daysLeftText)
                    .font(.spSerif(40))
                    .foregroundColor(.spIvory)
                Text("DAYS LEFT")
                    .font(.system(size: 9, weight: .semibold))
                    .tracking(1.5)
                    .foregroundColor(Color.spIvory.opacity(0.55))
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.spPrimary)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var heroDateLine: String {
        if let d = vm.examDate {
            let f = DateFormatter()
            f.dateFormat = "MMM d, yyyy"
            f.locale = Locale(identifier: "en_US")
            return f.string(from: d)
        }
        return "请选择考试日"
    }

    private var heroSubLine: String {
        if let d = vm.examDate {
            return weekdayFormatter.string(from: d).uppercased()
        }
        return "TAP A DATE BELOW"
    }

    private var daysLeftText: String {
        guard let d = vm.examDate else { return "—" }
        let today = calendar.startOfDay(for: Date())
        let picked = calendar.startOfDay(for: d)
        let days = calendar.dateComponents([.day], from: today, to: picked).day ?? 0
        return "\(max(0, days))"
    }

    // MARK: - Calendar grid

    private var calendarGrid: some View {
        VStack(spacing: 10) {
            monthNav
            weekdayRow
            daysGrid
        }
    }

    private var monthNav: some View {
        HStack {
            Button {
                if let d = calendar.date(byAdding: .month, value: -1, to: displayMonth) {
                    displayMonth = d
                }
            } label: {
                Image(systemName: "chevron.left").foregroundColor(.spMuted)
            }
            Spacer()
            Text(monthTitle(displayMonth))
                .font(.spSerif(18)).foregroundColor(.spPrimary)
            Spacer()
            Button {
                if let d = calendar.date(byAdding: .month, value: 1, to: displayMonth) {
                    displayMonth = d
                }
            } label: {
                Image(systemName: "chevron.right").foregroundColor(.spMuted)
            }
        }
    }

    private var weekdayRow: some View {
        HStack(spacing: 2) {
            ForEach(["S", "M", "T", "W", "T", "F", "S"], id: \.self) { w in
                Text(w)
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(1)
                    .foregroundColor(.spMuted)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var daysGrid: some View {
        let days = monthDays(for: displayMonth)
        let columns = Array(repeating: GridItem(.flexible(), spacing: 2), count: 7)
        return LazyVGrid(columns: columns, spacing: 4) {
            ForEach(Array(days.enumerated()), id: \.offset) { _, day in
                dayCell(day)
            }
        }
    }

    @ViewBuilder
    private func dayCell(_ day: Date?) -> some View {
        if let day = day {
            let selected = vm.examDate.map { calendar.isDate($0, inSameDayAs: day) } ?? false
            let past = day < calendar.startOfDay(for: Date())
            Button {
                if !past { vm.examDate = day }
            } label: {
                Text("\(calendar.component(.day, from: day))")
                    .font(.spSerif(13))
                    .foregroundColor(selected ? .spIvory : (past ? .spMuted : .spPrimary))
                    .opacity(past ? 0.4 : 1)
                    .frame(maxWidth: .infinity)
                    .aspectRatio(1, contentMode: .fit)
                    .background(selected ? Color.spAccent : Color.clear)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            .disabled(past)
            .buttonStyle(.plain)
        } else {
            Color.clear.aspectRatio(1, contentMode: .fit)
        }
    }

    private func monthTitle(_ d: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "MMMM yyyy"
        f.locale = Locale(identifier: "en_US")
        return f.string(from: d)
    }

    /// 返回一个长度为 42 的数组，null 表示 leading/trailing 空格
    private func monthDays(for date: Date) -> [Date?] {
        guard let range = calendar.range(of: .day, in: .month, for: date),
              let firstDay = calendar.date(from: calendar.dateComponents([.year, .month], from: date)) else {
            return []
        }
        let firstWeekday = calendar.component(.weekday, from: firstDay) - 1 // 0 = Sunday
        var cells: [Date?] = Array(repeating: nil, count: firstWeekday)
        for d in range {
            if let day = calendar.date(byAdding: .day, value: d - 1, to: firstDay) {
                cells.append(day)
            }
        }
        while cells.count < 42 { cells.append(nil) }
        return cells
    }
}

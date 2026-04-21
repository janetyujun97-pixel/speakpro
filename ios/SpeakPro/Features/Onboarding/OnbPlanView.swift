import SwiftUI

/// 08 · 学习计划 —— 展示服务端 finalize 返回的 studyPlan + 每周节奏。
struct OnbPlanView: View {

    @ObservedObject var vm: OnboardingViewModel

    private let schedule: [(day: String, activity: String, duration: String)] = [
        ("周一", "AI 对话", "15 min"),
        ("周二", "朗读 + 跟读", "12 min"),
        ("周三", "AI 对话", "15 min"),
        ("周四", "休息 · 轻听", "—"),
        ("周五", "朗读 + 跟读", "12 min"),
        ("周六", "完整模考", "15 min"),
        ("周日", "复盘 · 错题", "8 min"),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                headline
                    .padding(.horizontal, 24)
                    .padding(.top, 24)

                baselineSnapshot
                    .padding(.horizontal, 24)
                    .padding(.top, 18)

                weeklyRhythm
                    .padding(.horizontal, 24)
                    .padding(.top, 22)

                reminderCard
                    .padding(.horizontal, 24)
                    .padding(.top, 18)

                startButton
                    .padding(.horizontal, 24)
                    .padding(.top, 24)
                    .padding(.bottom, 24)
            }
        }
    }

    private var headline: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("YOUR PLAN · 07").font(.spEyebrow).foregroundColor(.spMuted)
            VStack(alignment: .leading, spacing: -2) {
                Text("From").font(.spSerif(30)).foregroundColor(.spPrimary)
                HStack(spacing: 0) {
                    Text("baseline")
                        .font(.spSerif(30, italic: true))
                        .foregroundColor(.spMuted)
                    Text(" to ")
                        .font(.spSerif(30))
                        .foregroundColor(.spPrimary)
                    Text(targetLabel)
                        .font(.spSerif(30, italic: true))
                        .foregroundColor(.spAccent)
                    Text(", \(weeksText).")
                        .font(.spSerif(30))
                        .foregroundColor(.spPrimary)
                }
            }
            Text(subtitleText).font(.system(size: 12)).foregroundColor(.spMuted)
        }
    }

    private var targetLabel: String {
        if let t = vm.targetScore {
            return vm.examType == .toefl ? String(format: "%.0f", t) : String(format: "%.1f", t)
        }
        return "目标"
    }

    private var weeksText: String {
        if let w = vm.studyPlan?.weeks { return "in \(w) weeks" }
        return "step by step"
    }

    private var subtitleText: String {
        var parts: [String] = []
        if let t = vm.targetScore {
            parts.append("目标：\(vm.examType == .toefl ? String(format: "%.0f", t) : String(format: "%.1f", t))")
        }
        if let d = vm.examDate {
            let f = DateFormatter()
            f.dateFormat = "M 月 d 日"
            f.locale = Locale(identifier: "zh_CN")
            parts.append("考期：\(f.string(from: d))")
        }
        return parts.joined(separator: " · ")
    }

    private var baselineSnapshot: some View {
        HStack(spacing: 16) {
            metric("FLUENCY", "—")
            metric("PRON.", "—")
            metric("GRAMMAR", "—")
            metric("VOCAB", "—")
        }
        .padding(14)
        .background(Color.spIvory)
        .overlay(
            RoundedRectangle(cornerRadius: 10).stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func metric(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.system(size: 9, weight: .semibold))
                .tracking(1)
                .foregroundColor(.spMuted)
            Text(value).font(.spSerif(18)).foregroundColor(.spPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var weeklyRhythm: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("WEEKLY RHYTHM · 每周节奏").font(.spEyebrow).foregroundColor(.spMuted)
            VStack(spacing: 0) {
                ForEach(Array(schedule.enumerated()), id: \.offset) { idx, row in
                    HStack(spacing: 14) {
                        Text(row.day)
                            .font(.spSerif(13, italic: true))
                            .foregroundColor(.spMuted)
                            .frame(width: 40, alignment: .leading)
                        Rectangle().fill(Color.spLine).frame(width: 1, height: 20)
                        Group {
                            if row.activity.contains("休息") {
                                Text(row.activity).italic()
                            } else {
                                Text(row.activity)
                            }
                        }
                        .font(.system(size: 13))
                        .foregroundColor(row.activity.contains("休息") ? .spMuted : .spPrimary)
                        Spacer()
                        Text(row.duration)
                            .font(.spSerif(11))
                            .foregroundColor(.spMuted)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    if idx < schedule.count - 1 {
                        Rectangle().fill(Color.spLine).frame(height: 1)
                    }
                }
            }
            .background(Color.spIvory)
            .overlay(
                RoundedRectangle(cornerRadius: 10).stroke(Color.spLine, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
    }

    private var reminderCard: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("每日提醒").font(.spEyebrow).foregroundColor(.spMuted)
                Text("19:00 · 工作日").font(.system(size: 13)).foregroundColor(.spPrimary)
            }
            Spacer()
            ZStack(alignment: .trailing) {
                Capsule().fill(Color.spAccent).frame(width: 40, height: 22)
                Circle().fill(Color.spIvory).frame(width: 18, height: 18).padding(2)
            }
        }
        .padding(14)
        .overlay(
            RoundedRectangle(cornerRadius: 10).stroke(Color.spLine, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private var startButton: some View {
        OnbPrimaryButton(title: "开始练习 · Start now", enabled: !vm.isFinalizing) {
            Task {
                // 如果还没 finalize（正常情况下在录音完成时已 finalize），这里兜底再调一次
                if vm.studyPlan == nil { await vm.finalize() }
                vm.completed = true
            }
        }
    }
}


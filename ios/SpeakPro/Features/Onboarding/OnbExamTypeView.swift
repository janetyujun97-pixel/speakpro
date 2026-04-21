import SwiftUI

/// 02 · 考试类型 — IELTS / TOEFL / GENERAL 三卡单选
struct OnbExamTypeView: View {

    @ObservedObject var vm: OnboardingViewModel

    private struct TypeOption {
        let type: OnbExamType
        let display: String       // 显示给用户的短字串
        let en: String            // 英文副标题
        let sub: String           // 中文说明
        let badge: String?
    }

    private let options: [TypeOption] = [
        .init(type: .ielts,
              display: "IELTS",
              en: "International English Language Testing",
              sub: "雅思口语 · Part 1 / 2 / 3",
              badge: "最常选"),
        .init(type: .toefl,
              display: "TOEFL",
              en: "Test of English as a Foreign Language",
              sub: "托福口语 · 4 个独立 / 综合任务",
              badge: nil),
        .init(type: .general,
              display: "GENERAL",
              en: "Daily conversation · business",
              sub: "日常英语 · 面试 · 商务",
              badge: nil),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 16) {
                Text("SELECT YOUR EXAM · 01").font(.spEyebrow).foregroundColor(.spMuted)
                VStack(alignment: .leading, spacing: -4) {
                    Text("What are you").font(.spSerif(34)).foregroundColor(.spPrimary)
                    Text("training for?")
                        .font(.spSerif(34, italic: true))
                        .foregroundColor(.spAccent)
                }
                Text("选择你要准备的考试类型 —— 我们会按这个做题目、评分和计划。")
                    .font(.system(size: 13))
                    .foregroundColor(.spMuted)
                    .frame(maxWidth: 280, alignment: .leading)
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(options, id: \.type) { opt in
                        card(opt)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 28)
            }

            OnbPrimaryButton(title: "继续", enabled: vm.examType != nil) {
                Task {
                    await vm.patchCurrent()
                    vm.next()
                }
            }
            .padding(24)
        }
    }

    private func card(_ opt: TypeOption) -> some View {
        let selected = vm.examType == opt.type
        return Button {
            vm.examType = opt.type
        } label: {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: 8) {
                        Text(opt.display)
                            .font(.spSerif(22))
                            .foregroundColor(selected ? .spIvory : .spPrimary)
                        if let badge = opt.badge {
                            Text(badge.uppercased())
                                .font(.system(size: 9, weight: .semibold))
                                .foregroundColor(.spIvory)
                                .padding(.horizontal, 8).padding(.vertical, 2)
                                .background(Color.spAccentWarm)
                        }
                    }
                    Text(opt.en)
                        .font(.spSerif(11, italic: true))
                        .foregroundColor(selected
                            ? Color.spIvory.opacity(0.55) : .spMuted)
                        .padding(.top, 4)
                    Text(opt.sub)
                        .font(.system(size: 12))
                        .foregroundColor(selected
                            ? Color.spIvory.opacity(0.8) : .spMuted)
                        .padding(.top, 10)
                }
                Spacer()
                checkDot(selected: selected)
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(selected ? Color.spPrimary : Color.spIvory)
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(selected ? Color.spPrimary : Color.spLine, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func checkDot(selected: Bool) -> some View {
        Circle()
            .fill(selected ? Color.spIvory : Color.clear)
            .overlay(
                Circle().stroke(
                    selected ? Color.spIvory : Color.spLine,
                    lineWidth: 1.5
                )
            )
            .frame(width: 24, height: 24)
            .overlay {
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.spPrimary)
                }
            }
    }
}

import SwiftUI

/// 05 · 自评水平 1–5 — 4 张卡片（ABCD）。
/// 后端 self_level 是 SMALLINT 1-5，这里把 D 高级映射为 4（留 5 给未来更细）。
struct OnbLevelView: View {

    @ObservedObject var vm: OnboardingViewModel

    private struct LevelOption {
        let letter: String
        let level: Int
        let band: String
        let title: String
        let desc: String
        let tags: [String]
    }

    private let options: [LevelOption] = [
        .init(letter: "A", level: 1, band: "4.0 – 5.0",
              title: "Beginner",
              desc: "能说简单句 · 想说但常卡壳",
              tags: ["语法错多", "词汇有限"]),
        .init(letter: "B", level: 2, band: "5.0 – 6.0",
              title: "Intermediate",
              desc: "基本能交流 · 但流畅度不足",
              tags: ["重复多", "发音不稳"]),
        .init(letter: "C", level: 3, band: "6.0 – 7.0",
              title: "Upper",
              desc: "能表达观点 · 复杂话题偶尔卡",
              tags: ["想更地道"]),
        .init(letter: "D", level: 4, band: "7.0+",
              title: "Advanced",
              desc: "流畅 · 想冲 7.5 或 8.0",
              tags: ["冲刺高分"]),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 16) {
                Text("SELF-ASSESSMENT · 04").font(.spEyebrow).foregroundColor(.spMuted)
                VStack(alignment: .leading, spacing: -4) {
                    Text("Where are you").font(.spSerif(34)).foregroundColor(.spPrimary)
                    Text("right now?")
                        .font(.spSerif(34, italic: true))
                        .foregroundColor(.spAccent)
                }
                Text("选一个最接近你当前水平的档位 —— 之后会用 30 秒基线测试校准。")
                    .font(.system(size: 13))
                    .foregroundColor(.spMuted)
            }
            .padding(.horizontal, 24)
            .padding(.top, 24)

            ScrollView {
                VStack(spacing: 10) {
                    ForEach(options, id: \.level) { opt in
                        card(opt)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 22)
            }

            OnbPrimaryButton(
                title: "继续 · 做基线测试",
                enabled: vm.selfLevel != nil
            ) {
                Task {
                    await vm.patchCurrent()
                    vm.next()
                }
            }
            .padding(24)
        }
    }

    private func card(_ opt: LevelOption) -> some View {
        let selected = vm.selfLevel == opt.level
        return Button {
            vm.selfLevel = opt.level
        } label: {
            HStack(alignment: .top, spacing: 14) {
                Text(opt.letter)
                    .font(.spSerif(28, italic: true))
                    .foregroundColor(selected ? .spAccent : .spMuted)
                    .frame(width: 30, alignment: .leading)
                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(opt.title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.spPrimary)
                        Spacer()
                        Text("Band \(opt.band)")
                            .font(.spSerif(11))
                            .foregroundColor(.spMuted)
                    }
                    Text(opt.desc)
                        .font(.system(size: 12))
                        .foregroundColor(.spMuted)
                    HStack(spacing: 6) {
                        ForEach(opt.tags, id: \.self) { tag in
                            Text(tag)
                                .font(.system(size: 10))
                                .foregroundColor(selected ? .spAccent : .spMuted)
                                .padding(.horizontal, 8).padding(.vertical, 2)
                                .background(selected ? Color.spAccentSoft : Color.spBgSoft)
                                .clipShape(Capsule())
                        }
                    }
                    .padding(.top, 4)
                }
                checkDot(selected: selected)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(selected ? Color.spIvory : Color.clear)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(selected ? Color.spPrimary : Color.spLine, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func checkDot(selected: Bool) -> some View {
        Circle()
            .fill(selected ? Color.spAccent : Color.clear)
            .overlay(
                Circle().stroke(selected ? Color.spAccent : Color.spLine, lineWidth: 1.5)
            )
            .frame(width: 22, height: 22)
            .overlay {
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.spIvory)
                }
            }
    }
}

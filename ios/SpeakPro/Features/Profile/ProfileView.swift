import SwiftUI

/// 个人中心 —— editorial 风格
///
/// 结构对照 Android `features/profile/ProfileScreen.kt`：
///   Masthead → Identity row → Goal block → Achievements grid →
///   Settings list → Logout (ghost) → Version footer
struct ProfileView: View {

    @StateObject private var viewModel = ProfileViewModel()
    @EnvironmentObject var coordinator: AppCoordinator

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Masthead()
                    IdentityRow(
                        name: viewModel.userName,
                        email: viewModel.userEmail
                    )
                    .padding(.top, 16)

                    GoalBlock()
                        .padding(.top, 28)

                    AchievementsGrid()
                        .padding(.top, 28)

                    SettingsList()
                        .padding(.top, 28)

                    LogoutGhost {
                        viewModel.logout()
                        coordinator.logout()
                    }
                    .padding(.top, 24)

                    VersionFooter()
                        .padding(.top, 28)
                        .padding(.bottom, 32)
                }
            }
            .background(Color.spBackground)
            .toolbar(.hidden, for: .navigationBar)
            .task { await viewModel.fetchProfile() }
        }
    }
}

// MARK: - Masthead

private struct Masthead: View {
    var body: some View {
        HStack {
            Eyebrow("PROFILE · 我的")
            Spacer()
            Text("EDITION № \(editionNumber())")
                .font(Font.custom("Inter", size: 11))
                .foregroundColor(.spMuted)
        }
        .padding(.horizontal, 24)
        .padding(.top, 14)
    }

    private func editionNumber() -> String {
        let day = Calendar.current.ordinality(of: .day, in: .year, for: Date()) ?? 1
        return String(format: "%03d", day)
    }
}

// MARK: - Identity

private struct IdentityRow: View {
    let name: String
    let email: String

    var body: some View {
        HStack(alignment: .center, spacing: 14) {
            // Avatar
            ZStack {
                Circle().fill(Color.spAccent.opacity(0.12))
                Text(String(name.prefix(1)).uppercased())
                    .font(Font.custom("Fraunces", size: 26))
                    .foregroundColor(.spAccent)
            }
            .frame(width: 60, height: 60)

            VStack(alignment: .leading, spacing: 4) {
                Text(name)
                    .font(Font.custom("Fraunces", size: 22))
                    .foregroundColor(.spPrimary)
                Text(email)
                    .font(Font.custom("Inter", size: 12))
                    .foregroundColor(.spMuted)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    Text("IELTS 学员")
                        .font(Font.custom("Inter", size: 10).weight(.semibold))
                        .tracking(1)
                        .foregroundColor(.spAccent)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 1)
                        .overlay(
                            RoundedRectangle(cornerRadius: 2)
                                .stroke(Color.spAccent, lineWidth: 1)
                        )
                    Text("LV 3")
                        .font(Font.custom("Inter", size: 10).weight(.semibold))
                        .tracking(1)
                        .foregroundColor(.spPrimary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 1)
                        .overlay(
                            RoundedRectangle(cornerRadius: 2)
                                .stroke(Color.spLine, lineWidth: 1)
                        )
                }
            }
            Spacer()
        }
        .padding(.horizontal, 24)
    }
}

// MARK: - Goal

private struct GoalBlock: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Eyebrow("MY GOAL · 学习目标")
                Spacer()
                Text("距考试 \(daysLeftToExam()) 天")
                    .font(Font.custom("Inter", size: 11))
                    .foregroundColor(.spMuted)
            }
            Spacer().frame(height: 10)
            (
                Text("目标 ")
                    .font(Font.custom("Fraunces", size: 28))
                    .foregroundColor(.spPrimary)
                + Text("IELTS 7.0")
                    .font(Font.custom("Fraunces-Italic", size: 28))
                    .foregroundColor(.spAccent)
            )
            Spacer().frame(height: 6)
            Text("坚持每天 15 分钟，有计划地推进 6 个维度。")
                .font(Font.custom("Inter", size: 12))
                .foregroundColor(.spMuted)
        }
        .padding(.horizontal, 24)
    }

    private func daysLeftToExam() -> Int {
        // 暂以"当前日期 + 1 个月"为占位考期
        let cal = Calendar.current
        let target = cal.date(byAdding: .month, value: 1, to: Date()) ?? Date()
        return max(cal.dateComponents([.day], from: Date(), to: target).day ?? 0, 0)
    }
}

// MARK: - Achievements grid (3×2)

private struct AchievementsGrid: View {
    private let items: [(icon: String, label: String)] = [
        ("🔥", "连胜 7 天"),
        ("◆", "模考 x 10"),
        ("❋", "发音 A+"),
        ("⬡", "语法满分"),
        ("✦", "Band 6.0"),
        ("⟡", "词汇王"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Eyebrow("ACHIEVEMENTS · 成就")
            Spacer().frame(height: 14)

            VStack(spacing: 8) {
                ForEach(0..<2, id: \.self) { row in
                    HStack(spacing: 8) {
                        ForEach(0..<3, id: \.self) { col in
                            let it = items[row * 3 + col]
                            VStack(spacing: 6) {
                                Text(it.icon)
                                    .font(Font.custom("Fraunces-Italic", size: 22))
                                    .foregroundColor(.spAccent)
                                Text(it.label)
                                    .font(Font.custom("Inter", size: 11))
                                    .foregroundColor(.spMuted)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.spIvory)
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(Color.spLine, lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 24)
    }
}

// MARK: - Settings

private struct SettingsList: View {
    private let rows: [(num: String, title: String, desc: String, badge: String?)] = [
        ("01", "学习目标", "考期 / 目标分 / 弱项偏好", nil),
        ("02", "每日提醒", "练习提醒时间 / 免打扰", nil),
        ("03", "考官口音", "英式 / 美式 / 澳洲", nil),
        ("04", "订阅计划", "解锁更多场景与报告", "PRO"),
        ("05", "数据与隐私", "录音存储 / 账号安全", nil),
        ("06", "帮助与反馈", "联系我们 / 常见问题", nil),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Eyebrow("SETTINGS · 设置")
            Spacer().frame(height: 14)
            Rectangle().fill(Color.spLine).frame(height: 1)

            ForEach(rows, id: \.num) { r in
                Row(num: r.num, title: r.title, desc: r.desc, badge: r.badge)
            }
        }
        .padding(.horizontal, 24)
    }

    private struct Row: View {
        let num: String
        let title: String
        let desc: String
        let badge: String?

        var body: some View {
            VStack(spacing: 0) {
                HStack(spacing: 14) {
                    Text(num)
                        .font(Font.custom("Fraunces-Italic", size: 20))
                        .foregroundColor(.spMuted)
                        .frame(width: 28, alignment: .leading)
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 8) {
                            Text(title)
                                .font(Font.custom("Inter", size: 15).weight(.semibold))
                                .foregroundColor(.spPrimary)
                            if let b = badge {
                                Text(b)
                                    .font(Font.custom("Inter", size: 9).weight(.semibold))
                                    .tracking(1)
                                    .foregroundColor(.spIvory)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 1)
                                    .background(Color.spAccent)
                                    .clipShape(RoundedRectangle(cornerRadius: 2))
                            }
                        }
                        Text(desc)
                            .font(Font.custom("Inter", size: 12))
                            .foregroundColor(.spMuted)
                    }
                    Spacer()
                    Image(systemName: "arrow.right")
                        .font(.system(size: 12))
                        .foregroundColor(.spMuted)
                }
                .padding(.vertical, 14)
                Rectangle().fill(Color.spLine).frame(height: 1)
            }
        }
    }
}

// MARK: - Logout ghost

private struct LogoutGhost: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text("退出登录")
                .font(Font.custom("Inter", size: 14).weight(.semibold))
                .foregroundColor(.spAccent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color.spAccent.opacity(0.4), lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 24)
    }
}

// MARK: - Version footer

private struct VersionFooter: View {
    var body: some View {
        Text("SPEAKPRO · V 2.4.1 · 2026")
            .font(Font.custom("Inter", size: 10))
            .tracking(2)
            .foregroundColor(.spMuted)
            .frame(maxWidth: .infinity)
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
    ProfileView()
        .environmentObject(AppCoordinator())
}

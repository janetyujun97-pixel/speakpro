import SwiftUI

/// 首页视图
struct HomeView: View {

    @EnvironmentObject private var coordinator: AppCoordinator
    @StateObject private var viewModel = HomeViewModel()
    @State private var navigationPath = NavigationPath()

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {

                    // MARK: - 问候 + 连续打卡
                    greetingSection

                    // MARK: - 今日进度条
                    todayProgressSection

                    // MARK: - 快速入口按钮
                    quickEntrySection

                    // MARK: - 推荐练习 (横向滚动)
                    recommendedSection

                    // MARK: - 作业提醒
                    homeworkReminderSection
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
            .background(Color.spBackground)
            .navigationTitle("SpeakPro")
            .navigationDestination(for: PracticeMode.self) { mode in
                switch mode {
                case .conversation:
                    ConversationView()
                case .readAloud:
                    ReadAloudView()
                case .followRead:
                    FollowReadView()
                case .mockExam:
                    MockExamView()
                }
            }
            .task {
                await viewModel.fetchHomeData()
            }
        }
    }

    // MARK: - Greeting

    private var greetingSection: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("你好，同学！")
                    .font(.spTitleLarge)
                    .foregroundColor(.spTextPrimary)

                Text("已连续练习 \(viewModel.streakDays) 天")
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextSecondary)
            }

            Spacer()

            // 打卡火焰图标
            ZStack {
                Circle()
                    .fill(Color.spAccent.opacity(0.1))
                    .frame(width: 48, height: 48)

                Image(systemName: "flame.fill")
                    .font(.system(size: 24))
                    .foregroundColor(.spAccent)
            }
        }
        .padding(.top, 8)
    }

    // MARK: - Today Progress

    private var todayProgressSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("今日进度")
                    .font(.spTitleSmall)
                    .foregroundColor(.spTextPrimary)
                Spacer()
                Text("\(Int(viewModel.todayProgress * 100))%")
                    .font(.spBodyMedium)
                    .foregroundColor(.spAccent)
            }

            SwiftUI.ProgressView(value: viewModel.todayProgress)
                .tint(.spAccent)
                .scaleEffect(y: 2)
                .clipShape(Capsule())
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(16)
        .shadow(color: .black.opacity(0.05), radius: 8, y: 2)
    }

    // MARK: - Quick Entry

    private var quickEntrySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("快速开始")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 12) {
                quickEntryButton(title: "AI 对话", icon: "bubble.left.and.bubble.right.fill", color: .spAccent, mode: .conversation)
                quickEntryButton(title: "跟读", icon: "waveform.and.mic", color: .spSuccess, mode: .followRead)
                quickEntryButton(title: "朗读", icon: "text.bubble.fill", color: .spPrimary, mode: .readAloud)
                quickEntryButton(title: "模考", icon: "clock.badge.checkmark.fill", color: .spWarning, mode: .mockExam)
            }
        }
    }

    private func quickEntryButton(title: String, icon: String, color: Color, mode: PracticeMode) -> some View {
        Button {
            navigationPath.append(mode)
        } label: {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 28))
                    .foregroundColor(color)
                Text(title)
                    .font(.spBodySmall)
                    .foregroundColor(.spTextPrimary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(Color.white)
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.04), radius: 4, y: 1)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Recommended Practice

    private var recommendedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("推荐练习")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(viewModel.recommendedItems) { item in
                        Button {
                            navigationPath.append(item.mode)
                        } label: {
                            recommendedCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func recommendedCard(item: RecommendedItem) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: item.mode.iconName)
                .font(.system(size: 20))
                .foregroundColor(.spAccent)

            Text(item.title)
                .font(.spBodyMedium)
                .foregroundColor(.spTextPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            Text("点击开始练习")
                .font(.spCaption)
                .foregroundColor(.spTextSecondary)
        }
        .padding(16)
        .frame(width: 160, alignment: .leading)
        .background(Color.white)
        .cornerRadius(12)
        .shadow(color: .black.opacity(0.05), radius: 4, y: 1)
    }

    // MARK: - Homework Reminders

    private var homeworkReminderSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("待完成作业")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            if viewModel.pendingHomework.isEmpty {
                Text("暂无待完成作业")
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextSecondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
            } else {
                ForEach(viewModel.pendingHomework, id: \.self) { homework in
                    Button {
                        // 切换到作业 Tab 并确保显示"待完成"列表
                        NotificationCenter.default.post(name: .switchToPendingHomework, object: nil)
                        coordinator.selectedTab = .homework
                    } label: {
                        HStack {
                            Image(systemName: "doc.text.fill")
                                .foregroundColor(.spWarning)
                            Text(homework)
                                .font(.spBodyMedium)
                                .foregroundColor(.spTextPrimary)
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.spCaption)
                                .foregroundColor(.spTextSecondary)
                        }
                        .padding(12)
                        .background(Color.white)
                        .cornerRadius(10)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

#Preview {
    HomeView()
        .environmentObject(AppCoordinator())
}

import SwiftUI

/// 应用全局导航与路由协调器
///
/// 路由状态：
///   .splash       —— App 启动时短暂展示 Splash
///   .auth         —— 未登录
///   .onboarding   —— 已登录但 onboarding 未完成
///   .main         —— 已登录且 onboarding 完成
final class AppCoordinator: ObservableObject {

    enum Route: Equatable {
        case splash
        case auth
        case onboarding
        case main
    }

    // MARK: - Tab 定义

    enum Tab: String, CaseIterable, Identifiable {
        case home, practice, homework, progress, profile

        var id: String { rawValue }

        var title: String {
            switch self {
            case .home:     return "首页"
            case .practice: return "练习"
            case .homework: return "作业"
            case .progress: return "进度"
            case .profile:  return "我的"
            }
        }

        var icon: String {
            switch self {
            case .home:     return "house.fill"
            case .practice: return "mic.fill"
            case .homework: return "book.fill"
            case .progress: return "chart.bar.fill"
            case .profile:  return "person.fill"
            }
        }
    }

    // MARK: - Published

    @Published var route: Route = .splash
    @Published var selectedTab: Tab = .home
    @Published var currentUser: UserInfo? = nil

    // 兼容旧代码：isAuthenticated
    var isAuthenticated: Bool {
        get { route == .main || route == .onboarding }
        set { /* 历史兼容：setter 只在登录成功时触发；实际切换用 route */
            if newValue == false { logout() }
        }
    }

    // MARK: - Init

    init() {
        // 从 Keychain 恢复登录态
        if let token = APIClient.shared.accessToken, !token.isEmpty {
            if let data = UserDefaults.standard.data(forKey: "speakpro_user"),
               let user = try? JSONDecoder().decode(UserInfo.self, from: data) {
                currentUser = user
            }
            // 已有 token：默认进 main，异步查 onboarding_status 决定是否降级到 onboarding
            route = .main
            Task { await refreshOnboardingStatusAfterLogin() }
        }
    }

    // MARK: - Actions

    /// Splash 动画完成后调用
    func splashFinished() {
        if route == .splash {
            if APIClient.shared.accessToken?.isEmpty == false {
                // 已登录：默认 main，异步校准 onboarding 状态
                route = .main
                selectedTab = .home
                Task { await refreshOnboardingStatusAfterLogin() }
            } else {
                route = .auth
            }
        }
    }

    /// 登录成功（邮箱 / 手机 OTP / Apple）后调用
    ///
    /// 分流策略（与 Android OnboardingCheck 对齐）：
    ///   - profile 不存在（老用户 / 没走过 onboarding）→ 直接进 main
    ///   - profile 存在但 completed=false（中途退出）→ onboarding 继续
    ///   - profile.completed=true → main
    ///   - 网络失败 → main（避免老用户被卡在 onboarding）
    ///
    /// 默认先进 main，随后异步校准：仅在"有 profile 但未完成"时降级到 onboarding。
    func completeLogin(user: UserInfo) {
        currentUser = user
        persistUser(user)
        route = .main
        selectedTab = .home
        Task { await refreshOnboardingStatusAfterLogin() }
    }

    /// onboarding 完成后切到主界面
    func markOnboardingCompleted() {
        withAnimation(.easeInOut(duration: 0.3)) {
            route = .main
            selectedTab = .home
        }
    }

    /// 退出登录
    func logout() {
        APIClient.shared.accessToken  = nil
        APIClient.shared.refreshToken = nil
        UserDefaults.standard.removeObject(forKey: "speakpro_user")
        withAnimation(.easeInOut(duration: 0.3)) {
            currentUser = nil
            selectedTab = .home
            route = .auth
        }
    }

    // MARK: - Internal

    private func persistUser(_ user: UserInfo) {
        if let data = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(data, forKey: "speakpro_user")
        }
    }

    /// 登录后异步查 onboarding 状态；只有"有 profile 但未完成"时才降级到 onboarding
    private func refreshOnboardingStatusAfterLogin() async {
        do {
            let status = try await APIClient.shared.getOnboardingStatus()
            await MainActor.run {
                if status.profile != nil && !status.completed {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        self.route = .onboarding
                    }
                }
                // 其他情况（profile=nil 或 completed=true）保持默认 main
            }
        } catch {
            // 网络失败不改路由 —— 留在 main（已是默认）
        }
    }
}

// MARK: - ContentView (Tab Bar 主界面)

struct ContentView: View {
    @EnvironmentObject var coordinator: AppCoordinator

    var body: some View {
        TabView(selection: $coordinator.selectedTab) {
            HomeView()
                .tabItem {
                    Label(AppCoordinator.Tab.home.title,
                          systemImage: AppCoordinator.Tab.home.icon)
                }
                .tag(AppCoordinator.Tab.home)

            PracticeListView()
                .tabItem {
                    Label(AppCoordinator.Tab.practice.title,
                          systemImage: AppCoordinator.Tab.practice.icon)
                }
                .tag(AppCoordinator.Tab.practice)

            HomeworkListView()
                .tabItem {
                    Label(AppCoordinator.Tab.homework.title,
                          systemImage: AppCoordinator.Tab.homework.icon)
                }
                .tag(AppCoordinator.Tab.homework)

            ProgressView()
                .tabItem {
                    Label(AppCoordinator.Tab.progress.title,
                          systemImage: AppCoordinator.Tab.progress.icon)
                }
                .tag(AppCoordinator.Tab.progress)

            ProfileView()
                .tabItem {
                    Label(AppCoordinator.Tab.profile.title,
                          systemImage: AppCoordinator.Tab.profile.icon)
                }
                .tag(AppCoordinator.Tab.profile)
        }
        .tint(Color.spAccent)
    }
}

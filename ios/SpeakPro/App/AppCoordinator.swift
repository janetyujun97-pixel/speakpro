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
            // 登录态存在时，先进 onboarding 查询；hydrate 后决定去哪
            route = .onboarding
        }
    }

    // MARK: - Actions

    /// Splash 动画完成后调用
    func splashFinished() {
        if route == .splash {
            route = (APIClient.shared.accessToken?.isEmpty == false) ? .onboarding : .auth
        }
    }

    /// 登录成功（邮箱 / 手机 OTP / Apple）后调用
    func completeLogin(user: UserInfo) {
        currentUser = user
        persistUser(user)
        route = .onboarding
        // 新用户进入 .onboarding 后，OnboardingCoordinator 里 hydrate 会拉 /onboarding/status，
        // 若 completed_at != nil，会调 markOnboardingCompleted 切到主界面。
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

    /// 登录后立刻问一下 onboarding 状态；若已完成则直接进主界面
    private func refreshOnboardingStatusAfterLogin() async {
        do {
            let status = try await APIClient.shared.getOnboardingStatus()
            await MainActor.run {
                if status.completed {
                    self.markOnboardingCompleted()
                }
            }
        } catch {
            // 网络失败不阻塞 —— 留在 .onboarding，VM 内部会重新拉
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

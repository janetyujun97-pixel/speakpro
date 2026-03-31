import SwiftUI

/// 应用全局导航与路由协调器
final class AppCoordinator: ObservableObject {

    // MARK: - Tab 定义

    enum Tab: String, CaseIterable, Identifiable {
        case home
        case practice
        case homework
        case progress
        case profile

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

    // MARK: - Published Properties

    @Published var isAuthenticated: Bool = false
    @Published var selectedTab: Tab = .home
    @Published var currentUser: UserInfo? = nil

    // MARK: - Init — 从 Keychain 恢复登录态

    init() {
        // 如果 Keychain 中存在 access token，则认为已登录
        if let token = APIClient.shared.accessToken, !token.isEmpty {
            isAuthenticated = true
            // 尝试从 UserDefaults 恢复用户信息
            if let data = UserDefaults.standard.data(forKey: "speakpro_user"),
               let user = try? JSONDecoder().decode(UserInfo.self, from: data) {
                currentUser = user
            }
        }
    }

    // MARK: - Auth Methods

    /// 登录成功后调用，保存用户信息并切换至主界面
    func completeLogin(user: UserInfo) {
        currentUser = user
        isAuthenticated = true

        // 持久化用户信息（非敏感数据）
        if let data = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(data, forKey: "speakpro_user")
        }
    }

    /// 登出：清除 Token、用户信息，返回登录页
    func logout() {
        APIClient.shared.accessToken  = nil
        APIClient.shared.refreshToken = nil
        UserDefaults.standard.removeObject(forKey: "speakpro_user")

        withAnimation(.easeInOut(duration: 0.3)) {
            currentUser = nil
            isAuthenticated = false
            selectedTab = .home
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

            // 练习 Tab — 暂用占位视图，Phase 2 实现
            NavigationStack {
                VStack(spacing: 16) {
                    Image(systemName: "mic.circle.fill")
                        .font(.system(size: 60))
                        .foregroundStyle(Color.spAccent)
                    Text("练习模式")
                        .font(.title2.bold())
                    Text("Phase 2 开发中...")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .navigationTitle("练习")
            }
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

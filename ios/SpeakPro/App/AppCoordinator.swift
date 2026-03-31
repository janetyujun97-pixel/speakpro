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

    // MARK: - Methods

    func logout() {
        // TODO: 清除 token、重置状态
        isAuthenticated = false
    }
}

// MARK: - ContentView (Tab Bar)

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

            // 练习 Tab — 暂用占位视图
            Text("练习入口")
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

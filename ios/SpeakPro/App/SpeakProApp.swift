import SwiftUI

@main
struct SpeakProApp: App {
    @StateObject private var coordinator = AppCoordinator()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(coordinator)
        }
    }
}

// MARK: - RootView — 根据认证状态路由到登录页或主界面

struct RootView: View {
    @EnvironmentObject private var coordinator: AppCoordinator

    var body: some View {
        Group {
            if coordinator.isAuthenticated {
                ContentView()
                    .transition(.asymmetric(
                        insertion: .move(edge: .trailing),
                        removal: .move(edge: .leading)
                    ))
            } else {
                LoginView(coordinator: coordinator)
                    .transition(.asymmetric(
                        insertion: .move(edge: .leading),
                        removal: .move(edge: .trailing)
                    ))
            }
        }
        .animation(.easeInOut(duration: 0.3), value: coordinator.isAuthenticated)
    }
}

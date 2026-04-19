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

// MARK: - RootView — 按 AppCoordinator.route 路由 Splash / Auth / Onboarding / Main

struct RootView: View {
    @EnvironmentObject private var coordinator: AppCoordinator
    @StateObject private var networkMonitor = NetworkMonitor.shared

    var body: some View {
        VStack(spacing: 0) {
            OfflineBanner(monitor: networkMonitor)
                .animation(.easeInOut(duration: 0.25), value: networkMonitor.isConnected)

            Group {
                switch coordinator.route {
                case .splash:
                    SplashView(onFinished: coordinator.splashFinished)
                        .transition(.opacity)
                case .auth:
                    LoginView(coordinator: coordinator)
                        .transition(.asymmetric(
                            insertion: .move(edge: .leading),
                            removal: .move(edge: .trailing)
                        ))
                case .onboarding:
                    OnboardingCoordinator()
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing),
                            removal: .move(edge: .leading)
                        ))
                case .main:
                    ContentView()
                        .transition(.asymmetric(
                            insertion: .move(edge: .trailing),
                            removal: .move(edge: .leading)
                        ))
                }
            }
            .animation(.easeInOut(duration: 0.3), value: coordinator.route)
        }
    }
}

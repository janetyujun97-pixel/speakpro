import SwiftUI

@main
struct SpeakProApp: App {
    @StateObject private var coordinator = AppCoordinator()

    init() {
        // PR5 follow-up —— 注入离线上传队列的默认 uploader。
        // 当前实现只处理 baseline 录音（/onboarding/baseline）；后续可扩展为 switch on 任务类型。
        Task { @MainActor in
            OfflineUploadQueue.shared.uploader = { task in
                guard !task.audioFilename.isEmpty else { return false }
                do {
                    _ = try await APIClient.shared.postBaseline(.init(
                        sessionId: task.sessionId,
                        audioUrl: task.audioFilename,   // 占位：OSS 就位后改签名 URL
                        transcript: nil,
                    ))
                    return true
                } catch {
                    return false
                }
            }
        }
    }

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

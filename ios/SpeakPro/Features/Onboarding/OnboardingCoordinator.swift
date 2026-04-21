import SwiftUI

/// 8 步 onboarding 的容器 —— 根据 VM.step 渲染对应 view，并提供统一头部（进度 + 跳过）。
struct OnboardingCoordinator: View {

    @EnvironmentObject private var appCoordinator: AppCoordinator
    @StateObject private var vm = OnboardingViewModel()

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()
            VStack(spacing: 0) {
                if vm.step != .welcome {
                    StepBar(step: vm.step, onBack: { vm.back() })
                        .padding(.horizontal, 24)
                        .padding(.top, 12)
                }

                currentView
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task { await vm.hydrate() }
        .onChange(of: vm.completed) { _, done in
            if done {
                // 完成后把控制权交回 AppCoordinator，走主界面
                appCoordinator.markOnboardingCompleted()
            }
        }
    }

    @ViewBuilder
    private var currentView: some View {
        switch vm.step {
        case .welcome:           OnbWelcomeView(vm: vm)
        case .examType:          OnbExamTypeView(vm: vm)
        case .target:            OnbTargetView(vm: vm)
        case .date:              OnbDateView(vm: vm)
        case .level:             OnbLevelView(vm: vm)
        case .baselineIntro:     OnbBaselineIntroView(vm: vm)
        case .baselineRecording: OnbBaselineRecordingView(vm: vm)
        case .plan:              OnbPlanView(vm: vm)
        }
    }
}

// MARK: - Step Bar（进度条 + STEP X / Y + 跳过）

struct StepBar: View {
    let step: OnboardingViewModel.Step
    var onBack: () -> Void

    var body: some View {
        VStack(spacing: 10) {
            HStack(spacing: 3) {
                ForEach(0..<OnboardingViewModel.Step.total, id: \.self) { i in
                    let state = stepState(i)
                    Rectangle()
                        .fill(
                            state == .done ? Color.spAccent :
                            state == .current ? Color.spPrimary : Color.spLine
                        )
                        .frame(height: 2)
                        .clipShape(Capsule())
                }
            }
            HStack {
                Button(action: onBack) {
                    HStack(spacing: 4) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 11, weight: .medium))
                        Text("返回")
                            .font(.system(size: 11))
                    }
                    .foregroundColor(.spMuted)
                }
                Spacer()
                Text("STEP \(pad(step.number)) · OF \(pad(OnboardingViewModel.Step.total))")
                    .font(.spEyebrow)
                    .foregroundColor(.spMuted)
            }
        }
    }

    private enum StepState { case done, current, pending }
    private func stepState(_ i: Int) -> StepState {
        if i < step.rawValue { return .done }
        if i == step.rawValue { return .current }
        return .pending
    }
    private func pad(_ n: Int) -> String { String(format: "%02d", n) }
}

// MARK: - 通用"继续"按钮

struct OnbPrimaryButton: View {
    let title: String
    var enabled: Bool = true
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Text(title).font(.system(size: 14, weight: .semibold))
                Image(systemName: "arrow.right")
            }
            .foregroundColor(.spIvory)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(enabled ? Color.spPrimary : Color.spPrimary.opacity(0.4))
            .clipShape(Capsule())
        }
        .disabled(!enabled)
    }
}

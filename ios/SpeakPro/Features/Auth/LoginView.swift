import SwiftUI

// MARK: - LoginView

struct LoginView: View {

    @EnvironmentObject private var coordinator: AppCoordinator
    @StateObject private var viewModel: LoginViewModel

    init(coordinator: AppCoordinator) {
        _viewModel = StateObject(wrappedValue: LoginViewModel(coordinator: coordinator))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    // 顶部 Logo 区域
                    logoSection
                        .padding(.top, 60)
                        .padding(.bottom, 40)

                    // 登录表单
                    formSection
                        .padding(.horizontal, 24)

                    // 底部提示
                    footerSection
                        .padding(.top, 32)
                }
            }
            .background(Color(.systemGroupedBackground))
            .navigationBarHidden(true)
        }
    }

    // MARK: - Logo 区域

    private var logoSection: some View {
        VStack(spacing: 12) {
            // App 图标
            ZStack {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [Color.spAccent, Color.spAccent.opacity(0.7)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 80, height: 80)

                Image(systemName: "waveform.circle.fill")
                    .font(.system(size: 40))
                    .foregroundStyle(.white)
            }

            Text("SpeakPro")
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundStyle(Color.spTextPrimary)

            Text("AI 驱动的托福/雅思口语练习")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - 表单区域

    private var formSection: some View {
        VStack(spacing: 16) {
            // 卡片容器
            VStack(spacing: 0) {
                // 邮箱输入
                HStack(spacing: 12) {
                    Image(systemName: "envelope")
                        .foregroundStyle(.secondary)
                        .frame(width: 20)

                    TextField("邮箱", text: $viewModel.email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                        .autocorrectionDisabled()
                        .textContentType(.emailAddress)
                }
                .padding(16)

                Divider()
                    .padding(.leading, 48)

                // 密码输入
                HStack(spacing: 12) {
                    Image(systemName: "lock")
                        .foregroundStyle(.secondary)
                        .frame(width: 20)

                    SecureField("密码", text: $viewModel.password)
                        .textContentType(.password)
                }
                .padding(16)
            }
            .background(Color(.secondarySystemGroupedBackground))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            // 错误提示
            if let error = viewModel.errorMessage {
                HStack(spacing: 8) {
                    Image(systemName: "exclamationmark.circle.fill")
                        .foregroundStyle(.red)
                    Text(error)
                        .font(.footnote)
                        .foregroundStyle(.red)
                    Spacer()
                }
                .padding(.horizontal, 4)
                .transition(.move(edge: .top).combined(with: .opacity))
            }

            // 登录按钮
            Button {
                Task { await viewModel.login() }
            } label: {
                Group {
                    if viewModel.isLoading {
                        // 使用 SwiftUI 模块限定，避免与自定义 ProgressView 冲突
                        SwiftUI.ProgressView()
                            .progressViewStyle(.circular)
                            .tint(.white)
                    } else {
                        Text("登录")
                            .font(.system(size: 16, weight: .semibold))
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(
                    viewModel.isFormValid
                        ? Color.spAccent
                        : Color.spAccent.opacity(0.4)
                )
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .disabled(!viewModel.isFormValid || viewModel.isLoading)
            .animation(.easeInOut(duration: 0.2), value: viewModel.isFormValid)
        }
    }

    // MARK: - 底部区域

    private var footerSection: some View {
        VStack(spacing: 8) {
            Text("测试账号：teacher@speakpro.com")
                .font(.caption)
                .foregroundStyle(.tertiary)
            Text("密码：teacher123")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
    }
}

// MARK: - Preview

#Preview {
    LoginView(coordinator: AppCoordinator())
        .environmentObject(AppCoordinator())
}

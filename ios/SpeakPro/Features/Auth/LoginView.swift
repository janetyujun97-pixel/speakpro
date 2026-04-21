import AuthenticationServices
import SwiftUI

// MARK: - LoginView — 手机 OTP 优先，OR 分割线下三社交入口（Apple / WeChat / 邮箱密码）

struct LoginView: View {

    @EnvironmentObject private var coordinator: AppCoordinator
    @StateObject private var phoneVM: PhoneAuthViewModel
    @StateObject private var loginVM: LoginViewModel

    @State private var appleCoordinator = AppleSignInCoordinator()
    @State private var showingEmailLogin = false
    @State private var navigateToOtp = false
    @State private var navigateToRegister = false
    @State private var navigateToForgot = false
    @State private var inlineError: String? = nil

    init(coordinator: AppCoordinator) {
        _phoneVM = StateObject(wrappedValue: PhoneAuthViewModel())
        _loginVM = StateObject(wrappedValue: LoginViewModel(coordinator: coordinator))
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.spBackground.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 0) {
                        masthead
                            .padding(.horizontal, 28)
                            .padding(.top, 20)

                        welcomeHeadline
                            .padding(.horizontal, 28)
                            .padding(.top, 32)

                        phoneField
                            .padding(.horizontal, 28)
                            .padding(.top, 36)

                        primaryButton
                            .padding(.horizontal, 28)
                            .padding(.top, 24)

                        if let err = inlineError ?? phoneVM.errorMessage {
                            errorBanner(err)
                                .padding(.horizontal, 28)
                                .padding(.top, 12)
                        }

                        divider
                            .padding(.horizontal, 28)
                            .padding(.top, 28)

                        socialGrid
                            .padding(.horizontal, 28)
                            .padding(.top, 18)

                        footerLinks
                            .padding(.top, 24)
                            .padding(.bottom, 28)

                        legalFooter
                            .padding(.horizontal, 28)
                            .padding(.bottom, 30)
                    }
                }
            }
            .navigationBarHidden(true)
            .navigationDestination(isPresented: $navigateToOtp) {
                OTPView(phoneVM: phoneVM, flow: .login)
                    .environmentObject(coordinator)
            }
            .navigationDestination(isPresented: $navigateToRegister) {
                RegisterView(prefilled: phoneVM)
                    .environmentObject(coordinator)
            }
            .navigationDestination(isPresented: $navigateToForgot) {
                ForgotView()
                    .environmentObject(coordinator)
            }
            .sheet(isPresented: $showingEmailLogin) {
                EmailLoginSheet(viewModel: loginVM)
            }
        }
    }

    // MARK: Subviews

    private var masthead: some View {
        HStack {
            Text("SPEAKPRO · LOG IN").font(.spEyebrow).foregroundColor(.spMuted)
            Spacer()
            Button("EN") { }
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(.spAccent)
        }
        .padding(.bottom, 14)
        .overlay(alignment: .bottom) {
            Rectangle().fill(Color.spLine).frame(height: 1)
        }
    }

    private var welcomeHeadline: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Welcome")
                .font(.spSerif(36))
                .foregroundColor(.spPrimary)
            Text("back.")
                .font(.spSerif(36, italic: true))
                .foregroundColor(.spAccent)
                .padding(.top, -6)
            Text("Sign in to pick up your rehearsal\nwhere you left off.")
                .font(.spSerif(14, italic: true))
                .foregroundColor(.spMuted)
                .lineSpacing(3)
                .padding(.top, 12)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var phoneField: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("MOBILE · 手机号").font(.spEyebrow).foregroundColor(.spMuted)
            HStack(spacing: 10) {
                Text("+86")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.spPrimary)
                Rectangle().fill(Color.spLine).frame(width: 1, height: 14)
                TextField("", text: $phoneVM.phone, prompt: Text("138 0013 8000")
                    .foregroundColor(.spMuted.opacity(0.6))
                )
                    .keyboardType(.numberPad)
                    .textContentType(.telephoneNumber)
                    .font(.spSerif(16, italic: true))
                    .foregroundColor(.spPrimary)
            }
            .padding(.bottom, 10)
            .overlay(alignment: .bottom) {
                Rectangle().fill(Color.spPrimary).frame(height: 1)
            }
        }
    }

    private var primaryButton: some View {
        Button {
            Task { await requestOtp() }
        } label: {
            HStack(spacing: 8) {
                if phoneVM.isSending {
                    SwiftUI.ProgressView().tint(.spIvory)
                } else {
                    Text("获取验证码").font(.system(size: 14, weight: .semibold))
                    Image(systemName: "arrow.right")
                        .font(.system(size: 13, weight: .medium))
                }
            }
            .foregroundColor(.spIvory)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(
                phoneVM.isPhoneValid ? Color.spPrimary : Color.spPrimary.opacity(0.3)
            )
            .clipShape(Capsule())
        }
        .disabled(!phoneVM.isPhoneValid || phoneVM.isSending)
        .animation(.easeInOut(duration: 0.2), value: phoneVM.isPhoneValid)
    }

    private func errorBanner(_ text: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "exclamationmark.circle.fill")
                .foregroundColor(.spError)
            Text(text).font(.footnote).foregroundColor(.spError)
            Spacer()
        }
    }

    private var divider: some View {
        HStack(spacing: 10) {
            Rectangle().fill(Color.spLine).frame(height: 1)
            Text("OR").font(.spEyebrow).foregroundColor(.spMuted)
            Rectangle().fill(Color.spLine).frame(height: 1)
        }
    }

    private var socialGrid: some View {
        HStack(spacing: 10) {
            socialButton(icon: Image(systemName: "apple.logo"), label: "Apple") {
                Task { await signInWithApple() }
            }
            socialButton(icon: Image(systemName: "message.fill"), label: "WeChat") {
                inlineError = "微信登录敬请期待"
            }
            socialButton(icon: Image(systemName: "lock"), label: "Password") {
                showingEmailLogin = true
            }
        }
    }

    private func socialButton(icon: Image, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                icon.font(.system(size: 18))
                    .foregroundColor(.spPrimary)
                Text(label)
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(.spMuted)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color.spIvory)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Color.spLine, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    private var footerLinks: some View {
        HStack(spacing: 16) {
            Button("注册账号") { navigateToRegister = true }
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.spPrimary)
            Text("·").foregroundColor(.spMuted)
            Button("忘记密码") { navigateToForgot = true }
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.spPrimary)
        }
    }

    private var legalFooter: some View {
        VStack(spacing: 4) {
            Text("首次登录即注册 · 代表同意")
                .font(.system(size: 11))
                .foregroundColor(.spMuted)
            HStack(spacing: 4) {
                Text("用户协议").underline()
                Text("·")
                Text("隐私政策").underline()
            }
            .font(.system(size: 11))
            .foregroundColor(.spPrimary)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Actions

    private func requestOtp() async {
        inlineError = nil
        let ok = await phoneVM.sendOtp()
        if ok { navigateToOtp = true }
    }

    private func signInWithApple() async {
        inlineError = nil
        await withCheckedContinuation { (cont: CheckedContinuation<Void, Never>) in
            appleCoordinator.start { result in
                Task { @MainActor in
                    switch result {
                    case .success(let resp):
                        APIClient.shared.accessToken  = resp.accessToken
                        APIClient.shared.refreshToken = resp.refreshToken
                        coordinator.completeLogin(user: resp.user)
                    case .failure(let err):
                        if let e = err as? AppleSignInCoordinator.AppleSignInError,
                           e == .cancelled {
                            // 用户取消 —— 无声
                        } else if case APIError.serverError(503, _) = err {
                            inlineError = "Apple 登录尚未开通"
                        } else {
                            inlineError = err.localizedDescription
                        }
                    }
                    cont.resume()
                }
            }
        }
    }
}

// MARK: - 邮箱密码 Sheet（备用入口）

private struct EmailLoginSheet: View {
    @ObservedObject var viewModel: LoginViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("邮箱登录")) {
                    TextField("邮箱", text: $viewModel.email)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                    SecureField("密码", text: $viewModel.password)
                }
                if let err = viewModel.errorMessage {
                    Text(err).foregroundColor(.spError)
                }
                Section {
                    Button {
                        Task {
                            await viewModel.login()
                            if viewModel.errorMessage == nil { dismiss() }
                        }
                    } label: {
                        HStack {
                            Spacer()
                            if viewModel.isLoading {
                                SwiftUI.ProgressView()
                            } else {
                                Text("登录").fontWeight(.semibold)
                            }
                            Spacer()
                        }
                    }
                    .disabled(!viewModel.isFormValid || viewModel.isLoading)
                }
            }
            .navigationTitle("密码登录")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }
}

#Preview {
    LoginView(coordinator: AppCoordinator())
        .environmentObject(AppCoordinator())
}

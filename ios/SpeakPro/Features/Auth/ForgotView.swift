import SwiftUI

/// 忘记密码入口 —— 先填手机号并下发 OTP，然后跳 OTPView(.reset)。
struct ForgotView: View {

    @Environment(\.dismiss) private var dismiss
    @StateObject private var phoneVM = PhoneAuthViewModel()
    @State private var navigateToOtp = false

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                header

                VStack(alignment: .leading, spacing: 14) {
                    Text("RECOVER · 找回密码")
                        .font(.spEyebrow).foregroundColor(.spMuted)
                    VStack(alignment: .leading, spacing: -2) {
                        Text("Reset your").font(.spSerif(32)).foregroundColor(.spPrimary)
                        Text("password.")
                            .font(.spSerif(32, italic: true))
                            .foregroundColor(.spAccent)
                    }
                    Text("输入注册手机号，我们会发送验证码给你。")
                        .font(.system(size: 13))
                        .foregroundColor(.spMuted)
                        .padding(.top, 6)
                }
                .padding(.horizontal, 28)
                .padding(.top, 14)

                phoneField
                    .padding(.horizontal, 28)
                    .padding(.top, 32)

                if let err = phoneVM.errorMessage {
                    Text(err)
                        .font(.footnote)
                        .foregroundColor(.spError)
                        .padding(.horizontal, 28)
                        .padding(.top, 12)
                }

                Spacer()

                Button {
                    Task { await request() }
                } label: {
                    HStack(spacing: 8) {
                        if phoneVM.isSending {
                            SwiftUI.ProgressView().tint(.spIvory)
                        } else {
                            Text("获取验证码").font(.system(size: 14, weight: .semibold))
                            Image(systemName: "arrow.right")
                        }
                    }
                    .foregroundColor(.spIvory)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(phoneVM.isPhoneValid ? Color.spPrimary : Color.spPrimary.opacity(0.4))
                    .clipShape(Capsule())
                }
                .disabled(!phoneVM.isPhoneValid || phoneVM.isSending)
                .padding(.horizontal, 28)
                .padding(.bottom, 24)
            }
            .navigationDestination(isPresented: $navigateToOtp) {
                OTPView(phoneVM: phoneVM, flow: .reset)
            }
        }
        .navigationBarHidden(true)
    }

    private var header: some View {
        HStack {
            Button { dismiss() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(.spPrimary)
            }
            Spacer()
        }
        .padding(.horizontal, 28)
        .padding(.top, 8)
        .padding(.bottom, 16)
    }

    private var phoneField: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("MOBILE · 手机号").font(.spEyebrow).foregroundColor(.spMuted)
            HStack(spacing: 10) {
                Text("+86").font(.system(size: 14, weight: .medium))
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

    private func request() async {
        let ok = await phoneVM.sendResetOtp()
        if ok { navigateToOtp = true }
    }
}

/// 新密码 / 确认密码 —— OTPView(.reset) 通过后跳这里。
struct NewPasswordView: View {

    @ObservedObject var phoneVM: PhoneAuthViewModel

    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var coordinator: AppCoordinator

    @State private var newPassword: String = ""
    @State private var confirmPassword: String = ""
    @State private var showPassword: Bool = false
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                header

                VStack(alignment: .leading, spacing: 14) {
                    Text("RECOVER · 找回密码")
                        .font(.spEyebrow).foregroundColor(.spMuted)
                    VStack(alignment: .leading, spacing: -2) {
                        Text("Set a new").font(.spSerif(32)).foregroundColor(.spPrimary)
                        Text("password.")
                            .font(.spSerif(32, italic: true))
                            .foregroundColor(.spAccent)
                    }
                    Text("为安全考虑，请设置不同于过往 3 次的密码。\n至少 8 位 · 字母 + 数字 · 推荐加符号。")
                        .font(.system(size: 12))
                        .foregroundColor(.spMuted)
                        .lineSpacing(3)
                        .padding(.top, 4)
                }
                .padding(.horizontal, 28)
                .padding(.top, 14)

                newPwdField
                    .padding(.horizontal, 28)
                    .padding(.top, 30)

                rulesList
                    .padding(.horizontal, 28)
                    .padding(.top, 12)

                confirmField
                    .padding(.horizontal, 28)
                    .padding(.top, 22)

                if let err = errorMessage {
                    Text(err)
                        .font(.footnote)
                        .foregroundColor(.spError)
                        .padding(.horizontal, 28)
                        .padding(.top, 10)
                }

                Spacer()

                Button {
                    Task { await submit() }
                } label: {
                    HStack(spacing: 8) {
                        if isLoading {
                            SwiftUI.ProgressView().tint(.spIvory)
                        } else {
                            Text("更新并重新登录")
                                .font(.system(size: 14, weight: .semibold))
                            Image(systemName: "arrow.right")
                        }
                    }
                    .foregroundColor(.spIvory)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(canSubmit ? Color.spPrimary : Color.spPrimary.opacity(0.4))
                    .clipShape(Capsule())
                }
                .disabled(!canSubmit || isLoading)
                .padding(.horizontal, 28)
                .padding(.bottom, 24)
            }
        }
        .navigationBarHidden(true)
    }

    private var header: some View {
        HStack {
            Button { dismiss() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(.spPrimary)
            }
            Spacer()
        }
        .padding(.horizontal, 28)
        .padding(.top, 8)
        .padding(.bottom, 16)
    }

    private var newPwdField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("NEW PASSWORD · 新密码").font(.spEyebrow).foregroundColor(.spMuted)
            HStack {
                Group {
                    if showPassword {
                        TextField("", text: $newPassword)
                    } else {
                        SecureField("", text: $newPassword)
                    }
                }
                .textContentType(.newPassword)
                .font(.system(size: 16))
                .foregroundColor(.spPrimary)

                Button(showPassword ? "HIDE" : "SHOW") { showPassword.toggle() }
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(.spAccent)
            }
            .padding(.bottom, 8)
            .overlay(alignment: .bottom) {
                Rectangle().fill(Color.spPrimary).frame(height: 1)
            }
        }
    }

    private var rulesList: some View {
        VStack(alignment: .leading, spacing: 4) {
            rule(label: "至少 8 位", ok: newPassword.count >= 8)
            rule(label: "含字母", ok: newPassword.contains(where: \.isLetter))
            rule(label: "含数字", ok: newPassword.contains(where: \.isNumber))
            rule(label: "含符号（推荐）",
                 ok: newPassword.contains { !$0.isLetter && !$0.isNumber && !$0.isWhitespace })
        }
    }

    private func rule(label: String, ok: Bool) -> some View {
        HStack(spacing: 8) {
            if ok {
                Image(systemName: "checkmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.spMoss)
            } else {
                Circle().stroke(Color.spLine, lineWidth: 1.5).frame(width: 12, height: 12)
            }
            Text(label).font(.system(size: 11)).foregroundColor(ok ? .spMoss : .spMuted)
            Spacer()
        }
    }

    private var confirmField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("CONFIRM · 确认密码").font(.spEyebrow).foregroundColor(.spMuted)
            HStack {
                SecureField("", text: $confirmPassword)
                    .textContentType(.newPassword)
                    .font(.system(size: 16))
                    .foregroundColor(.spPrimary)
                if !confirmPassword.isEmpty {
                    Text(matched ? "● MATCH" : "● MISMATCH")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundColor(matched ? .spMoss : .spError)
                }
            }
            .padding(.bottom, 8)
            .overlay(alignment: .bottom) {
                Rectangle().fill(Color.spLine).frame(height: 1)
            }
        }
    }

    private var canSubmit: Bool {
        newPassword.count >= 6 &&
        newPassword.contains(where: \.isLetter) &&
        newPassword.contains(where: \.isNumber) &&
        matched
    }

    private var matched: Bool { newPassword == confirmPassword }

    private func submit() async {
        errorMessage = nil
        isLoading = true
        defer { isLoading = false }
        do {
            _ = try await APIClient.shared.resetPassword(
                phone: phoneVM.phone,
                code: phoneVM.code,
                newPassword: newPassword
            )
            // 重置成功 —— 返回登录页让用户手动重登
            dismiss()
        } catch let e as APIError {
            switch e {
            case .serverError(_, let msg) where !msg.isEmpty: errorMessage = msg
            default: errorMessage = e.localizedDescription
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

#Preview("Forgot") {
    NavigationStack { ForgotView() }
}

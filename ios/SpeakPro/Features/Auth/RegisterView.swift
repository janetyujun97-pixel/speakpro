import SwiftUI

/// 手机号注册补全：姓名 + 可选密码 + 协议勾选。
/// 入口：LoginView → OTPView(.login/.register) → RegisterView。
/// 完成后调 `/auth/register-phone`（code 复用 OTP 里的 code），拿 tokens → completeLogin。
struct RegisterView: View {

    @EnvironmentObject private var coordinator: AppCoordinator
    @Environment(\.dismiss) private var dismiss

    /// 从上游传入：已填好 phone & code。若为空（直接进入），view 会提示回到登录。
    @ObservedObject var phoneVM: PhoneAuthViewModel

    @State private var name: String = ""
    @State private var password: String = ""
    @State private var agreed: Bool = false
    @State private var isLoading: Bool = false
    @State private var errorMessage: String? = nil

    init(prefilled: PhoneAuthViewModel) {
        self.phoneVM = prefilled
    }

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 0) {
                header
                eyebrowAndTitle
                    .padding(.horizontal, 28)
                    .padding(.top, 14)
                formFields
                    .padding(.horizontal, 28)
                    .padding(.top, 28)
                Spacer()
                if let err = errorMessage {
                    Text(err)
                        .font(.footnote).foregroundColor(.spError)
                        .padding(.horizontal, 28)
                        .padding(.bottom, 8)
                }
                consentRow
                    .padding(.horizontal, 28)
                primaryButton
                    .padding(.horizontal, 28)
                    .padding(.top, 16)
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

    private var eyebrowAndTitle: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("CREATE ACCOUNT · 注册")
                .font(.spEyebrow).foregroundColor(.spMuted)
            VStack(alignment: .leading, spacing: -2) {
                Text("A rehearsal space,")
                    .font(.spSerif(32)).foregroundColor(.spPrimary)
                Text("just for you.")
                    .font(.spSerif(32, italic: true))
                    .foregroundColor(.spAccent)
            }
        }
    }

    private var formFields: some View {
        VStack(alignment: .leading, spacing: 22) {
            field(label: "NAME · 昵称") {
                TextField("", text: $name, prompt: Text("Chen Wei")
                    .foregroundColor(.spMuted.opacity(0.6))
                )
                    .font(.spSerif(16, italic: true))
                    .foregroundColor(.spPrimary)
                    .textContentType(.name)
            }

            VStack(alignment: .leading, spacing: 8) {
                Text("MOBILE · 手机号")
                    .font(.spEyebrow).foregroundColor(.spMuted)
                HStack(spacing: 10) {
                    Text("+86").font(.system(size: 14, weight: .medium))
                    Rectangle().fill(Color.spLine).frame(width: 1, height: 12)
                    Text(displayPhone)
                        .font(.spSerif(16, italic: true))
                        .foregroundColor(phoneVM.phone.isEmpty ? .spMuted : .spPrimary)
                    Spacer()
                }
                .padding(.bottom, 8)
                .overlay(alignment: .bottom) {
                    Rectangle().fill(Color.spLine).frame(height: 1)
                }
            }

            field(label: "PASSWORD · 密码（可选）") {
                SecureField("至少 6 位", text: $password)
                    .font(.system(size: 16))
                    .foregroundColor(.spPrimary)
                    .textContentType(.newPassword)
            }
        }
    }

    @ViewBuilder
    private func field<Content: View>(
        label: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label).font(.spEyebrow).foregroundColor(.spMuted)
            content()
                .padding(.bottom, 8)
                .overlay(alignment: .bottom) {
                    Rectangle().fill(Color.spLine).frame(height: 1)
                }
        }
    }

    private var consentRow: some View {
        Button {
            agreed.toggle()
        } label: {
            HStack(alignment: .top, spacing: 10) {
                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(agreed ? Color.spAccent : Color.clear)
                    .overlay(
                        RoundedRectangle(cornerRadius: 4, style: .continuous)
                            .stroke(agreed ? Color.spAccent : Color.spLine, lineWidth: 1.5)
                    )
                    .frame(width: 18, height: 18)
                    .overlay {
                        if agreed {
                            Image(systemName: "checkmark")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(.spIvory)
                        }
                    }
                Text("我已阅读并同意《用户协议》与《隐私政策》，理解我的录音数据会用于发音评估，不会用于训练第三方模型。")
                    .font(.system(size: 11))
                    .foregroundColor(.spMuted)
                    .multilineTextAlignment(.leading)
                    .lineSpacing(3)
                Spacer(minLength: 0)
            }
        }
        .buttonStyle(.plain)
    }

    private var primaryButton: some View {
        Button {
            Task { await submit() }
        } label: {
            HStack(spacing: 8) {
                if isLoading {
                    SwiftUI.ProgressView().tint(.spIvory)
                } else {
                    Text("创建账号并开始").font(.system(size: 14, weight: .semibold))
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
    }

    // MARK: - Logic

    private var canSubmit: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        phoneVM.isPhoneValid &&
        phoneVM.isCodeValid &&
        agreed
    }

    private var displayPhone: String {
        let raw = phoneVM.phone
        if raw.isEmpty { return "138 0013 8000" }
        if raw.count == 11 {
            let idx3 = raw.index(raw.startIndex, offsetBy: 3)
            let idx7 = raw.index(raw.startIndex, offsetBy: 7)
            return "\(raw[raw.startIndex..<idx3]) \(raw[idx3..<idx7]) \(raw[idx7...])"
        }
        return raw
    }

    private func submit() async {
        errorMessage = nil
        guard canSubmit else { return }
        isLoading = true
        defer { isLoading = false }

        do {
            let resp = try await APIClient.shared.registerWithPhone(
                phone: phoneVM.phone,
                code: phoneVM.code,
                name: name.trimmingCharacters(in: .whitespaces),
                password: password.isEmpty ? nil : password
            )
            APIClient.shared.accessToken  = resp.accessToken
            APIClient.shared.refreshToken = resp.refreshToken
            coordinator.completeLogin(user: resp.user)
        } catch let e as APIError {
            errorMessage = humanize(e)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func humanize(_ e: APIError) -> String {
        switch e {
        case .serverError(_, let msg) where !msg.isEmpty: return msg
        case .unauthorized:                               return "凭证失效，请重新获取验证码"
        case .networkError:                               return "网络连接失败"
        default:                                          return e.localizedDescription
        }
    }
}

#Preview {
    NavigationStack {
        RegisterView(prefilled: PhoneAuthViewModel())
            .environmentObject(AppCoordinator())
    }
}

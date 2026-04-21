import SwiftUI

/// OTP 校验视图 — 6 格数字输入 + 倒计时重发。
/// 通过 `flow` 区分：
///   - .login：验证成功后直接登录（调 verify-otp + 后续由外层已跳 navigateToOtp 的入口决定）
///     ※ 当前后端 verify-otp 只校验不登录；登录靠 register-phone 或直接用已有账号的 /auth/login
///     ※ 一期简化：OTP 验证通过后走 register-phone（首登即注册的语义对齐 §2 设计）
///   - .register：验证成功后跳注册页续填姓名/密码
///   - .reset：验证成功后跳新密码页
struct OTPView: View {

    enum Flow {
        case login            // 首登即注册 —— 验证通过后前端让用户补姓名（进 Register）
        case register         // 注册流程 —— 从 RegisterView 过来
        case reset            // 重置流程 —— 从 ForgotView 过来
    }

    @EnvironmentObject private var coordinator: AppCoordinator
    @ObservedObject var phoneVM: PhoneAuthViewModel
    let flow: Flow

    @Environment(\.dismiss) private var dismiss
    @FocusState private var focused: Bool
    @State private var navigateNext = false

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                header

                eyebrowAndTitle
                    .padding(.horizontal, 28)
                    .padding(.top, 14)

                caption
                    .padding(.horizontal, 28)
                    .padding(.top, 12)

                codeBoxes
                    .padding(.horizontal, 28)
                    .padding(.top, 36)

                resendRow
                    .padding(.horizontal, 28)
                    .padding(.top, 20)

                if let err = phoneVM.errorMessage {
                    Text(err)
                        .font(.footnote)
                        .foregroundColor(.spError)
                        .padding(.horizontal, 28)
                        .padding(.top, 12)
                }

                Spacer()

                primaryButton
                    .padding(.horizontal, 28)
                    .padding(.bottom, 24)
            }
        }
        .navigationBarHidden(true)
        .onAppear { focused = true }
        .navigationDestination(isPresented: $navigateNext) {
            destinationView
                .environmentObject(coordinator)
        }
    }

    // MARK: - Subviews

    private var header: some View {
        HStack {
            Button {
                dismiss()
            } label: {
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
        VStack(alignment: .leading, spacing: 2) {
            Text("STEP · 02 / 02")
                .font(.spEyebrow).foregroundColor(.spMuted)
            VStack(alignment: .leading, spacing: 0) {
                Text("Enter the").font(.spSerif(32)).foregroundColor(.spPrimary)
                Text("six digits.")
                    .font(.spSerif(32, italic: true))
                    .foregroundColor(.spAccent)
                    .padding(.top, -4)
            }
            .padding(.top, 12)
        }
    }

    private var caption: some View {
        Text("验证码已发送至 +86 \(formattedPhone)")
            .font(.system(size: 13))
            .foregroundColor(.spMuted)
    }

    private var codeBoxes: some View {
        ZStack {
            // 隐形输入框，承接键盘
            TextField("", text: $phoneVM.code)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .focused($focused)
                .opacity(0.01)
                .onChange(of: phoneVM.code) { _, newValue in
                    let filtered = newValue.filter(\.isNumber)
                    let trimmed = String(filtered.prefix(6))
                    if trimmed != newValue {
                        phoneVM.code = trimmed
                    }
                }

            HStack(spacing: 8) {
                ForEach(0..<6, id: \.self) { i in
                    codeCell(index: i)
                }
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { focused = true }
    }

    private func codeCell(index i: Int) -> some View {
        let chars = Array(phoneVM.code)
        let filled = i < chars.count
        let active = !filled && i == chars.count
        let ch = filled ? String(chars[i]) : ""

        return Text(ch.isEmpty ? " " : ch)
            .font(.spSerif(28))
            .foregroundColor(filled ? .spIvory : .spPrimary)
            .frame(maxWidth: .infinity)
            .aspectRatio(1/1.15, contentMode: .fit)
            .background(filled ? Color.spPrimary : Color.spIvory)
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(
                        active ? Color.spAccent : (filled ? Color.spPrimary : Color.spLine),
                        lineWidth: active ? 2 : 1
                    )
            )
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private var resendRow: some View {
        HStack {
            Text("没收到？").font(.system(size: 12)).foregroundColor(.spMuted)
            Spacer()
            if phoneVM.cooldownSec > 0 {
                Text("重新发送 · \(phoneVM.cooldownSec)s")
                    .font(.spSerif(12, italic: true))
                    .foregroundColor(.spMuted)
            } else {
                Button("重新发送") {
                    Task { _ = await phoneVM.sendOtp() }
                }
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(.spPrimary)
            }
        }
    }

    private var primaryButton: some View {
        Button {
            Task { await verify() }
        } label: {
            HStack(spacing: 8) {
                if phoneVM.isVerifying {
                    SwiftUI.ProgressView().tint(.spIvory)
                } else {
                    Text(primaryLabel).font(.system(size: 14, weight: .semibold))
                    Image(systemName: "arrow.right")
                }
            }
            .foregroundColor(.spIvory)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(phoneVM.isCodeValid ? Color.spPrimary : Color.spPrimary.opacity(0.4))
            .clipShape(Capsule())
        }
        .disabled(!phoneVM.isCodeValid || phoneVM.isVerifying)
    }

    private var primaryLabel: String {
        switch flow {
        case .login:    return "验证并继续"
        case .register: return "验证并继续"
        case .reset:    return "验证并重置密码"
        }
    }

    @ViewBuilder
    private var destinationView: some View {
        switch flow {
        case .login:
            // 首登即注册：验证通过后跳到 RegisterView 补姓名；VM 共享 phone + code
            RegisterView(prefilled: phoneVM)
        case .register:
            RegisterView(prefilled: phoneVM)
        case .reset:
            NewPasswordView(phoneVM: phoneVM)
        }
    }

    private var formattedPhone: String {
        let raw = phoneVM.phone
        guard raw.count == 11 else { return raw }
        let idx3 = raw.index(raw.startIndex, offsetBy: 3)
        let idx7 = raw.index(raw.startIndex, offsetBy: 7)
        return "\(raw[raw.startIndex..<idx3]) \(raw[idx3..<idx7]) \(raw[idx7...])"
    }

    // MARK: - Actions

    private func verify() async {
        let ok = await phoneVM.verifyOtp()
        if ok { navigateNext = true }
    }
}

#Preview {
    NavigationStack {
        OTPView(phoneVM: PhoneAuthViewModel(), flow: .login)
            .environmentObject(AppCoordinator())
    }
}

import Foundation
import SwiftUI

/// 手机号 + OTP 流程的共享 VM。
/// 由 LoginView（发送验证码）、OTPView（验证登录/注册）、RegisterView / ForgotView 复用。
@MainActor
final class PhoneAuthViewModel: ObservableObject {

    // MARK: - Form state

    @Published var phone: String = ""
    @Published var code: String = ""                // 6 位
    @Published var cooldownSec: Int = 0             // 剩余冷却秒数
    @Published var isSending: Bool = false
    @Published var isVerifying: Bool = false
    @Published var errorMessage: String? = nil

    // MARK: - Deps

    private let apiClient: APIClient
    private var cooldownTimer: Timer?

    init(apiClient: APIClient = .shared) {
        self.apiClient = apiClient
    }

    // MARK: - Validation

    var isPhoneValid: Bool {
        let re = try? NSRegularExpression(pattern: "^1[3-9]\\d{9}$")
        return re?.firstMatch(
            in: phone,
            range: NSRange(phone.startIndex..., in: phone)
        ) != nil
    }

    var isCodeValid: Bool {
        code.count == 6 && code.allSatisfy(\.isNumber)
    }

    // MARK: - Actions

    /// 下发验证码（供登录 / 注册 / 忘记密码共用）
    func sendOtp() async -> Bool {
        guard isPhoneValid else {
            errorMessage = "请输入有效的手机号"
            return false
        }
        isSending = true
        errorMessage = nil
        defer { isSending = false }
        do {
            let resp = try await apiClient.sendOtp(phone: phone)
            startCooldown(seconds: resp.cooldownSec)
            return true
        } catch let e as APIError {
            errorMessage = humanize(e) ?? "发送失败"
            return false
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    /// 忘记密码流程下发（走 /auth/request-reset，逻辑同 sendOtp）
    func sendResetOtp() async -> Bool {
        guard isPhoneValid else {
            errorMessage = "请输入有效的手机号"
            return false
        }
        isSending = true
        errorMessage = nil
        defer { isSending = false }
        do {
            let resp = try await apiClient.requestPasswordReset(phone: phone)
            startCooldown(seconds: resp.cooldownSec)
            return true
        } catch let e as APIError {
            errorMessage = humanize(e) ?? "发送失败"
            return false
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    /// 校验 OTP（不消费；register / reset 会再消费一次）
    func verifyOtp() async -> Bool {
        guard isCodeValid else {
            errorMessage = "请输入 6 位验证码"
            return false
        }
        isVerifying = true
        errorMessage = nil
        defer { isVerifying = false }
        do {
            _ = try await apiClient.verifyOtp(phone: phone, code: code)
            return true
        } catch let e as APIError {
            errorMessage = humanize(e) ?? "验证失败"
            return false
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    // MARK: - Cooldown timer

    private func startCooldown(seconds: Int) {
        cooldownSec = seconds
        cooldownTimer?.invalidate()
        cooldownTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] t in
            Task { @MainActor in
                guard let self else { t.invalidate(); return }
                if self.cooldownSec > 0 {
                    self.cooldownSec -= 1
                } else {
                    t.invalidate()
                    self.cooldownTimer = nil
                }
            }
        }
    }

    func reset() {
        code = ""
        errorMessage = nil
    }

    // MARK: - Error mapping

    private func humanize(_ e: APIError) -> String? {
        switch e {
        case .unauthorized:            return "验证码错误"
        case .serverError(_, let msg): return msg.isEmpty ? nil : msg
        case .networkError:            return "网络连接失败"
        default:                       return e.localizedDescription
        }
    }
}

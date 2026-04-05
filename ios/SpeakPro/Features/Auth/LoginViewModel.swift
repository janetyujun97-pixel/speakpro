import Foundation
import SwiftUI

// MARK: - 登录请求/响应模型

private struct LoginRequest: Encodable {
    let email: String
    let password: String
}

private struct LoginResponseData: Decodable {
    let accessToken: String
    let refreshToken: String
    let user: UserInfo
}

struct UserInfo: Codable, Equatable {
    let id: String
    let name: String
    let email: String
    let role: String
}

// MARK: - LoginViewModel

@MainActor
final class LoginViewModel: ObservableObject {

    // MARK: - Published State

    @Published var email: String = ""
    @Published var password: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    // MARK: - Dependencies

    private let apiClient: APIClient
    private let coordinator: AppCoordinator

    init(coordinator: AppCoordinator, apiClient: APIClient = .shared) {
        self.coordinator = coordinator
        self.apiClient = apiClient
    }

    // MARK: - Validation

    var isFormValid: Bool {
        !email.trimmingCharacters(in: .whitespaces).isEmpty &&
        !password.isEmpty &&
        email.contains("@")
    }

    // MARK: - Actions

    /// 执行登录请求
    func login() async {
        guard isFormValid else {
            errorMessage = "请输入有效的邮箱和密码"
            return
        }

        isLoading = true
        errorMessage = nil

        defer { isLoading = false }

        do {
            let body = LoginRequest(
                email: email.trimmingCharacters(in: .whitespaces).lowercased(),
                password: password
            )

            let response: APIResponse<LoginResponseData> = try await apiClient.post(
                Endpoints.Auth.login,
                body: body
            )

            guard let data = response.data, response.code == 0 else {
                errorMessage = response.message.isEmpty ? "登录失败，请重试" : response.message
                return
            }

            // 保存 Token 到 Keychain
            apiClient.accessToken  = data.accessToken
            apiClient.refreshToken = data.refreshToken

            // 通知 Coordinator 登录成功
            coordinator.completeLogin(user: data.user)

        } catch let error as APIError {
            switch error {
            case .unauthorized:
                errorMessage = "邮箱或密码错误"
            case .serverError(_, let msg):
                errorMessage = msg
            case .networkError:
                errorMessage = "网络连接失败，请检查网络"
            default:
                errorMessage = error.localizedDescription
            }
        } catch {
            errorMessage = "登录失败: \(error.localizedDescription)"
        }
    }

    /// 清除错误信息
    func clearError() {
        errorMessage = nil
    }
}

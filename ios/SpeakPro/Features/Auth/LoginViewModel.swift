import Foundation
import SwiftUI

// MARK: - 登录请求/响应模型（保留既有 email 邮箱登录）

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
    let email: String?
    let phone: String?
    let role: String
    let avatarUrl: String?

    var emailOrEmpty: String { email ?? "" }
}

// MARK: - LoginViewModel — 邮箱密码登录（三 tab 中的 "Password" 入口）

@MainActor
final class LoginViewModel: ObservableObject {

    @Published var email: String = ""
    @Published var password: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    private let apiClient: APIClient
    private let coordinator: AppCoordinator

    init(coordinator: AppCoordinator, apiClient: APIClient = .shared) {
        self.coordinator = coordinator
        self.apiClient = apiClient
    }

    var isFormValid: Bool {
        !email.trimmingCharacters(in: .whitespaces).isEmpty &&
        !password.isEmpty &&
        email.contains("@")
    }

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
            apiClient.accessToken  = data.accessToken
            apiClient.refreshToken = data.refreshToken
            coordinator.completeLogin(user: data.user)
        } catch let error as APIError {
            switch error {
            case .unauthorized:            errorMessage = "邮箱或密码错误"
            case .serverError(_, let msg): errorMessage = msg
            case .networkError:            errorMessage = "网络连接失败，请检查网络"
            default:                       errorMessage = error.localizedDescription
            }
        } catch {
            errorMessage = "登录失败: \(error.localizedDescription)"
        }
    }

    func clearError() { errorMessage = nil }
}

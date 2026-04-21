import Foundation

// MARK: - Auth v2 请求 / 响应模型

/// 登录/注册/三方登录成功后的统一返回体（对应 Nest AuthService.signResponse）
struct AuthTokenResponse: Decodable {
    let accessToken: String
    let refreshToken: String
    let user: UserInfo
}

struct SendOtpRequest: Encodable { let phone: String }
struct SendOtpResponse: Decodable { let cooldownSec: Int }

struct VerifyOtpRequest: Encodable {
    let phone: String
    let code: String
}
struct VerifyOtpResponse: Decodable { let ok: Bool }

struct RegisterPhoneRequest: Encodable {
    let phone: String
    let code: String
    let name: String
    let password: String?
}

struct RequestResetRequest: Encodable { let phone: String }

struct ResetPasswordRequest: Encodable {
    let phone: String
    let code: String
    let newPassword: String
}
struct ResetPasswordResponse: Decodable { let ok: Bool }

struct AppleSignInRequest: Encodable {
    let identityToken: String
    let authorizationCode: String?
    let nonce: String?
    let name: String?
}

struct WechatSignInRequest: Encodable { let code: String }

// MARK: - APIClient + Auth v2

extension APIClient {

    @discardableResult
    func sendOtp(phone: String) async throws -> SendOtpResponse {
        let resp: APIResponse<SendOtpResponse> = try await post(
            Endpoints.Auth.sendOtp,
            body: SendOtpRequest(phone: phone)
        )
        return try unwrap(resp)
    }

    @discardableResult
    func verifyOtp(phone: String, code: String) async throws -> VerifyOtpResponse {
        let resp: APIResponse<VerifyOtpResponse> = try await post(
            Endpoints.Auth.verifyOtp,
            body: VerifyOtpRequest(phone: phone, code: code)
        )
        return try unwrap(resp)
    }

    func registerWithPhone(
        phone: String,
        code: String,
        name: String,
        password: String?
    ) async throws -> AuthTokenResponse {
        let resp: APIResponse<AuthTokenResponse> = try await post(
            Endpoints.Auth.registerPhone,
            body: RegisterPhoneRequest(
                phone: phone, code: code, name: name, password: password
            )
        )
        return try unwrap(resp)
    }

    @discardableResult
    func requestPasswordReset(phone: String) async throws -> SendOtpResponse {
        let resp: APIResponse<SendOtpResponse> = try await post(
            Endpoints.Auth.requestReset,
            body: RequestResetRequest(phone: phone)
        )
        return try unwrap(resp)
    }

    @discardableResult
    func resetPassword(
        phone: String,
        code: String,
        newPassword: String
    ) async throws -> ResetPasswordResponse {
        let resp: APIResponse<ResetPasswordResponse> = try await post(
            Endpoints.Auth.resetPassword,
            body: ResetPasswordRequest(
                phone: phone, code: code, newPassword: newPassword
            )
        )
        return try unwrap(resp)
    }

    func signInWithApple(
        identityToken: String,
        authorizationCode: String?,
        nonce: String?,
        name: String?
    ) async throws -> AuthTokenResponse {
        let resp: APIResponse<AuthTokenResponse> = try await post(
            Endpoints.Auth.apple,
            body: AppleSignInRequest(
                identityToken: identityToken,
                authorizationCode: authorizationCode,
                nonce: nonce,
                name: name
            )
        )
        return try unwrap(resp)
    }

    func signInWithWechat(code: String) async throws -> AuthTokenResponse {
        let resp: APIResponse<AuthTokenResponse> = try await post(
            Endpoints.Auth.wechat,
            body: WechatSignInRequest(code: code)
        )
        return try unwrap(resp)
    }

    // MARK: - 辅助：从 APIResponse 解出 data，否则抛 serverError

    fileprivate func unwrap<T>(_ resp: APIResponse<T>) throws -> T {
        if resp.code != 0 {
            throw APIError.serverError(resp.code, resp.message)
        }
        guard let data = resp.data else {
            throw APIError.noData
        }
        return data
    }
}

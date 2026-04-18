import AuthenticationServices
import CryptoKit
import Foundation
import UIKit

/// Apple Sign-In 胶水层。
/// - 生成一次性 nonce（SHA256 hash 作为 rawNonce 的替身传给 Apple；原 nonce 留下来传给后端）
/// - 走 ASAuthorizationAppleIDProvider 交互，拿到 identityToken / authorizationCode
/// - 交给 APIClient.signInWithApple，换 SpeakPro JWT
///
/// 当前服务端若未配置 APPLE_CLIENT_ID，后端会返回 503 —— UI 需要把这个错误做友好提示。
@MainActor
final class AppleSignInCoordinator: NSObject,
    ASAuthorizationControllerDelegate,
    ASAuthorizationControllerPresentationContextProviding
{
    typealias Completion = (Result<AuthTokenResponse, Error>) -> Void

    enum AppleSignInError: LocalizedError {
        case cancelled
        case missingCredential
        case missingIdentityToken
        case unavailable

        var errorDescription: String? {
            switch self {
            case .cancelled:            return "已取消"
            case .missingCredential:    return "Apple 凭证缺失"
            case .missingIdentityToken: return "identityToken 缺失"
            case .unavailable:          return "Apple 登录暂未开通"
            }
        }
    }

    private var completion: Completion?
    private var rawNonce: String?

    private let apiClient: APIClient

    init(apiClient: APIClient = .shared) {
        self.apiClient = apiClient
        super.init()
    }

    // MARK: - Public

    func start(_ completion: @escaping Completion) {
        self.completion = completion

        let rawNonce = Self.makeNonce()
        self.rawNonce = rawNonce

        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(rawNonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    // MARK: - Delegate

    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        Task { @MainActor in
            await handleSuccess(authorization)
        }
    }

    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        Task { @MainActor in
            let err: Error
            if let authError = error as? ASAuthorizationError,
               authError.code == .canceled {
                err = AppleSignInError.cancelled
            } else {
                err = error
            }
            completion?(.failure(err))
            completion = nil
        }
    }

    nonisolated func presentationAnchor(
        for controller: ASAuthorizationController
    ) -> ASPresentationAnchor {
        // 主线程安全获取 keyWindow
        if Thread.isMainThread {
            return topWindowSync() ?? ASPresentationAnchor()
        }
        return DispatchQueue.main.sync { topWindowSync() ?? ASPresentationAnchor() }
    }

    // MARK: - Private

    @MainActor
    private func handleSuccess(_ authorization: ASAuthorization) async {
        guard let credential = authorization.credential
                as? ASAuthorizationAppleIDCredential else {
            completion?(.failure(AppleSignInError.missingCredential))
            completion = nil
            return
        }
        guard let tokenData = credential.identityToken,
              let token = String(data: tokenData, encoding: .utf8) else {
            completion?(.failure(AppleSignInError.missingIdentityToken))
            completion = nil
            return
        }
        let authCode: String? = credential.authorizationCode
            .flatMap { String(data: $0, encoding: .utf8) }
        let name = credential.fullName.flatMap(formatName(_:))

        do {
            let resp = try await apiClient.signInWithApple(
                identityToken: token,
                authorizationCode: authCode,
                nonce: rawNonce,
                name: name
            )
            completion?(.success(resp))
        } catch {
            completion?(.failure(error))
        }
        completion = nil
    }

    private func formatName(_ name: PersonNameComponents) -> String? {
        let formatter = PersonNameComponentsFormatter()
        formatter.style = .default
        let s = formatter.string(from: name)
        return s.isEmpty ? nil : s
    }

    private nonisolated func topWindowSync() -> ASPresentationAnchor? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
    }

    // MARK: - Nonce

    private static func makeNonce(length: Int = 32) -> String {
        precondition(length > 0)
        let charset: [Character] = Array(
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
        )
        var result = ""
        var remaining = length
        while remaining > 0 {
            var randoms: [UInt8] = Array(repeating: 0, count: 16)
            _ = SecRandomCopyBytes(kSecRandomDefault, randoms.count, &randoms)
            for r in randoms where remaining > 0 {
                if r < charset.count {
                    result.append(charset[Int(r) % charset.count])
                    remaining -= 1
                }
            }
        }
        return result
    }

    private static func sha256(_ s: String) -> String {
        let data = Data(s.utf8)
        let hash = SHA256.hash(data: data)
        return hash.map { String(format: "%02x", $0) }.joined()
    }
}

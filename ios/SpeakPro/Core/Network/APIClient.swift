import Foundation

// MARK: - API Response Wrapper

struct APIResponse<T: Decodable>: Decodable {
    let code: Int
    let message: String
    let data: T?
}

// MARK: - API Error

enum APIError: Error, LocalizedError {
    case invalidURL
    case noData
    case decodingFailed
    case unauthorized
    case serverError(Int, String)
    case networkError(Error)

    var errorDescription: String? {
        switch self {
        case .invalidURL:              return "无效的 URL"
        case .noData:                  return "服务器未返回数据"
        case .decodingFailed:          return "数据解析失败"
        case .unauthorized:            return "身份验证失败，请重新登录"
        case .serverError(let c, let m): return "服务器错误 (\(c)): \(m)"
        case .networkError(let e):     return e.localizedDescription
        }
    }
}

// MARK: - HTTP Method

enum HTTPMethod: String {
    case get    = "GET"
    case post   = "POST"
    case put    = "PUT"
    case patch  = "PATCH"
    case delete = "DELETE"
}

// MARK: - API Client (Singleton)

final class APIClient {

    static let shared = APIClient()

    private let session: URLSession
    private let decoder: JSONDecoder

    var baseURL: String {
        Endpoints.baseURL
    }

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        session = URLSession(configuration: config)

        decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        decoder.dateDecodingStrategy = .iso8601
    }

    // MARK: - Token Management

    var accessToken: String? {
        get { KeychainManager.get(key: .accessToken) }
        set {
            if let value = newValue {
                KeychainManager.save(key: .accessToken, value: value)
            } else {
                KeychainManager.delete(key: .accessToken)
            }
        }
    }

    var refreshToken: String? {
        get { KeychainManager.get(key: .refreshToken) }
        set {
            if let value = newValue {
                KeychainManager.save(key: .refreshToken, value: value)
            } else {
                KeychainManager.delete(key: .refreshToken)
            }
        }
    }

    // MARK: - Generic Request

    func request<T: Decodable>(
        _ method: HTTPMethod,
        path: String,
        body: Encodable? = nil,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> APIResponse<T> {
        try await send(method, path: path, body: body, queryItems: queryItems, allowRetry: true)
    }

    private func send<T: Decodable>(
        _ method: HTTPMethod,
        path: String,
        body: Encodable?,
        queryItems: [URLQueryItem]?,
        allowRetry: Bool
    ) async throws -> APIResponse<T> {

        // 如果 path 已经是完整 URL（以 http 开头），直接使用；否则拼接 baseURL
        let fullURLString = path.hasPrefix("http") ? path : (baseURL + path)
        guard var components = URLComponents(string: fullURLString) else {
            throw APIError.invalidURL
        }
        components.queryItems = queryItems

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = method.rawValue
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // full-evaluate 等 AI 密集型端点需要更长超时
        if path.contains("full-evaluate") {
            urlRequest.timeoutInterval = 120 // 2 分钟
        }

        // 自动附加 Authorization header
        if let token = accessToken {
            urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body = body {
            let encoder = JSONEncoder()
            encoder.keyEncodingStrategy = .convertToSnakeCase
            urlRequest.httpBody = try encoder.encode(AnyEncodable(body))
        }

        do {
            let (data, response) = try await session.data(for: urlRequest)

            if let httpResponse = response as? HTTPURLResponse {
                switch httpResponse.statusCode {
                case 401:
                    // access token 过期：用 refresh token 换新后重试一次。
                    // auth 自身的端点（登录/刷新等）401 不触发刷新，避免递归
                    if allowRetry, !path.contains("/auth/"), refreshToken != nil {
                        try await refreshCoordinator.run { [weak self] in
                            try await self?.performTokenRefresh()
                        }
                        return try await send(
                            method, path: path, body: body,
                            queryItems: queryItems, allowRetry: false
                        )
                    }
                    throw APIError.unauthorized
                case 400..<600:
                    throw APIError.serverError(httpResponse.statusCode, "请求失败")
                default:
                    break
                }
            }

            let apiResponse = try decoder.decode(APIResponse<T>.self, from: data)
            return apiResponse
        } catch let error as APIError {
            throw error
        } catch let error as DecodingError {
            print("[APIClient] Decoding error: \(error)")
            throw APIError.decodingFailed
        } catch {
            throw APIError.networkError(error)
        }
    }

    // MARK: - Token 刷新（单飞：并发 401 只触发一次刷新）

    private let refreshCoordinator = RefreshCoordinator()

    private struct RefreshedTokens: Decodable {
        let accessToken: String
        let refreshToken: String
    }

    /// 调 /auth/refresh 换新一对 token（服务端每次轮换，滑动续期）。
    /// refresh token 也失效时清空本地凭证并抛 unauthorized，由 UI 引导重新登录。
    private func performTokenRefresh() async throws {
        guard let refresh = refreshToken else { throw APIError.unauthorized }

        guard let url = URL(string: baseURL + Endpoints.Auth.refresh) else {
            throw APIError.invalidURL
        }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        // 注意：这里不用 snake_case 策略，服务端字段就是 refreshToken
        req.httpBody = try JSONEncoder().encode(["refreshToken": refresh])

        let (data, response) = try await session.data(for: req)
        guard let http = response as? HTTPURLResponse, http.statusCode < 400,
              let resp = try? decoder.decode(APIResponse<RefreshedTokens>.self, from: data),
              resp.code == 0, let tokens = resp.data
        else {
            // refresh token 失效（180 天未用 / 账号被禁用）→ 清除凭证
            accessToken = nil
            refreshToken = nil
            throw APIError.unauthorized
        }

        accessToken = tokens.accessToken
        refreshToken = tokens.refreshToken
    }

    // MARK: - Convenience Methods

    func get<T: Decodable>(
        _ path: String,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> APIResponse<T> {
        try await request(.get, path: path, queryItems: queryItems)
    }

    func post<T: Decodable>(
        _ path: String,
        body: Encodable? = nil
    ) async throws -> APIResponse<T> {
        try await request(.post, path: path, body: body)
    }

    func put<T: Decodable>(
        _ path: String,
        body: Encodable? = nil
    ) async throws -> APIResponse<T> {
        try await request(.put, path: path, body: body)
    }

    func patch<T: Decodable>(
        _ path: String,
        body: Encodable? = nil
    ) async throws -> APIResponse<T> {
        try await request(.patch, path: path, body: body)
    }

    func delete<T: Decodable>(
        _ path: String
    ) async throws -> APIResponse<T> {
        try await request(.delete, path: path)
    }
}

// MARK: - Refresh 单飞协调器

/// 串行化 token 刷新：同一时刻只允许一次刷新在途，并发 401 等待并复用同一结果
private actor RefreshCoordinator {
    private var inFlight: Task<Void, Error>?

    func run(_ operation: @escaping @Sendable () async throws -> Void) async throws {
        if let inFlight {
            return try await inFlight.value
        }
        let task = Task { try await operation() }
        inFlight = task
        defer { inFlight = nil }
        try await task.value
    }
}

// MARK: - Type-erased Encodable wrapper

private struct AnyEncodable: Encodable {
    private let _encode: (Encoder) throws -> Void

    init(_ wrapped: Encodable) {
        _encode = wrapped.encode
    }

    func encode(to encoder: Encoder) throws {
        try _encode(encoder)
    }
}

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

        guard var components = URLComponents(string: baseURL + path) else {
            throw APIError.invalidURL
        }
        components.queryItems = queryItems

        guard let url = components.url else {
            throw APIError.invalidURL
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = method.rawValue
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

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
                    // TODO: 尝试刷新 token 后重试
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

    func delete<T: Decodable>(
        _ path: String
    ) async throws -> APIResponse<T> {
        try await request(.delete, path: path)
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

import Foundation

// MARK: - 历史时间线

struct SessionAudioResponse: Decodable {
    let sessionId: String
    let audioUrl: String?
    /// 签名 URL 的有效期；nil 表示未签名（OSS 凭证未配置时直接走 audioUrl）
    let expiresInSec: Int?
    let signed: Bool
}

struct PracticeSessionListItem: Decodable, Identifiable {
    let id: String
    let mode: String
    let audioUrl: String?
    let transcript: String?
    let durationSec: Int?
    let overallScore: Double?
    let createdAt: Date
    let question: SessionQuestion?

    struct SessionQuestion: Decodable {
        let id: String
        let promptText: String?
        let examType: String?
        let section: String?
    }
}

// MARK: - Notebook

enum NotebookFilter: String {
    case due, mastered, all
}

struct NotebookWord: Codable, Identifiable {
    let id: String
    let userId: String
    let word: String
    let ipa: String?
    let note: String?
    let sourceSessionId: String?
    let missCount: Int
    let lastSeenAt: Date?
    let masteredAt: Date?
    let nextReviewAt: Date?
    let ef: Double
    let intervalDays: Int
    let createdAt: Date
}

struct NotebookPhrase: Codable, Identifiable {
    let id: String
    let userId: String
    let phrase: String
    let note: String?
    let useCount: Int
    let lastSeenAt: Date?
    let createdAt: Date
}

struct ReviewRequest: Encodable {
    /// SM-2 质量分：0-5；UI 一般只给 2（没想起来）/ 4（想起来了）
    let quality: Int
}

struct OkResponse: Decodable { let ok: Bool }

// MARK: - Notifications

enum NotificationKind: String, Decodable {
    case homework, feedback, streak, reminder, system
    case unknown

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let raw = try container.decode(String.self)
        self = NotificationKind(rawValue: raw) ?? .unknown
    }
}

struct NotificationItem: Decodable, Identifiable {
    let id: String
    let userId: String
    let kind: NotificationKind
    let title: String
    let body: String
    let payload: [String: AnyJSON]?
    let isRead: Bool
    let createdAt: Date
}

struct NotificationListResponse: Decodable {
    let items: [NotificationItem]
    let unread: Int
}

struct NotificationPrefs: Codable {
    let userId: String
    let quietStart: String    // "HH:MM:SS"
    let quietEnd: String
    let pushEnabled: Bool
    let updatedAt: Date?
}

struct UpdatePrefsRequest: Encodable {
    var quietStart: String?
    var quietEnd: String?
    var pushEnabled: Bool?
}

/// 轻量 JSON 容器 —— payload 字段是任意结构，用 AnyJSON 防崩溃
enum AnyJSON: Decodable {
    case string(String)
    case int(Int)
    case double(Double)
    case bool(Bool)
    case null

    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if c.decodeNil() { self = .null; return }
        if let v = try? c.decode(Bool.self) { self = .bool(v); return }
        if let v = try? c.decode(Int.self) { self = .int(v); return }
        if let v = try? c.decode(Double.self) { self = .double(v); return }
        if let v = try? c.decode(String.self) { self = .string(v); return }
        self = .null
    }

    var stringValue: String? {
        switch self {
        case .string(let s): return s
        case .int(let i):    return String(i)
        case .double(let d): return String(d)
        case .bool(let b):   return String(b)
        case .null:          return nil
        }
    }
}

// MARK: - APIClient extensions

extension APIClient {

    // ========== History ==========

    func getSessions() async throws -> [PracticeSessionListItem] {
        let resp: APIResponse<[PracticeSessionListItem]> =
            try await get(Endpoints.Practice.sessions)
        return try unwrapReview(resp)
    }

    func getSessionAudio(id: String) async throws -> SessionAudioResponse {
        let resp: APIResponse<SessionAudioResponse> =
            try await get(Endpoints.Practice.sessionAudio(id: id))
        return try unwrapReview(resp)
    }

    // ========== Notebook ==========

    func getNotebookWords(filter: NotebookFilter = .all) async throws -> [NotebookWord] {
        let query = [URLQueryItem(name: "filter", value: filter.rawValue)]
        let resp: APIResponse<[NotebookWord]> =
            try await get(Endpoints.Notebook.words, queryItems: query)
        return try unwrapReview(resp)
    }

    func getNotebookPhrases() async throws -> [NotebookPhrase] {
        let resp: APIResponse<[NotebookPhrase]> =
            try await get(Endpoints.Notebook.phrases)
        return try unwrapReview(resp)
    }

    @discardableResult
    func reviewNotebookWord(id: String, quality: Int) async throws -> NotebookWord {
        let resp: APIResponse<NotebookWord> = try await post(
            Endpoints.Notebook.reviewed(id: id),
            body: ReviewRequest(quality: quality),
        )
        return try unwrapReview(resp)
    }

    @discardableResult
    func masterNotebookWord(id: String) async throws -> NotebookWord {
        let resp: APIResponse<NotebookWord> =
            try await post(Endpoints.Notebook.master(id: id), body: EmptyBody())
        return try unwrapReview(resp)
    }

    @discardableResult
    func deleteNotebookWord(id: String) async throws -> OkResponse {
        let resp: APIResponse<OkResponse> =
            try await delete(Endpoints.Notebook.deleteWord(id: id))
        return try unwrapReview(resp)
    }

    // ========== Notifications ==========

    func getNotifications(limit: Int = 50) async throws -> NotificationListResponse {
        let q = [URLQueryItem(name: "limit", value: String(limit))]
        let resp: APIResponse<NotificationListResponse> =
            try await get(Endpoints.Notifications.list, queryItems: q)
        return try unwrapReview(resp)
    }

    @discardableResult
    func markAllNotificationsRead() async throws -> [String: Int] {
        let resp: APIResponse<[String: Int]> =
            try await patch(Endpoints.Notifications.readAll, body: EmptyBody())
        return try unwrapReview(resp)
    }

    @discardableResult
    func markNotificationRead(id: String) async throws -> NotificationItem {
        let resp: APIResponse<NotificationItem> =
            try await patch(Endpoints.Notifications.read(id: id), body: EmptyBody())
        return try unwrapReview(resp)
    }

    func getNotificationPrefs() async throws -> NotificationPrefs {
        let resp: APIResponse<NotificationPrefs> =
            try await get(Endpoints.Notifications.prefs)
        return try unwrapReview(resp)
    }

    @discardableResult
    func updateNotificationPrefs(_ req: UpdatePrefsRequest) async throws -> NotificationPrefs {
        let resp: APIResponse<NotificationPrefs> =
            try await patch(Endpoints.Notifications.prefs, body: req)
        return try unwrapReview(resp)
    }

    // MARK: - helpers

    fileprivate func unwrapReview<T>(_ resp: APIResponse<T>) throws -> T {
        if resp.code != 0 { throw APIError.serverError(resp.code, resp.message) }
        guard let data = resp.data else { throw APIError.noData }
        return data
    }
}

private struct EmptyBody: Encodable {}

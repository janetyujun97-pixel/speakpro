import Foundation

// MARK: - 客户端 → 服务端 消息

/// 客户端发送的 WebSocket 消息
enum WSClientMessageType: String, Codable {
    case sessionInit   = "session_init"
    case audioChunk    = "audio_chunk"
    case audioComplete = "audio_complete"
    case text          = "text"
    case pong          = "pong"
}

struct WSClientMessage<T: Encodable>: Encodable {
    let type: WSClientMessageType
    let data: T?
}

struct SessionInitData: Codable {
    let sessionId: String
    let examType: String   // IELTS, TOEFL
    let section: String    // Part1, Part2, Part3, Independent, Integrated
    let mode: String       // conversation, read_aloud, follow_read, mock_exam
}

struct AudioChunkData: Encodable {
    let sequence: Int
    let audioB64: String
    let isFinal: Bool
}

struct AudioCompleteData: Encodable {
    let sessionId: String
    let referenceText: String?
}

struct TextMessageData: Encodable {
    let content: String
}

// MARK: - 服务端 → 客户端 消息

/// 服务端消息类型
enum WSServerMessageType: String, Codable {
    case sessionReady  = "session_ready"
    case transcript    = "transcript"
    case examiner      = "examiner"
    case scoreUpdate   = "score_update"
    case error         = "error"
    case ping          = "ping"
    case processing    = "processing"
}

/// 服务端消息信封（用于解码 type 字段）
struct WSServerEnvelope: Decodable {
    let type: WSServerMessageType
}

/// 解析后的服务端消息
enum WSServerMessage {
    case sessionReady(SessionReadyPayload)
    case transcript(TranscriptPayload)
    case examiner(ExaminerPayload)
    case scoreUpdate(ScoreUpdatePayload)
    case error(ErrorPayload)
    case processing(ProcessingPayload)
    case ping
    case unknown(String)
}

struct SessionReadyPayload: Decodable {
    let sessionId: String
    let examinerGreeting: String
    let timeLimitSec: Int
    let greetingTtsB64: String?
}

struct TranscriptPayload: Decodable {
    let text: String
    let isFinal: Bool
}

struct ExaminerPayload: Decodable {
    let text: String
    let ttsAudioB64: String?
}

struct ScoreUpdatePayload: Decodable {
    let pronunciation: PronunciationScoreData?
    let grammar: GrammarScoreData?
    let content: ContentScoreData?
    let overall: Double
    let aiFeedback: String?
}

struct PronunciationScoreData: Decodable {
    let overall: Double?
    let fluency: Double?
    let stress: Double?
    let intonation: Double?
    let integrity: Double?
}

struct GrammarScoreData: Decodable {
    let score: Double?
    let errors: [GrammarErrorData]?
    let corrections: [String]?
}

struct GrammarErrorData: Decodable {
    let text: String?
    let type: String?
    let suggestion: String?
}

struct ContentScoreData: Decodable {
    let score: Double?
    let relevance: Double?
    let vocabulary: Double?
    let coherence: Double?
}

struct ErrorPayload: Decodable {
    let code: String
    let message: String
}

struct ProcessingPayload: Decodable {
    let step: String
    let message: String
}

// MARK: - 解析器

enum WSMessageParser {
    /// 从 JSON 字符串解析服务端消息
    static func parse(_ jsonString: String) -> WSServerMessage {
        guard let data = jsonString.data(using: .utf8) else {
            return .unknown(jsonString)
        }

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase

        // 先解析 type 字段
        guard let envelope = try? decoder.decode(WSServerEnvelope.self, from: data) else {
            return .unknown(jsonString)
        }

        switch envelope.type {
        case .sessionReady:
            return decodeData(data, as: SessionReadyPayload.self, decoder: decoder)
                .map { .sessionReady($0) } ?? .unknown(jsonString)

        case .transcript:
            return decodeData(data, as: TranscriptPayload.self, decoder: decoder)
                .map { .transcript($0) } ?? .unknown(jsonString)

        case .examiner:
            return decodeData(data, as: ExaminerPayload.self, decoder: decoder)
                .map { .examiner($0) } ?? .unknown(jsonString)

        case .scoreUpdate:
            return decodeData(data, as: ScoreUpdatePayload.self, decoder: decoder)
                .map { .scoreUpdate($0) } ?? .unknown(jsonString)

        case .error:
            return decodeData(data, as: ErrorPayload.self, decoder: decoder)
                .map { .error($0) } ?? .unknown(jsonString)

        case .processing:
            return decodeData(data, as: ProcessingPayload.self, decoder: decoder)
                .map { .processing($0) } ?? .unknown(jsonString)

        case .ping:
            return .ping
        }
    }

    /// 解析 { "type": "...", "data": { ... } } 中的 data 字段
    private static func decodeData<T: Decodable>(
        _ jsonData: Data,
        as type: T.Type,
        decoder: JSONDecoder
    ) -> T? {
        return try? decoder.decode(WSDataWrapper<T>.self, from: jsonData).data
    }
}

/// WebSocket 消息 data 字段包装器（需要在顶层定义以兼容 Swift 6 泛型规则）
private struct WSDataWrapper<T: Decodable>: Decodable {
    let data: T
}

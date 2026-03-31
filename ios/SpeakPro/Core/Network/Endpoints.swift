import Foundation

/// API 端点常量
enum Endpoints {

    static let baseURL = "http://localhost/api/v1"

    // MARK: - Auth

    enum Auth {
        static let login    = "/auth/login"
        static let register = "/auth/register"
        static let refresh  = "/auth/refresh"
    }

    // MARK: - Practice

    enum Practice {
        static let start    = "/practice/start"
        static let audio    = "/practice/audio"
        static let sessions = "/practice/sessions"
        static let stats    = "/practice/stats"
    }

    // MARK: - Conversation (WebSocket)

    enum Conversation {
        /// 返回完整的 WebSocket URL
        static func wsConnect(sessionId: String) -> String {
            let wsBase = baseURL
                .replacingOccurrences(of: "http://", with: "ws://")
                .replacingOccurrences(of: "https://", with: "wss://")
            return "\(wsBase)/conversation/ws/\(sessionId)"
        }
    }

    // MARK: - Questions

    enum Questions {
        static let list     = "/questions"
        static let create   = "/questions"
        static func importQuestions() -> String { "/questions/import" }
    }

    // MARK: - Assignments (Homework)

    enum Assignments {
        static let list   = "/assignments"

        static func detail(id: String) -> String {
            "/assignments/\(id)"
        }

        static func submit(id: String) -> String {
            "/assignments/\(id)/submit"
        }
    }

    // MARK: - Assessment

    enum Assessment {
        static let evaluate = "/assessment/evaluate"
        static let feedback = "/assessment/feedback"
    }

    // MARK: - TTS

    enum TTS {
        static let synthesize = "/tts/synthesize"
    }
}

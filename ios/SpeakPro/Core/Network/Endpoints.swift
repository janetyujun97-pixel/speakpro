import Foundation

/// API 端点常量
enum Endpoints {

    // NestJS CRUD 服务（通过 Nginx 代理，统一用 80 端口）
    #if DEBUG
    static let baseURL = "http://122.51.170.224/api/v1"
    #else
    static let baseURL = "https://api.speakpro.com/api/v1"
    #endif

    // Go AI 服务（通过 Nginx 代理，统一用 80 端口）
    #if DEBUG
    static let goBaseURL = "http://122.51.170.224/api/v1"
    #else
    static let goBaseURL = "https://api.speakpro.com/api/v1"
    #endif

    // MARK: - Auth

    enum Auth {
        static let login          = "/auth/login"
        static let register       = "/auth/register"
        static let refresh        = "/auth/refresh"

        // —— Auth v2（PR2a 新增） ——
        static let sendOtp        = "/auth/send-otp"
        static let verifyOtp      = "/auth/verify-otp"
        static let registerPhone  = "/auth/register-phone"
        static let requestReset   = "/auth/request-reset"
        static let resetPassword  = "/auth/reset-password"
        static let apple          = "/auth/apple"
        static let wechat         = "/auth/wechat"
    }

    // MARK: - Onboarding

    enum Onboarding {
        static let status    = "/onboarding/status"
        static let profile   = "/onboarding/profile"
        static let baseline  = "/onboarding/baseline"
        static let finalize  = "/onboarding/finalize"
    }

    // MARK: - Practice

    enum Practice {
        static let start    = "/practice/start"
        static let audio    = "/practice/audio"
        static let sessions = "/practice/sessions"
        static let stats    = "/practice/stats"
    }

    // MARK: - Conversation (WebSocket → Go 服务)

    enum Conversation {
        /// 返回完整的 WebSocket URL（连接 Go 服务）
        static func wsConnect(sessionId: String) -> String {
            let wsBase = goBaseURL
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

    // MARK: - Assessment（Go 服务）

    enum Assessment {
        static var evaluate: String { goBaseURL + "/assessment/evaluate" }
        static var fullEvaluate: String { goBaseURL + "/assessment/full-evaluate" }
        static var feedback: String { goBaseURL + "/assessment/feedback" }
    }

    // MARK: - TTS（Go 服务）

    enum TTS {
        static var synthesize: String { goBaseURL + "/tts/synthesize" }
    }
}

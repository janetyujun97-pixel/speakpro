import Foundation

/// 客户端统一错误码 —— 对应 §4.3 容错映射。
/// 每个错误码承担三件事：
///   1. 展示给用户的文案（eyebrow / headline / body）
///   2. 数字 glyph（ErrorStateView 的大号字）
///   3. 底层 APIError / 网络错误 → 错误码的映射
enum SpErrorCode: String, CaseIterable {
    case scoreEngine503 = "ERR-SCORE-503"   // Go 调讯飞 ISE / 千问 5xx
    case ttsTimeout504  = "ERR-TTS-504"     // Fish Audio 超时（已被 Go 端 fallback 兜住，不常抛到 UI）
    case offline        = "ERR-NET"          // 客户端断网
    case unauthorized401 = "ERR-AUTH-401"   // Token 失效（上层应先尝试 refresh）
    case unknown        = "ERR-UNKNOWN"

    // MARK: - Display

    /// 大号数字 glyph
    var displayNumber: String {
        switch self {
        case .scoreEngine503: return "503"
        case .ttsTimeout504:  return "504"
        case .offline:        return "—"
        case .unauthorized401: return "401"
        case .unknown:        return "?"
        }
    }

    var eyebrow: String {
        switch self {
        case .scoreEngine503: return "SCORE ENGINE UNREACHABLE"
        case .ttsTimeout504:  return "VOICE SYNTH SLOW"
        case .offline:        return "NO CONNECTION · 无网络"
        case .unauthorized401: return "SESSION EXPIRED · 会话过期"
        case .unknown:        return "SOMETHING WRONG · 出错了"
        }
    }

    var headline: String {
        switch self {
        case .scoreEngine503: return "Our scoring engine"
        case .ttsTimeout504:  return "The voice synthesis"
        case .offline:        return "We're offline,"
        case .unauthorized401: return "Please log in again,"
        case .unknown:        return "Something odd,"
        }
    }

    var headlineItalic: String {
        switch self {
        case .scoreEngine503: return "is catching its breath."
        case .ttsTimeout504:  return "is catching up."
        case .offline:        return "some rehearsal."
        case .unauthorized401: return "for continuity."
        case .unknown:        return "please retry."
        }
    }

    var body: String {
        switch self {
        case .scoreEngine503:
            return "你的录音已经安全保存在本地 —— 等服务恢复会自动上传评分。不会丢。"
        case .ttsTimeout504:
            return "示范发音生成较慢，已自动切换到备用语音。你可以继续练习。"
        case .offline:
            return "检查下 WiFi / 蜂窝数据。已缓存的材料仍可继续练。"
        case .unauthorized401:
            return "为保护账号安全，请重新登录一次。"
        case .unknown:
            return "发生了预期之外的问题。请重试，或反馈给我们。"
        }
    }

    // MARK: - Mapping from Error

    /// 把底层 APIError / NSError 映射为 SpErrorCode
    static func from(_ error: Error) -> SpErrorCode {
        if let api = error as? APIError {
            switch api {
            case .unauthorized:             return .unauthorized401
            case .networkError:             return .offline
            case .serverError(let code, _): return fromHTTPStatus(code)
            default:                        return .unknown
            }
        }
        // 原生 URLError —— 断网 / 超时
        let ns = error as NSError
        if ns.domain == NSURLErrorDomain {
            switch ns.code {
            case NSURLErrorNotConnectedToInternet,
                 NSURLErrorNetworkConnectionLost,
                 NSURLErrorTimedOut:
                return .offline
            default:
                return .unknown
            }
        }
        return .unknown
    }

    private static func fromHTTPStatus(_ code: Int) -> SpErrorCode {
        switch code {
        case 401: return .unauthorized401
        case 503: return .scoreEngine503
        case 504: return .ttsTimeout504
        default:  return .unknown
        }
    }
}

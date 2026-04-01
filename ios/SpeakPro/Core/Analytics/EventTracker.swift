import Foundation

/// 事件追踪单例 — 批量上报埋点到后端
final class EventTracker {

    static let shared = EventTracker()

    private var buffer: [TrackEvent] = []
    private let batchSize = 10
    private let flushInterval: TimeInterval = 30
    private var flushTimer: Timer?

    private init() {
        startFlushTimer()
    }

    // MARK: - 公开方法

    /// 追踪事件
    func track(_ eventName: String, properties: [String: Any]? = nil) {
        let event = TrackEvent(
            eventName: eventName,
            payload: properties ?? [:],
            timestamp: Date()
        )
        buffer.append(event)

        // 达到批量大小时立即发送
        if buffer.count >= batchSize {
            flush()
        }
    }

    /// 立即发送所有缓存事件
    func flush() {
        guard !buffer.isEmpty else { return }
        let events = buffer
        buffer.removeAll()

        Task {
            for event in events {
                do {
                    let _: APIResponse<EmptyResponse> = try await APIClient.shared.post(
                        "/analytics/events",
                        body: EventBody(
                            eventName: event.eventName,
                            payload: event.payload
                        )
                    )
                } catch {
                    // 上报失败不影响用户体验，静默忽略
                    print("[EventTracker] 上报失败: \(event.eventName) - \(error.localizedDescription)")
                }
            }
        }
    }

    // MARK: - 便捷方法

    /// 页面浏览
    func trackPageView(_ pageName: String) {
        track("page_view", properties: ["page": pageName])
    }

    /// 练习开始
    func trackPracticeStart(mode: String, questionId: String? = nil) {
        var props: [String: Any] = ["mode": mode]
        if let qid = questionId { props["question_id"] = qid }
        track("practice_start", properties: props)
    }

    /// 练习完成
    func trackPracticeComplete(mode: String, score: Double, sessionId: String) {
        track("practice_complete", properties: [
            "mode": mode,
            "score": score,
            "session_id": sessionId,
        ])
    }

    /// 作业提交
    func trackHomeworkSubmit(assignmentId: String) {
        track("homework_submit", properties: ["assignment_id": assignmentId])
    }

    // MARK: - 私有

    private func startFlushTimer() {
        flushTimer = Timer.scheduledTimer(withTimeInterval: flushInterval, repeats: true) { [weak self] _ in
            self?.flush()
        }
    }

    deinit {
        flushTimer?.invalidate()
        flush()
    }
}

// MARK: - 内部模型

private struct TrackEvent {
    let eventName: String
    let payload: [String: Any]
    let timestamp: Date
}

private struct EventBody: Encodable {
    let eventName: String
    let payload: [String: Any]

    // [String: Any] 需要手动编码
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(eventName, forKey: .eventName)
        // payload 转为 JSON Data 再转 String（简化处理）
        if let data = try? JSONSerialization.data(withJSONObject: payload),
           let jsonStr = String(data: data, encoding: .utf8) {
            try container.encode(jsonStr, forKey: .payload)
        }
    }

    enum CodingKeys: String, CodingKey {
        case eventName, payload
    }
}

private struct EmptyResponse: Decodable {}

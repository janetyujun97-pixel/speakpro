import Foundation
import Combine

/// WebSocket 连接状态
enum WebSocketConnectionState: String {
    case disconnected
    case connecting
    case connected
}

/// WebSocket 消息类型
enum WSMessage {
    case text(String)
    case data(Data)
}

/// 管理 WebSocket 实时通信（如 AI 对话流式交互）
final class WebSocketManager: ObservableObject {

    @Published var connectionState: WebSocketConnectionState = .disconnected

    private var webSocketTask: URLSessionWebSocketTask?
    private let session: URLSession
    private var reconnectAttempts = 0
    private let maxReconnectAttempts = 5
    private var currentSessionId: String?

    /// 收到消息时的回调
    var onMessage: ((WSMessage) -> Void)?

    /// 连接断开时的回调
    var onDisconnect: ((Error?) -> Void)?

    init() {
        session = URLSession(configuration: .default)
    }

    // MARK: - Connect

    func connect(sessionId: String) {
        currentSessionId = sessionId
        connectionState = .connecting

        let wsURL = Endpoints.Conversation.wsConnect(sessionId: sessionId)
        guard let url = URL(string: wsURL) else {
            print("[WebSocket] 无效的 URL: \(wsURL)")
            connectionState = .disconnected
            return
        }

        var request = URLRequest(url: url)
        // 附加 JWT token
        if let token = APIClient.shared.accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        webSocketTask = session.webSocketTask(with: request)
        webSocketTask?.resume()
        connectionState = .connected
        reconnectAttempts = 0

        receiveMessage()
    }

    // MARK: - Disconnect

    func disconnect() {
        webSocketTask?.cancel(with: .goingAway, reason: nil)
        webSocketTask = nil
        connectionState = .disconnected
        currentSessionId = nil
        reconnectAttempts = 0
    }

    // MARK: - Send

    func send(message: String) {
        guard connectionState == .connected else { return }
        let wsMessage = URLSessionWebSocketTask.Message.string(message)
        webSocketTask?.send(wsMessage) { error in
            if let error = error {
                print("[WebSocket] 发送失败: \(error.localizedDescription)")
            }
        }
    }

    func send(data: Data) {
        guard connectionState == .connected else { return }
        let wsMessage = URLSessionWebSocketTask.Message.data(data)
        webSocketTask?.send(wsMessage) { error in
            if let error = error {
                print("[WebSocket] 发送数据失败: \(error.localizedDescription)")
            }
        }
    }

    // MARK: - Receive (recursive)

    private func receiveMessage() {
        webSocketTask?.receive { [weak self] result in
            guard let self = self else { return }
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    DispatchQueue.main.async {
                        self.onMessage?(.text(text))
                    }
                case .data(let data):
                    DispatchQueue.main.async {
                        self.onMessage?(.data(data))
                    }
                @unknown default:
                    break
                }
                // 继续监听下一条消息
                self.receiveMessage()

            case .failure(let error):
                print("[WebSocket] 接收失败: \(error.localizedDescription)")
                DispatchQueue.main.async {
                    self.connectionState = .disconnected
                    self.onDisconnect?(error)
                    self.attemptReconnect()
                }
            }
        }
    }

    // MARK: - Auto Reconnect

    private func attemptReconnect() {
        guard reconnectAttempts < maxReconnectAttempts,
              let sessionId = currentSessionId else { return }

        reconnectAttempts += 1
        let delay = Double(reconnectAttempts) * 2.0 // 指数退避（简化版）

        print("[WebSocket] 将在 \(delay) 秒后尝试第 \(reconnectAttempts) 次重连...")

        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            self?.connect(sessionId: sessionId)
        }
    }
}

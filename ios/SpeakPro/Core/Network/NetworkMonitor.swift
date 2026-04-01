import Foundation
import Network
import Combine

/// 网络状态监测器 — 基于 NWPathMonitor
/// 实时检测网络连接状态和类型
final class NetworkMonitor: ObservableObject {

    static let shared = NetworkMonitor()

    @Published var isConnected: Bool = true
    @Published var connectionType: ConnectionType = .unknown

    enum ConnectionType: String {
        case wifi = "WiFi"
        case cellular = "蜂窝网络"
        case wired = "有线"
        case unknown = "未知"
        case none = "无网络"
    }

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.speakpro.network-monitor")

    private init() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                self?.isConnected = path.status == .satisfied
                self?.connectionType = self?.getConnectionType(path) ?? .unknown
            }
        }
        monitor.start(queue: queue)
    }

    deinit {
        monitor.cancel()
    }

    private func getConnectionType(_ path: NWPath) -> ConnectionType {
        if path.status != .satisfied { return .none }
        if path.usesInterfaceType(.wifi) { return .wifi }
        if path.usesInterfaceType(.cellular) { return .cellular }
        if path.usesInterfaceType(.wiredEthernet) { return .wired }
        return .unknown
    }
}

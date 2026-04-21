import Foundation
import SwiftUI

/// 首页风格偏好 —— dev 彩蛋：连点 masthead 的 No.XXX 3 下可切换
///
/// 对应 Android `features/home/HomeStylePreference.kt`，两端行为一致：
///   Editorial 风 × 3 种模考卡（Full / Ticket / Diagram）+ Minimal / Dashboard 占位
enum HomeStyle: String, CaseIterable, Identifiable {
    case editorialFull = "EDITORIAL_FULL"
    case editorialTicket = "EDITORIAL_TICKET"
    case editorialDiagram = "EDITORIAL_DIAGRAM"
    case minimal = "MINIMAL"
    case dashboard = "DASHBOARD"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .editorialFull:    return "编辑风 · 完整卡"
        case .editorialTicket:  return "编辑风 · 票根卡"
        case .editorialDiagram: return "编辑风 · 示意图"
        case .minimal:          return "极简风"
        case .dashboard:        return "仪表盘风"
        }
    }

    var subtitle: String {
        switch self {
        case .editorialFull:    return "深底 hero + 3 项统计"
        case .editorialTicket:  return "左侧 sienna 票根 + 虚线分割"
        case .editorialDiagram: return "P1/P2/P3 schematic"
        case .minimal:          return "大量留白 · 单列信息"
        case .dashboard:        return "圆环评分 + 多图表"
        }
    }

    var available: Bool {
        switch self {
        case .minimal, .dashboard: return false
        default: return true
        }
    }

    static var `default`: HomeStyle { .editorialFull }
}

final class HomeStylePreference: ObservableObject {

    static let shared = HomeStylePreference()

    @Published var current: HomeStyle

    private let key = "speakpro.home_style"

    private init() {
        let raw = UserDefaults.standard.string(forKey: key)
        current = HomeStyle(rawValue: raw ?? "") ?? .default
    }

    func set(_ style: HomeStyle) {
        current = style
        UserDefaults.standard.set(style.rawValue, forKey: key)
    }
}

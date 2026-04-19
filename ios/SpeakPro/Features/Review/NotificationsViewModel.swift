import Foundation

@MainActor
final class NotificationsViewModel: ObservableObject {

    @Published var items: [NotificationItem] = []
    @Published var unread: Int = 0
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil

    func load() async {
        isLoading = true; defer { isLoading = false }
        do {
            let resp = try await APIClient.shared.getNotifications()
            items = resp.items
            unread = resp.unread
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func markAllRead() async {
        do {
            _ = try await APIClient.shared.markAllNotificationsRead()
            // 本地直接把 isRead 置为 true（服务端已改，避免再拉一次）
            items = items.map { Self.markedRead($0) }
            unread = 0
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func markRead(id: String) async {
        // 已读则跳过
        guard let i = items.firstIndex(where: { $0.id == id }), !items[i].isRead else { return }
        do {
            _ = try await APIClient.shared.markNotificationRead(id: id)
            items[i] = Self.markedRead(items[i])
            unread = max(0, unread - 1)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private static func markedRead(_ item: NotificationItem) -> NotificationItem {
        NotificationItem(
            id: item.id,
            userId: item.userId,
            kind: item.kind,
            title: item.title,
            body: item.body,
            payload: item.payload,
            isRead: true,
            createdAt: item.createdAt,
        )
    }
}

@MainActor
final class NotificationPrefsViewModel: ObservableObject {

    @Published var quietStart: String = "22:30:00"
    @Published var quietEnd: String = "07:30:00"
    @Published var pushEnabled: Bool = true
    @Published var isSaving: Bool = false
    @Published var errorMessage: String? = nil

    func load() async {
        do {
            let p = try await APIClient.shared.getNotificationPrefs()
            quietStart = p.quietStart
            quietEnd = p.quietEnd
            pushEnabled = p.pushEnabled
        } catch {
            // 首次无记录 —— 用默认值即可
        }
    }

    func save() async {
        isSaving = true; defer { isSaving = false }
        errorMessage = nil
        do {
            _ = try await APIClient.shared.updateNotificationPrefs(
                UpdatePrefsRequest(
                    quietStart: quietStart,
                    quietEnd: quietEnd,
                    pushEnabled: pushEnabled,
                ),
            )
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

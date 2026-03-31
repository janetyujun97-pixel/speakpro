import Foundation

/// 个人中心视图模型
final class ProfileViewModel: ObservableObject {

    // MARK: - Published

    @Published var userName: String = ""
    @Published var userEmail: String = ""
    @Published var className: String = ""
    @Published var teacherName: String = ""
    @Published var avatarURL: URL?

    // MARK: - Fetch

    func fetchProfile() async {
        // TODO: 调用 API 获取用户资料
        await MainActor.run {
            userName = "张同学"
            userEmail = "zhang@example.com"
            className = "雅思口语提分班 A"
            teacherName = "王老师"
        }
    }

    // MARK: - Logout

    func logout() {
        // 清除 Token
        KeychainManager.delete(key: .accessToken)
        KeychainManager.delete(key: .refreshToken)

        // 重置本地数据
        userName = ""
        userEmail = ""
    }
}

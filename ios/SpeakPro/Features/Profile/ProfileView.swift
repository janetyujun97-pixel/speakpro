import SwiftUI

/// 个人中心视图
struct ProfileView: View {

    @StateObject private var viewModel = ProfileViewModel()
    @EnvironmentObject var coordinator: AppCoordinator

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {

                    // MARK: - 头像 + 基本信息
                    profileHeader

                    // MARK: - 班级信息
                    classInfoSection

                    // MARK: - 设置列表
                    settingsSection

                    // MARK: - 退出登录
                    logoutButton
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
            .background(Color.spBackground)
            .navigationTitle("我的")
            .task {
                await viewModel.fetchProfile()
            }
        }
    }

    // MARK: - Profile Header

    private var profileHeader: some View {
        VStack(spacing: 12) {
            // 头像
            ZStack {
                Circle()
                    .fill(Color.spPrimary.opacity(0.1))
                    .frame(width: 80, height: 80)

                Image(systemName: "person.crop.circle.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.spPrimary.opacity(0.5))
            }

            // 姓名
            Text(viewModel.userName)
                .font(.spTitleMedium)
                .foregroundColor(.spTextPrimary)

            // 学号 / 标签
            Text(viewModel.userEmail)
                .font(.spBodySmall)
                .foregroundColor(.spTextSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
        .background(Color.white)
        .cornerRadius(16)
    }

    // MARK: - Class Info

    private var classInfoSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("班级信息")
                .font(.spTitleSmall)
                .foregroundColor(.spTextPrimary)

            HStack {
                Label(viewModel.className, systemImage: "person.3.fill")
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextPrimary)
                Spacer()
                Text(viewModel.teacherName)
                    .font(.spBodySmall)
                    .foregroundColor(.spTextSecondary)
            }
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
    }

    // MARK: - Settings

    private var settingsSection: some View {
        VStack(spacing: 0) {
            settingsRow(icon: "bell.fill", title: "通知设置")
            Divider().padding(.leading, 50)
            settingsRow(icon: "globe", title: "语言")
            Divider().padding(.leading, 50)
            settingsRow(icon: "shield.fill", title: "隐私政策")
            Divider().padding(.leading, 50)
            settingsRow(icon: "info.circle.fill", title: "关于 SpeakPro")
        }
        .background(Color.white)
        .cornerRadius(12)
    }

    private func settingsRow(icon: String, title: String) -> some View {
        Button {
            // TODO: 导航到对应设置页
        } label: {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(.spPrimary)
                    .frame(width: 24)

                Text(title)
                    .font(.spBodyMedium)
                    .foregroundColor(.spTextPrimary)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(.spTextSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Logout

    private var logoutButton: some View {
        Button {
            viewModel.logout()
            coordinator.logout()
        } label: {
            Text("退出登录")
                .font(.spBodyMedium)
                .foregroundColor(.spError)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(Color.white)
                .cornerRadius(12)
        }
    }
}

#Preview {
    ProfileView()
        .environmentObject(AppCoordinator())
}

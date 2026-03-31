import SwiftUI

/// 练习入口卡片组件
struct PracticeCard: View {

    let title: String
    let subtitle: String
    let iconName: String
    var action: (() -> Void)?

    var body: some View {
        Button(action: { action?() }) {
            HStack(spacing: 16) {
                // 图标
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.spAccent.opacity(0.1))
                        .frame(width: 48, height: 48)

                    Image(systemName: iconName)
                        .font(.system(size: 22))
                        .foregroundColor(.spAccent)
                }

                // 文本
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.spTitleSmall)
                        .foregroundColor(.spTextPrimary)

                    Text(subtitle)
                        .font(.spBodySmall)
                        .foregroundColor(.spTextSecondary)
                        .lineLimit(2)
                }

                Spacer()

                // 箭头
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.spTextSecondary)
            }
            .padding(16)
            .background(Color.white)
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 8, y: 2)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack(spacing: 12) {
        PracticeCard(
            title: "AI 对话练习",
            subtitle: "与 AI 考官进行模拟对话",
            iconName: "bubble.left.and.bubble.right.fill"
        )
        PracticeCard(
            title: "跟读练习",
            subtitle: "跟随标准发音练习",
            iconName: "waveform.and.mic"
        )
    }
    .padding()
    .background(Color.spBackground)
}

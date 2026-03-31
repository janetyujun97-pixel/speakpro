import SwiftUI

/// 聊天气泡组件
struct ChatBubbleView: View {

    let isExaminer: Bool
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            if isExaminer {
                avatar
            } else {
                Spacer(minLength: 60)
            }

            // 气泡内容
            Text(text)
                .font(.spBodyMedium)
                .foregroundColor(isExaminer ? .spTextPrimary : .white)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(bubbleBackground)

            if isExaminer {
                Spacer(minLength: 60)
            } else {
                avatar
            }
        }
    }

    // MARK: - Avatar

    private var avatar: some View {
        ZStack {
            Circle()
                .fill(isExaminer ? Color.spPrimary.opacity(0.1) : Color.spAccent.opacity(0.1))
                .frame(width: 36, height: 36)

            Image(systemName: isExaminer ? "person.crop.circle.fill" : "person.fill")
                .font(.system(size: 18))
                .foregroundColor(isExaminer ? .spPrimary : .spAccent)
        }
    }

    // MARK: - Bubble Background

    private var bubbleBackground: some View {
        Group {
            if isExaminer {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.white)
                    .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
            } else {
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color.spAccent)
            }
        }
    }
}

#Preview {
    VStack(spacing: 12) {
        ChatBubbleView(
            isExaminer: true,
            text: "Can you tell me about your hometown?"
        )
        ChatBubbleView(
            isExaminer: false,
            text: "Sure! My hometown is a beautiful city in southern China..."
        )
    }
    .padding()
    .background(Color.spBackground)
}

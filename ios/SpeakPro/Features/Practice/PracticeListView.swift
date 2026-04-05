import SwiftUI

/// 练习模式选择页面 —— Practice tab 的根视图
struct PracticeListView: View {

    @State private var selectedMode: PracticeMode?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // 页面标题区域
                    VStack(alignment: .leading, spacing: 8) {
                        Text("选择练习模式")
                            .font(.spTitleLarge)
                            .foregroundColor(.spTextPrimary)
                        Text("每天坚持练习，口语能力稳步提升")
                            .font(.spBodyMedium)
                            .foregroundColor(.spTextSecondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.bottom, 8)

                    // 四种练习模式卡片
                    ForEach(PracticeMode.allCases) { mode in
                        NavigationLink(value: mode) {
                            PracticeCard(
                                title: mode.title,
                                subtitle: mode.subtitle,
                                iconName: mode.iconName
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 32)
            }
            .background(Color.spBackground)
            .navigationTitle("口语练习")
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(for: PracticeMode.self) { mode in
                switch mode {
                case .conversation:
                    ConversationView()
                case .readAloud:
                    ReadAloudView()
                case .followRead:
                    FollowReadView()
                case .mockExam:
                    MockExamView()
                }
            }
        }
    }
}

// MARK: - 练习模式枚举

enum PracticeMode: String, CaseIterable, Identifiable, Hashable {
    case conversation
    case readAloud
    case followRead
    case mockExam

    var id: String { rawValue }

    var title: String {
        switch self {
        case .conversation: return "AI 对话练习"
        case .readAloud:    return "朗读练习"
        case .followRead:   return "跟读练习"
        case .mockExam:     return "模考练习"
        }
    }

    var subtitle: String {
        switch self {
        case .conversation: return "与 AI 考官进行模拟对话，实时评分反馈"
        case .readAloud:    return "朗读给定文章，评测发音准确度和流利度"
        case .followRead:   return "跟随标准发音逐句练习，对比纠正"
        case .mockExam:     return "完整模考流程，还原真实考试体验"
        }
    }

    var iconName: String {
        switch self {
        case .conversation: return "bubble.left.and.bubble.right.fill"
        case .readAloud:    return "text.bubble.fill"
        case .followRead:   return "waveform.and.mic"
        case .mockExam:     return "timer"
        }
    }
}

#Preview {
    PracticeListView()
}

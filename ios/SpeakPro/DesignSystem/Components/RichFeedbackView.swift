import SwiftUI

/// AI 反馈富文本渲染组件
/// 解析简单 Markdown（**加粗**、- 列表）并用原生 SwiftUI 样式展示
struct RichFeedbackView: View {

    let text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ForEach(Array(parseBlocks().enumerated()), id: \.offset) { _, block in
                switch block {
                case .heading(let title):
                    Text(title)
                        .font(.spTitleSmall)
                        .foregroundColor(.spTextPrimary)
                        .padding(.top, 4)

                case .bullet(let content):
                    HStack(alignment: .top, spacing: 8) {
                        Circle()
                            .fill(Color.spAccent)
                            .frame(width: 5, height: 5)
                            .padding(.top, 6)
                        Text(content)
                            .font(.spBodySmall)
                            .foregroundColor(.spTextSecondary)
                            .lineSpacing(3)
                    }

                case .paragraph(let content):
                    Text(content)
                        .font(.spBodySmall)
                        .foregroundColor(.spTextSecondary)
                        .lineSpacing(3)

                case .quote(let content):
                    HStack(spacing: 0) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(Color.spAccent.opacity(0.5))
                            .frame(width: 3)
                        Text(content)
                            .font(.spBodySmall)
                            .italic()
                            .foregroundColor(.spTextSecondary)
                            .padding(.leading, 10)
                            .lineSpacing(3)
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }

    // MARK: - 解析

    private enum Block {
        case heading(String)
        case bullet(String)
        case paragraph(String)
        case quote(String)
    }

    private func parseBlocks() -> [Block] {
        var blocks: [Block] = []
        let lines = text.components(separatedBy: "\n")

        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            if trimmed.isEmpty { continue }

            // **标题:** 或 **标题 (English):**
            if trimmed.hasPrefix("**") && (trimmed.contains(":**") || trimmed.hasSuffix("**")) {
                let title = trimmed
                    .replacingOccurrences(of: "**", with: "")
                    .replacingOccurrences(of: ":", with: "")
                    .trimmingCharacters(in: .whitespaces)
                if !title.isEmpty {
                    blocks.append(.heading(title))
                }
                continue
            }

            // - 列表项
            if trimmed.hasPrefix("- ") {
                let content = String(trimmed.dropFirst(2))
                    .replacingOccurrences(of: "**", with: "")
                    .trimmingCharacters(in: .whitespaces)
                blocks.append(.bullet(content))
                continue
            }

            // "引用文本"
            if trimmed.hasPrefix("\"") || trimmed.hasPrefix("\u{201C}") {
                let content = trimmed
                    .replacingOccurrences(of: "**", with: "")
                    .replacingOccurrences(of: "\"", with: "")
                    .replacingOccurrences(of: "\u{201C}", with: "")
                    .replacingOccurrences(of: "\u{201D}", with: "")
                    .trimmingCharacters(in: .whitespaces)
                blocks.append(.quote(content))
                continue
            }

            // 普通段落（移除所有 ** 标记）
            let cleaned = trimmed.replacingOccurrences(of: "**", with: "")
            blocks.append(.paragraph(cleaned))
        }

        return blocks
    }
}

#Preview {
    ScrollView {
        RichFeedbackView(text: "**优点:**\n- 表达清晰\n- 语法正确\n\n**改进建议:**\n- 增加细节\n- 使用更多连接词")
            .padding()
    }
}

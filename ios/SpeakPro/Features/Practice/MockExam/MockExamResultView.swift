import SwiftUI
import AVFoundation

/// 模考单题评测结果页 — 评分 + 录音回放 + 转写 + 4Tab 分析
struct MockExamResultView: View {

    let question: String
    let part: Int
    let result: FullEvaluateResult
    let audioURL: URL?
    let questionIndex: Int
    let totalQuestions: Int
    var onNext: (() -> Void)?
    var onRedo: (() -> Void)?

    @State private var selectedTab = 0
    @State private var studentPlayer: AVAudioPlayer?
    @State private var revisedPlayer: AVAudioPlayer?
    @State private var isPlayingStudent = false
    @State private var isPlayingRevised = false

    private let tabs = ["已修订", "思维导图", "关键词", "样例答案"]

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 16) {
                    // 1. 评分头部
                    scoreHeader
                    // 2. 题目卡片
                    questionCard
                    // 3. 我的答案
                    myAnswerSection
                    // 4. Tab 分析
                    tabbedAnalysis
                }
                .padding(.bottom, 16)
            }

            // 底部按钮
            bottomActions
        }
        .background(Color.spBackground)
    }

    // MARK: - Score Header

    private var scoreHeader: some View {
        VStack(spacing: 16) {
            Text("AI 评估")
                .font(.spTitleMedium)
                .foregroundColor(.spTextPrimary)

            // 总分大环
            ScoreRing(
                score: result.overallScore ?? 0,
                color: scoreColor(result.overallScore ?? 0),
                lineWidth: 10,
                size: 100
            )

            // 三维度评分
            HStack(spacing: 0) {
                dimensionScore("发音", result.pronunciationScore?.overall ?? 0, .spSuccess)
                Divider().frame(height: 40)
                dimensionScore("流利度", result.pronunciationScore?.fluency ?? 0, .spPrimary)
                Divider().frame(height: 40)
                dimensionScore("语法", result.grammarScore?.score ?? 0, .spAccent)
            }
            .padding(.vertical, 12)
            .background(Color.white)
            .cornerRadius(12)
            .padding(.horizontal, 16)
        }
        .padding(.vertical, 20)
        .background(
            LinearGradient(colors: [Color.spAccent.opacity(0.08), Color.spBackground], startPoint: .top, endPoint: .bottom)
        )
    }

    private func dimensionScore(_ title: String, _ score: Double, _ color: Color) -> some View {
        VStack(spacing: 4) {
            Text("\(Int(score))")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(color)
            Text(title)
                .font(.system(size: 11))
                .foregroundColor(.spTextSecondary)
        }
        .frame(maxWidth: .infinity)
    }

    private func scoreColor(_ score: Double) -> Color {
        if score >= 80 { return .spSuccess }
        if score >= 60 { return .spWarning }
        return .spError
    }

    // scoreLabel removed — replaced by dimensionScore

    // MARK: - Question Card

    private var questionCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Part \(part)")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.spAccent)
                    .cornerRadius(4)

                Spacer()

                if let onRedo = onRedo {
                    Button(action: onRedo) {
                        HStack(spacing: 4) {
                            Image(systemName: "pencil")
                                .font(.system(size: 12))
                            Text("重做")
                                .font(.spCaption)
                        }
                        .foregroundColor(.spAccent)
                    }
                }
            }

            Text(question)
                .font(.spBodyMedium)
                .foregroundColor(.spTextPrimary)
                .lineSpacing(4)
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
        .padding(.horizontal, 16)
    }

    // MARK: - My Answer Section

    private var myAnswerSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("我的答案")
                    .font(.spTitleSmall)
                    .foregroundColor(.spTextPrimary)

                if let wc = result.wordCount, wc > 0 {
                    Text("\(wc) 单词")
                        .font(.system(size: 10))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color(.systemGray5))
                        .cornerRadius(4)
                }
                if let sc = result.sentenceCount, sc > 0 {
                    Text("\(sc) 句子")
                        .font(.system(size: 10))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color(.systemGray5))
                        .cornerRadius(4)
                }
            }

            // 录音播放
            if audioURL != nil {
                Button {
                    toggleStudentPlayback()
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: isPlayingStudent ? "stop.fill" : "play.fill")
                        Text(isPlayingStudent ? "停止" : "播放")
                            .font(.spBodySmall)
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(Color.spAccent)
                    .cornerRadius(8)
                }
            }

            // 转写文本 + 错误高亮
            if let transcript = result.transcript, !transcript.isEmpty {
                highlightedTranscript(transcript)
            }
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
        .padding(.horizontal, 16)
    }

    private func highlightedTranscript(_ text: String) -> some View {
        let errors = result.grammarScore?.errors ?? []
        let errorTexts = Set(errors.compactMap { $0.text?.lowercased() })

        // 简单实现：按单词检测是否在错误短语中
        let words = text.components(separatedBy: " ")

        return FlowLayout(spacing: 3) {
            ForEach(Array(words.enumerated()), id: \.offset) { _, word in
                let isError = errorTexts.contains(where: { $0.contains(word.lowercased()) })
                Text(word)
                    .font(.spBodySmall)
                    .foregroundColor(isError ? .spError : .spTextPrimary)
                    .underline(isError, color: .spError)
            }
        }
    }

    // MARK: - Tabbed Analysis

    private var tabbedAnalysis: some View {
        VStack(spacing: 0) {
            // Tab 选择栏
            HStack(spacing: 0) {
                ForEach(0..<tabs.count, id: \.self) { i in
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) { selectedTab = i }
                    } label: {
                        VStack(spacing: 6) {
                            Text(tabs[i])
                                .font(.spBodySmall)
                                .fontWeight(selectedTab == i ? .semibold : .regular)
                                .foregroundColor(selectedTab == i ? .spAccent : .spTextSecondary)
                            Rectangle()
                                .fill(selectedTab == i ? Color.spAccent : Color.clear)
                                .frame(height: 2)
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 16)
            .background(Color.white)

            Divider()

            // Tab 内容
            Group {
                switch selectedTab {
                case 0: revisedTab
                case 1: mindMapTab
                case 2: keywordsTab
                case 3: sampleAnswersTab
                default: EmptyView()
                }
            }
            .padding(16)
            .background(Color.white)
            .cornerRadius(12)
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
    }

    // Tab 0: 已修订
    private var revisedTab: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let revised = result.revisedAnswer {
                HStack {
                    Text("修改后答案")
                        .font(.spTitleSmall)
                        .foregroundColor(.spAccent)

                    if let wc = revised.wordCount {
                        Text("\(wc) 单词")
                            .font(.system(size: 10))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color(.systemGray5))
                            .cornerRadius(4)
                    }
                }

                // 修订答案 TTS 播放
                if result.revisedAudioB64 != nil {
                    Button {
                        toggleRevisedPlayback()
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: isPlayingRevised ? "stop.fill" : "play.fill")
                            Text(isPlayingRevised ? "停止" : "播放修订答案")
                                .font(.spCaption)
                        }
                        .foregroundColor(.spPrimary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.spPrimary.opacity(0.1))
                        .cornerRadius(6)
                    }
                }

                Text(revised.text ?? "")
                    .font(.spBodySmall)
                    .foregroundColor(.spTextPrimary)
                    .lineSpacing(4)
            } else {
                Text("修订答案生成中...")
                    .font(.spBodySmall)
                    .foregroundColor(.spTextSecondary)
            }
        }
    }

    // Tab 1: 思维导图
    private var mindMapTab: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("思维导图")
                .font(.spTitleSmall)
                .foregroundColor(.spAccent)

            if let mindMap = result.mindMap {
                if let title = mindMap.title {
                    Text(title)
                        .font(.spBodyMedium)
                        .fontWeight(.bold)
                        .foregroundColor(.spTextPrimary)
                }
                if let children = mindMap.children {
                    ForEach(children) { node in
                        mindMapNodeView(node: node, level: 0)
                    }
                }
            } else {
                Text("思维导图生成中...")
                    .font(.spBodySmall)
                    .foregroundColor(.spTextSecondary)
            }
        }
    }

    private func mindMapNodeView(node: MindMapNode, level: Int) -> AnyView {
        AnyView(
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .top, spacing: 8) {
                    if level > 0 {
                        RoundedRectangle(cornerRadius: 1)
                            .fill(Color.spAccent.opacity(0.3))
                            .frame(width: 2, height: 16)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(node.label ?? "")
                            .font(level == 0 ? .spBodyMedium : .spBodySmall)
                            .fontWeight(level == 0 ? .bold : .regular)
                            .foregroundColor(.spTextPrimary)

                        if let detail = node.detail, !detail.isEmpty {
                            Text(detail)
                                .font(.spCaption)
                                .foregroundColor(.spTextSecondary)
                        }
                    }
                }
                .padding(.leading, CGFloat(level) * 20)

                if let children = node.children {
                    ForEach(children) { child in
                        mindMapNodeView(node: child, level: level + 1)
                    }
                }
            }
        )
    }

    // Tab 2: 关键词
    private var keywordsTab: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("关键词")
                .font(.spTitleSmall)
                .foregroundColor(.spAccent)

            if let keywords = result.keywords, !keywords.isEmpty {
                ForEach(keywords) { kw in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 8) {
                            Text(kw.word ?? "")
                                .font(.spBodyMedium)
                                .fontWeight(.bold)
                                .foregroundColor(.spTextPrimary)
                            if let ph = kw.phonetic {
                                Text(ph)
                                    .font(.system(size: 12))
                                    .italic()
                                    .foregroundColor(.spTextSecondary)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color(.systemGray6))
                                    .cornerRadius(4)
                            }
                        }
                        if let def = kw.definition {
                            HStack(spacing: 4) {
                                Text("定义:")
                                    .font(.spCaption)
                                    .foregroundColor(.spAccent)
                                Text(def)
                                    .font(.spCaption)
                                    .foregroundColor(.spTextSecondary)
                            }
                        }
                        if let ex = kw.exampleSentence {
                            HStack(spacing: 4) {
                                Text("范例句子:")
                                    .font(.spCaption)
                                    .foregroundColor(.spAccent)
                                Text(ex)
                                    .font(.spCaption)
                                    .foregroundColor(.spTextSecondary)
                                    .italic()
                            }
                        }
                    }
                    .padding(12)
                    .background(Color(.systemGray6).opacity(0.5))
                    .cornerRadius(8)
                }
            } else {
                Text("关键词生成中...")
                    .font(.spBodySmall)
                    .foregroundColor(.spTextSecondary)
            }
        }
    }

    // Tab 3: 样例答案
    private var sampleAnswersTab: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("样例答案")
                .font(.spTitleSmall)
                .foregroundColor(.spAccent)

            if let samples = result.sampleAnswers, !samples.isEmpty {
                ForEach(Array(samples.enumerated()), id: \.offset) { i, sample in
                    VStack(alignment: .leading, spacing: 6) {
                        Text("\(i + 1).")
                            .font(.spBodySmall)
                            .fontWeight(.bold)
                            .foregroundColor(.spAccent)
                        Text(sample)
                            .font(.spBodySmall)
                            .foregroundColor(.spTextPrimary)
                            .lineSpacing(4)
                    }
                    .padding(12)
                    .background(Color.white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color(.systemGray5), lineWidth: 1)
                    )
                    .cornerRadius(8)
                }
            } else {
                Text("样例答案生成中...")
                    .font(.spBodySmall)
                    .foregroundColor(.spTextSecondary)
            }
        }
    }

    // MARK: - Bottom Actions

    private var bottomActions: some View {
        HStack(spacing: 12) {
            if let onRedo = onRedo {
                Button(action: onRedo) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.counterclockwise")
                            .font(.system(size: 13))
                        Text("重做")
                            .font(.spBodySmall)
                    }
                    .foregroundColor(.spAccent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(Color.spAccent.opacity(0.1))
                    .cornerRadius(10)
                }
            }

            if let onNext = onNext {
                Button(action: onNext) {
                    HStack(spacing: 6) {
                        Text("下一题")
                            .font(.spBodyMedium)
                            .fontWeight(.semibold)
                        Image(systemName: "forward.fill")
                            .font(.system(size: 13))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 13)
                    .background(Color.spAccent)
                    .cornerRadius(10)
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.white)
    }

    // MARK: - Audio Playback

    private func toggleStudentPlayback() {
        if isPlayingStudent {
            studentPlayer?.stop()
            isPlayingStudent = false
            return
        }
        guard let url = audioURL else { return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback)
            try AVAudioSession.sharedInstance().setActive(true)
            studentPlayer = try AVAudioPlayer(contentsOf: url)
            studentPlayer?.play()
            isPlayingStudent = true
        } catch {
            print("[ResultView] 播放学生录音失败: \(error)")
        }
    }

    private func toggleRevisedPlayback() {
        if isPlayingRevised {
            revisedPlayer?.stop()
            isPlayingRevised = false
            return
        }
        guard let b64 = result.revisedAudioB64,
              let data = Data(base64Encoded: b64) else { return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback)
            try AVAudioSession.sharedInstance().setActive(true)
            revisedPlayer = try AVAudioPlayer(data: data)
            revisedPlayer?.play()
            isPlayingRevised = true
        } catch {
            print("[ResultView] 播放修订答案失败: \(error)")
        }
    }
}

// MARK: - FlowLayout（简单的流式布局用于单词高亮）

struct FlowLayout: Layout {
    var spacing: CGFloat = 4

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = arrange(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(proposal: ProposedViewSize(width: bounds.width, height: bounds.height), subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y), proposal: .unspecified)
        }
    }

    private func arrange(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var maxHeight: CGFloat = 0
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth && x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
            maxHeight = max(maxHeight, y + rowHeight)
        }

        return (CGSize(width: maxWidth, height: maxHeight), positions)
    }
}

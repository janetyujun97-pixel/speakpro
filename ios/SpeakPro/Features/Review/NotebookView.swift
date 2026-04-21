import SwiftUI

/// 错题本 / 生词本 —— 上部 Tabs（生词 / 短语），下部列表 + 复习弹层
struct NotebookView: View {

    @StateObject private var vm = NotebookViewModel()
    @State private var selectedTab: Tab = .words

    enum Tab: String, CaseIterable { case words = "生词", phrases = "短语" }

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()
            VStack(spacing: 0) {
                tabBar
                    .padding(.horizontal, 20)
                    .padding(.top, 12)
                    .padding(.bottom, 8)

                if selectedTab == .words {
                    filterBar
                        .padding(.horizontal, 20)
                        .padding(.bottom, 12)
                }

                if vm.isLoading && vm.words.isEmpty && vm.phrases.isEmpty {
                    SwiftUI.ProgressView().frame(maxHeight: .infinity)
                } else {
                    Group {
                        if selectedTab == .words {
                            wordList
                        } else {
                            phraseList
                        }
                    }
                }
            }
        }
        .navigationTitle("错题本")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.loadAll() }
        .refreshable { await vm.loadAll() }
        .sheet(item: $vm.reviewingWord) { word in
            ReviewCard(word: word, onAnswer: { quality in
                Task { await vm.submitReview(word: word, quality: quality) }
            })
        }
    }

    // MARK: - Subviews

    private var tabBar: some View {
        HStack(spacing: 0) {
            ForEach(Tab.allCases, id: \.self) { tab in
                Button {
                    selectedTab = tab
                } label: {
                    VStack(spacing: 8) {
                        Text(tab.rawValue)
                            .font(.spBodyMedium)
                            .foregroundColor(selectedTab == tab ? .spPrimary : .spMuted)
                        Rectangle()
                            .fill(selectedTab == tab ? Color.spAccent : Color.clear)
                            .frame(height: 2)
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var filterBar: some View {
        HStack(spacing: 8) {
            ForEach([NotebookFilter.due, .all, .mastered], id: \.self) { f in
                Button {
                    Task { await vm.setFilter(f) }
                } label: {
                    Text(filterLabel(f))
                        .font(.spCaption)
                        .foregroundColor(vm.filter == f ? .spIvory : .spPrimary)
                        .padding(.horizontal, 12).padding(.vertical, 6)
                        .background(vm.filter == f ? Color.spPrimary : Color.spIvory)
                        .overlay(
                            Capsule().stroke(
                                vm.filter == f ? Color.spPrimary : Color.spLine,
                                lineWidth: 1,
                            )
                        )
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
    }

    private func filterLabel(_ f: NotebookFilter) -> String {
        switch f {
        case .due:      return "待复习 (\(vm.dueCount))"
        case .all:      return "全部"
        case .mastered: return "已掌握"
        }
    }

    private var wordList: some View {
        List {
            ForEach(vm.words) { word in
                wordRow(word)
                    .listRowBackground(Color.spBackground)
                    .listRowSeparator(.hidden)
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            Task { await vm.delete(word: word) }
                        } label: { Label("删除", systemImage: "trash") }
                    }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    @ViewBuilder
    private func wordRow(_ word: NotebookWord) -> some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                Text(word.word)
                    .font(.spSerif(18))
                    .foregroundColor(.spPrimary)
                HStack(spacing: 10) {
                    if let ipa = word.ipa {
                        Text(ipa)
                            .font(.spCaption)
                            .foregroundColor(.spMuted)
                    }
                    if word.masteredAt != nil {
                        Text("已掌握")
                            .font(.spCaption)
                            .foregroundColor(.spMoss)
                    } else if let next = word.nextReviewAt, next <= Date() {
                        Text("待复习")
                            .font(.spCaption)
                            .foregroundColor(.spAccent)
                    }
                }
            }
            Spacer()
            Text("×\(word.missCount)")
                .font(.spCaption)
                .foregroundColor(.spMuted)
            Button {
                vm.reviewingWord = word
            } label: {
                Text("复习")
                    .font(.spCaption)
                    .foregroundColor(.spIvory)
                    .padding(.horizontal, 10).padding(.vertical, 6)
                    .background(Color.spPrimary)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 8)
    }

    private var phraseList: some View {
        List(vm.phrases) { phrase in
            VStack(alignment: .leading, spacing: 4) {
                Text(phrase.phrase)
                    .font(.spBodyMedium)
                    .foregroundColor(.spPrimary)
                if let note = phrase.note {
                    Text(note).font(.spCaption).foregroundColor(.spMuted)
                }
            }
            .padding(.vertical, 4)
            .listRowBackground(Color.spBackground)
            .listRowSeparator(.hidden)
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }
}

// MARK: - Review Card Sheet

private struct ReviewCard: View {
    let word: NotebookWord
    var onAnswer: (Int) -> Void

    @State private var revealed = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color.spBackground.ignoresSafeArea()
            VStack(spacing: 24) {
                Text("REVIEW · 复习")
                    .font(.spEyebrow)
                    .foregroundColor(.spMuted)
                    .padding(.top, 28)

                VStack(spacing: 10) {
                    Text(word.word)
                        .font(.spSerif(40))
                        .foregroundColor(.spPrimary)
                    if revealed, let ipa = word.ipa {
                        Text(ipa)
                            .font(.spBodyMedium)
                            .foregroundColor(.spMuted)
                    }
                }
                .padding(.vertical, 40)

                if !revealed {
                    Button {
                        revealed = true
                    } label: {
                        Text("显示答案")
                            .font(.spBodyMedium)
                            .foregroundColor(.spPrimary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .overlay(
                                Capsule().stroke(Color.spPrimary, lineWidth: 1),
                            )
                    }
                    .buttonStyle(.plain)
                } else {
                    HStack(spacing: 12) {
                        Button {
                            onAnswer(2)
                            dismiss()
                        } label: {
                            Text("没想起来")
                                .font(.spBodyMedium)
                                .foregroundColor(.spIvory)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(Color.spAccent)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        Button {
                            onAnswer(4)
                            dismiss()
                        } label: {
                            Text("想起来了")
                                .font(.spBodyMedium)
                                .foregroundColor(.spIvory)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(Color.spMoss)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }

                Spacer()
            }
            .padding(.horizontal, 28)
        }
        .presentationDetents([.medium])
    }
}

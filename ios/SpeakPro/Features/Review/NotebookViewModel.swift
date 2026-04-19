import Foundation
import SwiftUI

@MainActor
final class NotebookViewModel: ObservableObject {

    @Published var words: [NotebookWord] = []
    @Published var phrases: [NotebookPhrase] = []
    @Published var filter: NotebookFilter = .due
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    @Published var reviewingWord: NotebookWord? = nil

    /// 未筛选时用全量计算 due count；避免每次切筛选都再拉一次
    private var allWordsCache: [NotebookWord] = []

    var dueCount: Int {
        allWordsCache.filter { $0.masteredAt == nil && ($0.nextReviewAt ?? Date()) <= Date() }.count
    }

    func loadAll() async {
        isLoading = true; defer { isLoading = false }
        do {
            async let all = APIClient.shared.getNotebookWords(filter: .all)
            async let ph = APIClient.shared.getNotebookPhrases()
            let (w, p) = try await (all, ph)
            allWordsCache = w
            phrases = p
            applyFilter()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func setFilter(_ f: NotebookFilter) async {
        filter = f
        applyFilter()
    }

    private func applyFilter() {
        switch filter {
        case .all:
            words = allWordsCache
        case .mastered:
            words = allWordsCache
                .filter { $0.masteredAt != nil }
                .sorted { ($0.masteredAt ?? .distantPast) > ($1.masteredAt ?? .distantPast) }
        case .due:
            let now = Date()
            words = allWordsCache
                .filter { $0.masteredAt == nil && ($0.nextReviewAt ?? now) <= now }
                .sorted { ($0.nextReviewAt ?? .distantPast) < ($1.nextReviewAt ?? .distantPast) }
        }
    }

    func submitReview(word: NotebookWord, quality: Int) async {
        do {
            let updated = try await APIClient.shared.reviewNotebookWord(
                id: word.id, quality: quality,
            )
            // 本地替换单词 —— nextReviewAt 变了，dueCount 跟着变
            if let idx = allWordsCache.firstIndex(where: { $0.id == word.id }) {
                allWordsCache[idx] = updated
            }
            applyFilter()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func delete(word: NotebookWord) async {
        do {
            _ = try await APIClient.shared.deleteNotebookWord(id: word.id)
            allWordsCache.removeAll { $0.id == word.id }
            applyFilter()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

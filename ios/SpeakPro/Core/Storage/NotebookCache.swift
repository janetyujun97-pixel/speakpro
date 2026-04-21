import Foundation

/// 错题本 / 生词本的本地镜像（PR5 follow-up）。
///
/// 用 JSON 持久化代替 CoreData/SQLite：
/// - 生词本数据量有限（几百 ~ 几千条最多）
/// - 场景简单：全量拉 + 整体覆盖；离线时 UI 直接读本地
/// - 避免引入 CoreData / SwiftData 带来的 schema 管理成本
///
/// 写入时机：在线 fetch 成功后由 NotebookViewModel 调 `save(words:phrases:)`。
/// 读取时机：首屏未拿到网络数据前 + 离线回退。
@MainActor
final class NotebookCache {

    static let shared = NotebookCache()

    private let fileManager = FileManager.default

    private var wordsFile: URL { cacheDir.appendingPathComponent("notebook_words.json") }
    private var phrasesFile: URL { cacheDir.appendingPathComponent("notebook_phrases.json") }

    private var cacheDir: URL {
        let base = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        let dir = base.appendingPathComponent("Notebook")
        if !fileManager.fileExists(atPath: dir.path) {
            try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    private init() {}

    // MARK: - Save

    func save(words: [NotebookWord]) {
        writeJSON(words, to: wordsFile)
    }

    func save(phrases: [NotebookPhrase]) {
        writeJSON(phrases, to: phrasesFile)
    }

    // MARK: - Load

    func loadWords() -> [NotebookWord] {
        readJSON(wordsFile) ?? []
    }

    func loadPhrases() -> [NotebookPhrase] {
        readJSON(phrasesFile) ?? []
    }

    var hasCachedWords: Bool {
        fileManager.fileExists(atPath: wordsFile.path)
    }

    // MARK: - Clear（用户登出时调）

    func clearAll() {
        try? fileManager.removeItem(at: wordsFile)
        try? fileManager.removeItem(at: phrasesFile)
    }

    // MARK: - Internal

    private func writeJSON<T: Encodable>(_ value: T, to url: URL) {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        encoder.dateEncodingStrategy = .iso8601
        do {
            let data = try encoder.encode(value)
            try data.write(to: url, options: .atomic)
        } catch {
            print("[NotebookCache] write failed: \(error)")
        }
    }

    private func readJSON<T: Decodable>(_ url: URL) -> T? {
        guard fileManager.fileExists(atPath: url.path) else { return nil }
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        decoder.dateDecodingStrategy = .iso8601
        do {
            let data = try Data(contentsOf: url)
            return try decoder.decode(T.self, from: data)
        } catch {
            print("[NotebookCache] read failed: \(error)")
            return nil
        }
    }
}

import Foundation
import AVFoundation

/// 本地录音文件管理
final class AudioFileManager {

    static let shared = AudioFileManager()

    private let fileManager = FileManager.default

    /// 录音文件临时目录
    private var recordingsDirectory: URL {
        let dir = fileManager.temporaryDirectory.appendingPathComponent("SpeakPro_Recordings")
        if !fileManager.fileExists(atPath: dir.path) {
            try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    private init() {}

    // MARK: - Create Temp File

    func createTempFileURL(extension ext: String) -> URL {
        let filename = UUID().uuidString + "." + ext
        return recordingsDirectory.appendingPathComponent(filename)
    }

    // MARK: - Delete

    func deleteFile(at url: URL) {
        try? fileManager.removeItem(at: url)
    }

    /// 清理超过指定天数的旧录音
    func deleteOldRecordings(olderThanDays days: Int = 7) {
        guard let files = try? fileManager.contentsOfDirectory(
            at: recordingsDirectory,
            includingPropertiesForKeys: [.creationDateKey]
        ) else { return }

        let cutoff = Date().addingTimeInterval(-Double(days) * 86400)

        for file in files {
            guard let attrs = try? fileManager.attributesOfItem(atPath: file.path),
                  let created = attrs[.creationDate] as? Date,
                  created < cutoff else { continue }
            try? fileManager.removeItem(at: file)
        }
    }

    // MARK: - 历史回听本地缓存（PR5 降级 follow-up）
    //
    // 策略：把最新 N 条录音拷贝到 Documents/AudioCache，用于历史时间线页离线回听。
    // 超出 N 条按创建时间倒序淘汰（LRU 近似：只看创建时间）。
    // Documents 目录会被 iCloud 备份，但录音本身不敏感且已做网络 OSS；足够 v1 用。

    static let maxCachedRecordings = 30

    /// 持久化录音缓存目录
    var cacheDirectory: URL {
        let base = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first!
        let dir = base.appendingPathComponent("AudioCache")
        if !fileManager.fileExists(atPath: dir.path) {
            try? fileManager.createDirectory(at: dir, withIntermediateDirectories: true)
        }
        return dir
    }

    /// 把录音从临时目录拷到持久缓存（保留原文件名），并触发 LRU 淘汰
    @discardableResult
    func cacheRecording(sourceURL: URL) -> URL? {
        let dest = cacheDirectory.appendingPathComponent(sourceURL.lastPathComponent)
        do {
            if fileManager.fileExists(atPath: dest.path) {
                try fileManager.removeItem(at: dest)
            }
            try fileManager.copyItem(at: sourceURL, to: dest)
        } catch {
            return nil
        }
        pruneCacheIfNeeded()
        return dest
    }

    /// 调用方手动触发：保持 cacheDirectory 内最多 [maxCachedRecordings] 个文件
    func pruneCacheIfNeeded(limit: Int = AudioFileManager.maxCachedRecordings) {
        let keys: [URLResourceKey] = [.creationDateKey]
        guard let files = try? fileManager.contentsOfDirectory(
            at: cacheDirectory, includingPropertiesForKeys: keys,
        ) else { return }

        // 按创建时间降序，前 N 保留，其余删除
        let sorted = files.sorted { lhs, rhs in
            let ld = (try? lhs.resourceValues(forKeys: Set(keys)).creationDate) ?? .distantPast
            let rd = (try? rhs.resourceValues(forKeys: Set(keys)).creationDate) ?? .distantPast
            return ld > rd
        }
        guard sorted.count > limit else { return }
        for file in sorted.dropFirst(limit) {
            try? fileManager.removeItem(at: file)
        }
    }

    /// 当前缓存里的录音 URL，按创建时间从新到旧
    func cachedRecordings() -> [URL] {
        let keys: [URLResourceKey] = [.creationDateKey]
        guard let files = try? fileManager.contentsOfDirectory(
            at: cacheDirectory, includingPropertiesForKeys: keys,
        ) else { return [] }
        return files.sorted { lhs, rhs in
            let ld = (try? lhs.resourceValues(forKeys: Set(keys)).creationDate) ?? .distantPast
            let rd = (try? rhs.resourceValues(forKeys: Set(keys)).creationDate) ?? .distantPast
            return ld > rd
        }
    }

    // MARK: - File Info

    func fileSize(at url: URL) -> Int64 {
        guard let attrs = try? fileManager.attributesOfItem(atPath: url.path),
              let size = attrs[.size] as? Int64 else { return 0 }
        return size
    }

    func audioDuration(at url: URL) -> TimeInterval {
        guard let player = try? AVAudioPlayer(contentsOf: url) else { return 0 }
        return player.duration
    }
}

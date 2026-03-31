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

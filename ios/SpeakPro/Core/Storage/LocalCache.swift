import Foundation

/// 基于 FileManager + Codable 的本地缓存
/// 支持 TTL 过期、自动清理
final class LocalCache {

    static let shared = LocalCache()

    private let cacheDir: URL
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()
    private let defaultTTL: TimeInterval = 24 * 3600 // 24 小时

    private init() {
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        cacheDir = base.appendingPathComponent("SpeakProCache", isDirectory: true)
        try? FileManager.default.createDirectory(at: cacheDir, withIntermediateDirectories: true)
    }

    // MARK: - 存储

    /// 保存 Codable 数据到缓存
    func save<T: Encodable>(_ value: T, forKey key: String, ttl: TimeInterval? = nil) {
        let wrapper = CacheWrapper(
            data: (try? encoder.encode(value)) ?? Data(),
            expiresAt: Date().addingTimeInterval(ttl ?? defaultTTL)
        )
        if let encoded = try? encoder.encode(wrapper) {
            let file = fileURL(for: key)
            try? encoded.write(to: file)
        }
    }

    /// 从缓存读取数据（过期则返回 nil）
    func load<T: Decodable>(forKey key: String, as type: T.Type) -> T? {
        let file = fileURL(for: key)
        guard let raw = try? Data(contentsOf: file),
              let wrapper = try? decoder.decode(CacheWrapper.self, from: raw) else { return nil }

        // 检查是否过期
        if wrapper.expiresAt < Date() {
            try? FileManager.default.removeItem(at: file)
            return nil
        }

        return try? decoder.decode(T.self, from: wrapper.data)
    }

    /// 删除指定缓存
    func invalidate(key: String) {
        let file = fileURL(for: key)
        try? FileManager.default.removeItem(at: file)
    }

    /// 清理所有过期缓存
    func cleanExpired() {
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: cacheDir, includingPropertiesForKeys: nil
        ) else { return }

        for file in files {
            guard let raw = try? Data(contentsOf: file),
                  let wrapper = try? decoder.decode(CacheWrapper.self, from: raw) else {
                try? FileManager.default.removeItem(at: file)
                continue
            }
            if wrapper.expiresAt < Date() {
                try? FileManager.default.removeItem(at: file)
            }
        }
    }

    /// 清理全部缓存
    func clearAll() {
        try? FileManager.default.removeItem(at: cacheDir)
        try? FileManager.default.createDirectory(at: cacheDir, withIntermediateDirectories: true)
    }

    // MARK: - 缓存大小

    var cacheSizeMB: Double {
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: cacheDir, includingPropertiesForKeys: [.fileSizeKey]
        ) else { return 0 }
        let totalBytes = files.compactMap { url -> Int? in
            try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize
        }.reduce(0, +)
        return Double(totalBytes) / 1_048_576.0
    }

    // MARK: - Private

    private func fileURL(for key: String) -> URL {
        let sanitized = key.addingPercentEncoding(withAllowedCharacters: .alphanumerics) ?? key
        return cacheDir.appendingPathComponent(sanitized)
    }
}

// MARK: - Internal

private struct CacheWrapper: Codable {
    let data: Data
    let expiresAt: Date
}

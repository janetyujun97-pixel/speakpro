import Foundation
import Combine
import SwiftUI

/// 离线上传任务 —— 录音 / 评测请求在断网时入队，联网后自动重试。
///
/// 存储：JSON 文件 `Documents/offline_upload_queue.json`（Codable）
/// 文件本身：录音 WAV 已由 AudioRecorder 写在 `Documents/Audio/*.wav`，此处只存 file URL。
///
/// 仅负责队列本身；实际上传由调用方注入 `Uploader` 闭包执行（上传 + 评测回传）。
struct OfflineUploadTask: Codable, Identifiable, Equatable {
    let id: UUID
    /// 本地录音文件（Documents 下的相对路径，跨设备 / 重装会失效但可接受）
    let audioFilename: String
    let sessionId: String?
    let referenceText: String?
    let examType: String?
    let section: String?
    let createdAt: Date
    /// 已重试次数；上限 5 次后转为 failed（保留条目供用户手动处理）
    var retries: Int
    var status: Status

    enum Status: String, Codable { case pending, uploading, failed }

    init(
        audioFilename: String,
        sessionId: String? = nil,
        referenceText: String? = nil,
        examType: String? = nil,
        section: String? = nil,
    ) {
        self.id = UUID()
        self.audioFilename = audioFilename
        self.sessionId = sessionId
        self.referenceText = referenceText
        self.examType = examType
        self.section = section
        self.createdAt = Date()
        self.retries = 0
        self.status = .pending
    }

    /// 文件大小（字节），用于 ErrorStateView 的 "已本地保存 2.4 MB" 展示
    var fileSizeBytes: Int64? {
        guard let url = resolvedURL else { return nil }
        return (try? FileManager.default.attributesOfItem(atPath: url.path))?[.size] as? Int64
    }

    var fileSizeDisplay: String {
        guard let bytes = fileSizeBytes else { return "— MB" }
        let mb = Double(bytes) / (1024 * 1024)
        return String(format: "%.1f MB", mb)
    }

    var resolvedURL: URL? {
        guard let dir = FileManager.default.urls(
            for: .documentDirectory, in: .userDomainMask,
        ).first else { return nil }
        return dir.appendingPathComponent(audioFilename)
    }
}

@MainActor
final class OfflineUploadQueue: ObservableObject {

    static let shared = OfflineUploadQueue()

    @Published private(set) var tasks: [OfflineUploadTask] = []
    @Published private(set) var isProcessing: Bool = false

    /// 上传器：由外层在启动时注入。签名返回成功/失败。
    /// 默认是 no-op（避免忘记注入时死循环）。
    var uploader: ((OfflineUploadTask) async -> Bool)? = nil

    private let storageURL: URL = {
        let dir = FileManager.default.urls(
            for: .documentDirectory, in: .userDomainMask,
        ).first!
        return dir.appendingPathComponent("offline_upload_queue.json")
    }()

    private var cancellables = Set<AnyCancellable>()

    private init() {
        load()
        observeNetwork()
    }

    // MARK: - Public API

    /// 录音完成但上传失败 → 进队
    func enqueue(_ task: OfflineUploadTask) {
        tasks.append(task)
        persist()
    }

    /// 手动重试单条
    func retry(_ id: UUID) {
        Task { await drain() }
    }

    /// 手动删除（用户确认舍弃）
    func remove(_ id: UUID) {
        tasks.removeAll { $0.id == id }
        persist()
    }

    /// 未完成任务数（UI 角标用）
    var pendingCount: Int {
        tasks.filter { $0.status != .failed }.count
    }

    var pendingBytes: Int64 {
        tasks
            .filter { $0.status != .failed }
            .compactMap { $0.fileSizeBytes }
            .reduce(0, +)
    }

    /// 触发一次清空（联网自动 or 手动）
    func drain() async {
        guard !isProcessing else { return }
        guard let uploader else { return }
        guard NetworkMonitor.shared.isConnected else { return }

        isProcessing = true
        defer { isProcessing = false }

        for idx in tasks.indices {
            // 循环过程中 tasks 可能被 enqueue 改动，短路保护
            if idx >= tasks.count { break }
            if tasks[idx].status == .failed { continue }

            tasks[idx].status = .uploading
            persist()

            let ok = await uploader(tasks[idx])
            if ok {
                let id = tasks[idx].id
                tasks.removeAll { $0.id == id }
            } else {
                tasks[idx].retries += 1
                tasks[idx].status = tasks[idx].retries >= 5 ? .failed : .pending
            }
            persist()
        }
    }

    // MARK: - Internal

    private func observeNetwork() {
        NetworkMonitor.shared.$isConnected
            .removeDuplicates()
            .sink { [weak self] connected in
                guard let self, connected else { return }
                Task { await self.drain() }
            }
            .store(in: &cancellables)
    }

    private func persist() {
        do {
            let data = try JSONEncoder().encode(tasks)
            try data.write(to: storageURL, options: .atomic)
        } catch {
            print("[OfflineUploadQueue] persist 失败: \(error)")
        }
    }

    private func load() {
        guard FileManager.default.fileExists(atPath: storageURL.path) else { return }
        do {
            let data = try Data(contentsOf: storageURL)
            tasks = try JSONDecoder().decode([OfflineUploadTask].self, from: data)
            // 启动时把 uploading 态回退为 pending（上次可能是崩溃时留下的）
            for i in tasks.indices where tasks[i].status == .uploading {
                tasks[i].status = .pending
            }
        } catch {
            print("[OfflineUploadQueue] load 失败: \(error)")
            tasks = []
        }
    }
}

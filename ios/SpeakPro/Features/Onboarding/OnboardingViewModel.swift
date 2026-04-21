import Combine
import Foundation
import SwiftUI

/// 8 步 onboarding 的共享状态 + 每步 PATCH 持久化。
/// 每步更新后立即调 `/onboarding/profile`（增量），保证重入时不丢数据。
/// 基线录音走 `/practice/audio`（走 Go 拿 session_id），再把 session_id 写到 profile；
/// 最终 `/onboarding/finalize` 触发服务端生成 study_plan。
@MainActor
final class OnboardingViewModel: ObservableObject {

    enum Step: Int, CaseIterable {
        case welcome
        case examType
        case target
        case date
        case level
        case baselineIntro
        case baselineRecording
        case plan

        var number: Int { rawValue + 1 }
        static var total: Int { Step.allCases.count }
    }

    // MARK: - State

    @Published var step: Step = .welcome
    @Published var examType: OnbExamType? = nil
    @Published var targetScore: Double? = nil
    @Published var examDate: Date? = nil
    @Published var selfLevel: Int? = nil

    @Published var baselineSessionId: String? = nil
    @Published var baselineAudioURL: URL? = nil
    @Published var studyPlan: StudyPlan? = nil

    @Published var isSyncing: Bool = false
    @Published var isFinalizing: Bool = false
    @Published var errorMessage: String? = nil
    @Published var completed: Bool = false

    private let apiClient: APIClient
    private let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = TimeZone(secondsFromGMT: 0)
        return f
    }()

    init(apiClient: APIClient = .shared) {
        self.apiClient = apiClient
    }

    // MARK: - Navigation

    func next() {
        let raw = step.rawValue + 1
        if raw < Step.total, let next = Step(rawValue: raw) {
            step = next
        }
    }

    func back() {
        let raw = step.rawValue - 1
        if raw >= 0, let prev = Step(rawValue: raw) {
            step = prev
        }
    }

    // MARK: - Increment persist

    /// 把当前 profile 片段 PATCH 到服务端。失败时不打断 UI 推进（乐观更新），错误写到 errorMessage。
    func patchCurrent() async {
        let req = UpdateProfileRequest(
            examType: examType,
            targetScore: targetScore,
            examDate: examDate.map { dateFormatter.string(from: $0) },
            selfLevel: selfLevel
        )
        isSyncing = true
        errorMessage = nil
        defer { isSyncing = false }
        do {
            _ = try await apiClient.patchOnboardingProfile(req)
        } catch let e as APIError {
            errorMessage = humanize(e)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// 把基线 session_id 或 audio_url 写到 profile。
    func submitBaseline(sessionId: String?, audioUrl: String?, transcript: String?) async {
        isSyncing = true
        errorMessage = nil
        defer { isSyncing = false }
        do {
            let req = BaselineRequest(
                sessionId: sessionId,
                audioUrl: audioUrl,
                transcript: transcript
            )
            let resp = try await apiClient.postBaseline(req)
            baselineSessionId = resp.sessionId
        } catch let e as APIError {
            errorMessage = humanize(e)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// finalize —— 服务端生成 study_plan 并标记 completed_at
    func finalize() async {
        isFinalizing = true
        errorMessage = nil
        defer { isFinalizing = false }
        do {
            let resp = try await apiClient.finalizeOnboarding()
            studyPlan = resp.profile?.studyPlan
            completed = resp.completed
        } catch let e as APIError {
            errorMessage = humanize(e)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    /// 初始化：从服务端拉已有进度（resume 流程）
    func hydrate() async {
        do {
            let status = try await apiClient.getOnboardingStatus()
            if let p = status.profile {
                examType = p.examType
                targetScore = p.targetScore
                if let s = p.examDate, let d = dateFormatter.date(from: s) { examDate = d }
                selfLevel = p.selfLevel
                baselineSessionId = p.baselineSessionId
                studyPlan = p.studyPlan
                completed = status.completed
            }
        } catch {
            // 拉取失败不影响进入流程；首次注册用户 profile 不存在属正常
        }
    }

    // MARK: - Utils

    /// 目标分合法性校验（雅思 4.0-9.0 / 托福 0-30）
    func isTargetValid(_ v: Double) -> Bool {
        switch examType {
        case .ielts:
            return v >= 4.0 && v <= 9.0 && abs(v * 2 - (v * 2).rounded()) < 1e-6
        case .toefl:
            return v >= 0 && v <= 30
        case .general, .none:
            return true
        }
    }

    private func humanize(_ e: APIError) -> String {
        switch e {
        case .serverError(_, let msg) where !msg.isEmpty: return msg
        case .networkError:                               return "网络连接失败"
        default:                                          return e.localizedDescription
        }
    }
}

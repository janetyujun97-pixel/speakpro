import Foundation

// MARK: - Onboarding 模型

enum OnbExamType: String, Codable, CaseIterable, Identifiable {
    case ielts   = "IELTS"
    case toefl   = "TOEFL"
    case general = "GENERAL"
    var id: String { rawValue }
}

struct OnboardingProfile: Codable, Equatable {
    let userId: String
    let examType: OnbExamType?
    let targetScore: Double?
    let examDate: String?   // ISO 日期 "2026-05-22"
    let selfLevel: Int?     // 1-5
    let baselineSessionId: String?
    let studyPlan: StudyPlan?
    let completedAt: Date?
    let updatedAt: Date?
}

struct StudyPlan: Codable, Equatable {
    let weeks: Int
    let dailyMinutes: Int
    let focusAreas: [String]
    let milestones: [Milestone]

    struct Milestone: Codable, Equatable {
        let week: Int
        let goal: String
    }
}

struct OnboardingStatusResponse: Decodable {
    let completed: Bool
    let profile: OnboardingProfile?
}

struct UpdateProfileRequest: Encodable {
    let examType: OnbExamType?
    let targetScore: Double?
    let examDate: String?
    let selfLevel: Int?
}

struct BaselineRequest: Encodable {
    let sessionId: String?
    let audioUrl: String?
    let transcript: String?
}

struct BaselineResponse: Decodable { let sessionId: String }

// MARK: - APIClient + Onboarding

extension APIClient {

    func getOnboardingStatus() async throws -> OnboardingStatusResponse {
        let resp: APIResponse<OnboardingStatusResponse> =
            try await get(Endpoints.Onboarding.status)
        return try unwrapOnb(resp)
    }

    @discardableResult
    func patchOnboardingProfile(_ req: UpdateProfileRequest) async throws -> OnboardingProfile {
        let resp: APIResponse<OnboardingProfile> =
            try await patch(Endpoints.Onboarding.profile, body: req)
        return try unwrapOnb(resp)
    }

    @discardableResult
    func postBaseline(_ req: BaselineRequest) async throws -> BaselineResponse {
        let resp: APIResponse<BaselineResponse> =
            try await post(Endpoints.Onboarding.baseline, body: req)
        return try unwrapOnb(resp)
    }

    @discardableResult
    func finalizeOnboarding() async throws -> OnboardingStatusResponse {
        let empty: [String: String] = [:]
        let resp: APIResponse<OnboardingStatusResponse> =
            try await post(Endpoints.Onboarding.finalize, body: empty)
        return try unwrapOnb(resp)
    }

    fileprivate func unwrapOnb<T>(_ resp: APIResponse<T>) throws -> T {
        if resp.code != 0 {
            throw APIError.serverError(resp.code, resp.message)
        }
        guard let data = resp.data else {
            throw APIError.noData
        }
        return data
    }
}

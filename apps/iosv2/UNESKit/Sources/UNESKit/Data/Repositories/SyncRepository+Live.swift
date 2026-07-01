import ComposableArchitecture
import Foundation

extension SyncRepository: DependencyKey {
    static let liveValue = SyncRepository(
        ping: {
            @Dependency(\.apiClient) var apiClient
            try await apiClient.post(to: "api/me/ping")
        },
        onboardingStatus: {
            @Dependency(\.apiClient) var apiClient
            let dto: OnboardingStatusDTO = try await apiClient.get(from: "api/sync/onboarding-status")
            return dto.domain
        },
        semesters: {
            @Dependency(\.apiClient) var apiClient
            let dto: SemesterListDTO = try await apiClient.get(from: "api/sync/semesters")
            return dto.semesters.map(\.domain)
        },
        readyOverview: { semester, now in
            @Dependency(\.apiClient) var apiClient
            let dto: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(semester.id)")
            return dto.snapshot.readyOverview(now: now)
        },
        fetchFirstMessagesPage: {
            @Dependency(\.apiClient) var apiClient
            _ = try await apiClient.get(MessagesPageDTO.self, from: "api/sync/messages")
        }
    )
}

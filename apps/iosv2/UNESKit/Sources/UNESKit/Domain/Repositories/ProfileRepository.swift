import ComposableArchitecture

@DependencyClient
struct ProfileRepository: Sendable {
    var current: @Sendable () async throws -> Profile
}

extension ProfileRepository: TestDependencyKey {
    static let testValue = ProfileRepository()
    static let previewValue = ProfileRepository(current: { .preview })
}

extension DependencyValues {
    var profileRepository: ProfileRepository {
        get { self[ProfileRepository.self] }
        set { self[ProfileRepository.self] = newValue }
    }
}

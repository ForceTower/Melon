import ComposableArchitecture

extension ProfileRepository: DependencyKey {
    static let liveValue = ProfileRepository(
        current: {
            @Dependency(\.apiClient) var apiClient
            let dto = try await apiClient.get(ProfileDTO.self, from: "me")
            return dto.domain
        }
    )
}

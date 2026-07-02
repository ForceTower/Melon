import ComposableArchitecture

extension SettingsRepository: DependencyKey {
    static let liveValue = SettingsRepository(
        account: {
            @Dependency(\.apiClient) var apiClient
            let dto = try await apiClient.get(SettingsProfileDTO.self, from: "api/sync/profile")
            return dto.domain
        },
        credentials: {
            @Dependency(\.apiClient) var apiClient
            let dto = try await apiClient.get(CredentialsDTO.self, from: "api/me/credentials")
            return dto.credentials.map {
                AccountCredentials(username: $0.username, password: $0.password)
            }
        },
        update: { change in
            @Dependency(\.apiClient) var apiClient
            let dto = try await apiClient.patch(
                UpdateSettingsDTO.self,
                at: "api/me/settings",
                body: UserSettingsPatchDTO(change)
            )
            return dto.settings.domain
        }
    )
}

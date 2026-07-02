import ComposableArchitecture

@DependencyClient
struct SettingsRepository: Sendable {
    /// Identity + preferences in the one `api/sync/profile` round-trip.
    var account: @Sendable () async throws -> SettingsAccount
    /// The stored SAGRES login pair; nil when nothing is on file.
    var credentials: @Sendable () async throws -> AccountCredentials?
    /// Ships the single changed field; returns the server-canonical row.
    var update: @Sendable (SettingsChange) async throws -> UserSettings
}

extension SettingsRepository: TestDependencyKey {
    static let testValue = SettingsRepository()
    static let previewValue = SettingsRepository(
        account: { .preview },
        credentials: { .preview },
        update: { change in
            var settings = UserSettings()
            settings.apply(change)
            return settings
        }
    )
}

extension DependencyValues {
    var settingsRepository: SettingsRepository {
        get { self[SettingsRepository.self] }
        set { self[SettingsRepository.self] = newValue }
    }
}

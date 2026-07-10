import ComposableArchitecture

/// One-shot importer for the session the previous (KMP-based) generation of
/// the app left behind. Both generations ship under the same bundle ID, so
/// the legacy container survives the App Store update.
@DependencyClient
struct LegacyMigrationClient: Sendable {
    /// Tries to turn whatever the legacy app left on disk into a live session.
    /// Terminal outcomes also delete the legacy artifacts.
    var attempt: @Sendable () async -> LegacyMigrationOutcome = { .nothing }
    /// Deletes leftover legacy artifacts, for launches that already have a
    /// session (e.g. the user signed in manually after a failed attempt).
    var removeArtifacts: @Sendable () -> Void
}

enum LegacyMigrationOutcome: Equatable, Sendable {
    /// A session was recovered and persisted — the app can go straight in.
    case migrated(Session)
    /// Recovery is impossible (rejected token, stale or missing credentials);
    /// the login screen can be prefilled with the legacy username when known.
    case loginRequired(prefillUsername: String?)
    /// Transient failure (offline, server error) — artifacts were kept so the
    /// next launch retries.
    case retry
    /// Nothing legacy on disk.
    case nothing
}

extension LegacyMigrationClient: TestDependencyKey {
    static let testValue = LegacyMigrationClient()

    static var previewValue: LegacyMigrationClient {
        LegacyMigrationClient(attempt: { .nothing }, removeArtifacts: {})
    }
}

extension DependencyValues {
    var legacyMigration: LegacyMigrationClient {
        get { self[LegacyMigrationClient.self] }
        set { self[LegacyMigrationClient.self] = newValue }
    }
}

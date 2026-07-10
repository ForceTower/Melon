import ComposableArchitecture
import Foundation
import GRDB

private let log = Log.scoped("LegacyMigration")

extension LegacyMigrationClient: DependencyKey {
    static let liveValue = LegacyMigrationClient(
        attempt: {
            @Dependency(\.apiClient) var apiClient
            @Dependency(\.sessionStore) var sessionStore
            @Dependency(\.authRepository) var authRepository

            guard let carryover = LegacyStore.read() else { return .nothing }
            log.info(
                "attempt start tokenPresent=\(carryover.accessToken != nil) credentialsPresent=\(carryover.credentials != nil)"
            )

            // Adopt the legacy tokens as-is: the refresh token is single-use,
            // so it must not be rotated before the session is safely persisted.
            if let accessToken = carryover.accessToken {
                do {
                    let dto = try await apiClient.get(
                        ProfileDTO.self,
                        from: "api/sync/profile",
                        authorization: .bearer(accessToken)
                    )
                    guard sessionStore.current() == nil else {
                        log.info("attempt aborted: a session appeared mid-flight")
                        LegacyStore.remove()
                        return .nothing
                    }
                    let session = Session(
                        accessToken: accessToken,
                        refreshToken: carryover.refreshToken ?? "",
                        user: SessionUser(id: dto.user.id, name: dto.user.name, imageUrl: dto.user.imageUrl)
                    )
                    try sessionStore.save(session)
                    LegacyStore.remove()
                    log.info("attempt ok: token adopted userId=\(session.user.id)")
                    return .migrated(session)
                } catch APIError.server(401, _) {
                    log.info("legacy token rejected, falling back to credentials")
                } catch {
                    log.warn("token adoption failed, keeping artifacts for retry", error: error)
                    return .retry
                }
            }

            if let credentials = carryover.credentials {
                guard sessionStore.current() == nil else {
                    log.info("attempt aborted: a session appeared mid-flight")
                    LegacyStore.remove()
                    return .nothing
                }
                do {
                    let session = try await authRepository.login(
                        username: credentials.username,
                        password: credentials.password
                    )
                    LegacyStore.remove()
                    log.info("attempt ok: credentials replayed userId=\(session.user.id)")
                    return .migrated(session)
                } catch AuthError.invalidCredentials {
                    log.warn("legacy credentials rejected, sign-in required")
                    LegacyStore.remove()
                    return .loginRequired(prefillUsername: credentials.username)
                } catch {
                    log.warn("credential replay failed, keeping artifacts for retry", error: error)
                    return .retry
                }
            }

            let hadToken = carryover.accessToken != nil
            log.info("attempt exhausted: no recovery path hadToken=\(hadToken)")
            LegacyStore.remove()
            return hadToken ? .loginRequired(prefillUsername: nil) : .nothing
        },
        removeArtifacts: {
            guard LegacyStore.isPresent else { return }
            log.info("removing stale legacy artifacts")
            LegacyStore.remove()
        }
    )
}

/// The legacy app's on-disk session: tokens in the `dev.forcetower.melon`
/// NSUserDefaults suite (the KMP KeyValueStorage backend) and the typed
/// SAGRES credentials in the Room database at `Documents/melon.db`.
private enum LegacyStore {
    private static let suiteName = "dev.forcetower.melon"
    private static let accessTokenKey = "melon.access_token"
    private static let refreshTokenKey = "melon.refresh_token"

    struct Carryover: Sendable {
        var accessToken: String?
        var refreshToken: String?
        var credentials: Credentials?
    }

    struct Credentials: Sendable {
        var username: String
        var password: String
    }

    private static var databaseURL: URL {
        URL.documentsDirectory.appending(path: "melon.db")
    }

    static var isPresent: Bool {
        UserDefaults(suiteName: suiteName)?.string(forKey: accessTokenKey) != nil
            || FileManager.default.fileExists(atPath: databaseURL.path)
    }

    static func read() -> Carryover? {
        let suite = UserDefaults(suiteName: suiteName)
        let accessToken = suite?.string(forKey: accessTokenKey)
        let hasDatabase = FileManager.default.fileExists(atPath: databaseURL.path)
        guard accessToken != nil || hasDatabase else { return nil }
        return Carryover(
            accessToken: accessToken,
            refreshToken: suite?.string(forKey: refreshTokenKey),
            credentials: hasDatabase ? readCredentials() : nil
        )
    }

    private static func readCredentials() -> Credentials? {
        do {
            var configuration = Configuration()
            configuration.readonly = true
            let queue = try DatabaseQueue(path: databaseURL.path, configuration: configuration)
            return try queue.read { db -> Credentials? in
                guard
                    let row = try Row.fetchOne(db, sql: "SELECT username, password FROM Credentials LIMIT 1"),
                    let username = row["username"] as String?,
                    let password = row["password"] as String?
                else { return nil }
                return Credentials(username: username, password: password)
            }
        } catch {
            log.warn("legacy credentials unreadable", error: error)
            return nil
        }
    }

    static func remove() {
        UserDefaults(suiteName: suiteName)?.removePersistentDomain(forName: suiteName)
        for suffix in ["", "-wal", "-shm"] {
            try? FileManager.default.removeItem(at: URL(fileURLWithPath: databaseURL.path + suffix))
        }
        log.info("legacy artifacts removed")
    }
}

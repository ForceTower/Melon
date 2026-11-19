import ComposableArchitecture
import Foundation
import Security

private let log = Log.scoped("SessionStore")

extension SessionStore: DependencyKey {
    static let liveValue: SessionStore = {
        let initial = Keychain.read(Session.self, account: Keychain.sessionAccount)
        log.info("session bootstrapped tokenPresent=\(initial != nil)")
        let cached = LockIsolated<Session?>(initial)
        let credentials = LockIsolated<AccountCredentials?>(
            Keychain.read(AccountCredentials.self, account: Keychain.credentialsAccount)
        )
        return SessionStore(
            current: { cached.value },
            save: { session in
                do {
                    try Keychain.write(session, account: Keychain.sessionAccount)
                } catch {
                    log.error("session persist failed userId=\(session.user.id)", error: error)
                    throw error
                }
                cached.setValue(session)
                log.info("session persisted userId=\(session.user.id)")
            },
            clear: {
                do {
                    // Credentials first: a failure then leaves the session, and so the shell, intact.
                    try Keychain.delete(account: Keychain.credentialsAccount)
                    credentials.setValue(nil)
                    try Keychain.delete(account: Keychain.sessionAccount)
                } catch {
                    log.error("session clear failed", error: error)
                    throw error
                }
                cached.setValue(nil)
                log.info("session cleared")
            },
            upstreamCredentials: { credentials.value },
            saveUpstreamCredentials: { new in
                do {
                    try Keychain.write(new, account: Keychain.credentialsAccount)
                    credentials.setValue(new)
                    log.info("upstream credentials saved")
                } catch {
                    log.error("upstream credentials persist failed", error: error)
                }
            }
        )
    }()
}

private struct KeychainError: Error {
    let status: OSStatus
}

private enum Keychain {
    static let sessionAccount = "session"
    static let credentialsAccount = "upstream-credentials"

    private static let service = "dev.forcetower.unes.session"

    private static func baseQuery(account: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }

    static func read<Value: Decodable>(_ type: Value.Type, account: String) -> Value? {
        var query = baseQuery(account: account)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data
        else { return nil }
        do {
            return try JSONDecoder().decode(type, from: data)
        } catch {
            log.warn("keychain decode failed account=\(account)", error: error)
            return nil
        }
    }

    static func write(_ value: some Encodable, account: String) throws {
        let data = try JSONEncoder().encode(value)
        try delete(account: account)

        var attributes = baseQuery(account: account)
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock

        let status = SecItemAdd(attributes as CFDictionary, nil)
        guard status == errSecSuccess else { throw KeychainError(status: status) }
    }

    static func delete(account: String) throws {
        let status = SecItemDelete(baseQuery(account: account) as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError(status: status)
        }
    }
}

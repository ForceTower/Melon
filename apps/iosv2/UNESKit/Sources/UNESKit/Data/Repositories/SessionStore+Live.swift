import ComposableArchitecture
import Foundation
import Security

extension SessionStore: DependencyKey {
    static let liveValue: SessionStore = {
        let cached = LockIsolated<Session?>(Keychain.readSession())
        return SessionStore(
            current: { cached.value },
            save: { session in
                try Keychain.writeSession(session)
                cached.setValue(session)
            },
            clear: {
                try Keychain.deleteSession()
                cached.setValue(nil)
            }
        )
    }()
}

private struct KeychainError: Error {
    let status: OSStatus
}

private enum Keychain {
    private static let service = "dev.forcetower.unes.session"
    private static let account = "session"

    private static var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }

    static func readSession() -> Session? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data
        else { return nil }
        return try? JSONDecoder().decode(Session.self, from: data)
    }

    static func writeSession(_ session: Session) throws {
        let data = try JSONEncoder().encode(session)
        try deleteSession()

        var attributes = baseQuery
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock

        let status = SecItemAdd(attributes as CFDictionary, nil)
        guard status == errSecSuccess else { throw KeychainError(status: status) }
    }

    static func deleteSession() throws {
        let status = SecItemDelete(baseQuery as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError(status: status)
        }
    }
}

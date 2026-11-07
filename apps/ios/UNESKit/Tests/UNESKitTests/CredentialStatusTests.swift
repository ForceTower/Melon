import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

struct CredentialStatusTests {
    private static func poll(_ status: String) async throws -> CredentialHealth {
        try await withDependencies {
            $0.apiClient.send = { _ in
                Data(#"{"ok":true,"data":{"credentials":{"status":"\#(status)","username":"20191234"}}}"#.utf8)
            }
        } operation: {
            try await CredentialStatusRepository.liveValue.current()
        }
    }

    @Test
    func aStatusThisBuildDoesNotKnowIsNotReadAsHealthy() async throws {
        #expect(try await Self.poll("locked").status == .unknown)
    }

    @Test
    func statusCasingMatchesAndroid() async throws {
        #expect(try await Self.poll("INVALID").status == .invalid)
    }

    @Test
    func anUnknownStatusLeavesTheBannerUp() {
        let defaults = UserDefaults.inMemory
        defaults.set(true, forKey: CredentialInvalidation.storageKey)

        withDependencies {
            $0.defaultAppStorage = defaults
        } operation: {
            CredentialInvalidation.liveValue.apply(CredentialHealth(status: .unknown, username: "20191234"))
        }

        #expect(defaults.bool(forKey: CredentialInvalidation.storageKey))
        #expect(defaults.string(forKey: CredentialInvalidation.usernameKey) == "20191234")
    }

    @Test
    func resetForgetsTheFlagAndTheUsername() {
        let defaults = UserDefaults.inMemory
        defaults.set(true, forKey: CredentialInvalidation.storageKey)
        defaults.set("20191234", forKey: CredentialInvalidation.usernameKey)

        withDependencies {
            $0.defaultAppStorage = defaults
        } operation: {
            CredentialInvalidation.liveValue.reset()
        }

        #expect(defaults.object(forKey: CredentialInvalidation.storageKey) == nil)
        #expect(defaults.string(forKey: CredentialInvalidation.usernameKey) == nil)
    }
}

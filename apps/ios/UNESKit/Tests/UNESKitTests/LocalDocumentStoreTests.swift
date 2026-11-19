import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct LocalDocumentStoreTests {
    @Test
    func aSavedCopyOnlyOpensForTheAccountThatSavedIt() throws {
        let sessions = SessionStore.inMemory(initial: .preview)
        let otherAccount = Session(
            accessToken: "other-token",
            refreshToken: "other-refresh",
            user: SessionUser(id: "other", name: "Outra Pessoa", imageUrl: nil)
        )
        let download = URL.temporaryDirectory.appending(path: "historico-\(UUID().uuidString).pdf")
        try Data("%PDF-synthetic".utf8).write(to: download)

        try withDependencies {
            $0.sessionStore = sessions
        } operation: {
            let store = LocalDocumentStore.liveValue
            try store.clear()
            defer { try? store.clear() }

            _ = try store.save(.academicHistory, download)
            #expect(store.load(.academicHistory) != nil)

            try sessions.save(otherAccount)
            #expect(store.load(.academicHistory) == nil)

            try sessions.save(.preview)
            try store.clear()
            #expect(store.load(.academicHistory) == nil)
        }
    }
}

import ComposableArchitecture
import Foundation

/// The device-local slot each official document is kept in for offline
/// access — one file per kind, replaced on every successful refresh.
@DependencyClient
struct LocalDocumentStore: Sendable {
    /// The offline copy saved on this device, or nil before the first fetch.
    var load: @Sendable (_ document: AcademicDocument) -> StoredAcademicDocument? = { _ in nil }
    /// Moves a freshly downloaded PDF into the offline slot, bumping its
    /// version.
    var save: @Sendable (_ document: AcademicDocument, _ fileURL: URL) throws -> StoredAcademicDocument
}

extension LocalDocumentStore: TestDependencyKey {
    static let testValue = LocalDocumentStore()

    static let previewValue = LocalDocumentStore(
        load: { _ in nil },
        save: { _, fileURL in
            StoredAcademicDocument(fileURL: fileURL, version: 1, savedAt: .now)
        }
    )
}

extension DependencyValues {
    var localDocuments: LocalDocumentStore {
        get { self[LocalDocumentStore.self] }
        set { self[LocalDocumentStore.self] = newValue }
    }
}

import ComposableArchitecture
import Foundation

/// Files live in Application Support (backed up, never purged like tmp),
/// one PDF per document kind plus a JSON sidecar with version, save date and
/// the account that saved it.
extension LocalDocumentStore: DependencyKey {
    static let liveValue = LocalDocumentStore(
        load: { document in
            let fileURL = slot(for: document)
            // Copies saved before owners were recorded have none, so they never show.
            guard FileManager.default.fileExists(atPath: fileURL.path),
                  let owner = signedInUserId(),
                  let meta = readMeta(for: document),
                  meta.ownerId == owner
            else { return nil }
            return StoredAcademicDocument(fileURL: fileURL, version: meta.version, savedAt: meta.savedAt)
        },
        save: { document, downloadedURL in
            let fileURL = slot(for: document)
            try FileManager.default.createDirectory(
                at: fileURL.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            let data = try Data(contentsOf: downloadedURL)
            try data.write(to: fileURL, options: .atomic)

            let meta = DocumentMeta(
                version: (readMeta(for: document)?.version ?? 0) + 1,
                savedAt: .now,
                ownerId: signedInUserId()
            )
            try JSONEncoder().encode(meta).write(to: metaURL(for: document), options: .atomic)
            return StoredAcademicDocument(fileURL: fileURL, version: meta.version, savedAt: meta.savedAt)
        },
        clear: {
            guard FileManager.default.fileExists(atPath: directory().path) else { return }
            try FileManager.default.removeItem(at: directory())
        }
    )
}

private struct DocumentMeta: Codable {
    var version: Int
    var savedAt: Date
    var ownerId: String?
}

private func signedInUserId() -> String? {
    @Dependency(\.sessionStore) var sessionStore
    return sessionStore.current()?.user.id
}

private func slot(for document: AcademicDocument) -> URL {
    directory().appendingPathComponent(document.fileName)
}

private func metaURL(for document: AcademicDocument) -> URL {
    directory().appendingPathComponent("\(document.fileName).meta.json")
}

private func directory() -> URL {
    URL.applicationSupportDirectory.appendingPathComponent("AcademicDocuments", isDirectory: true)
}

private func readMeta(for document: AcademicDocument) -> DocumentMeta? {
    guard let data = try? Data(contentsOf: metaURL(for: document)) else { return nil }
    return try? JSONDecoder().decode(DocumentMeta.self, from: data)
}

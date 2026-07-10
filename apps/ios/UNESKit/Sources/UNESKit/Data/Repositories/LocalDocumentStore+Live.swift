import ComposableArchitecture
import Foundation

/// Files live in Application Support (backed up, never purged like tmp),
/// one PDF per document kind plus a JSON sidecar with version + save date.
extension LocalDocumentStore: DependencyKey {
    static let liveValue = LocalDocumentStore(
        load: { document in
            let fileURL = slot(for: document)
            guard FileManager.default.fileExists(atPath: fileURL.path) else { return nil }
            let meta = readMeta(for: document)
            return StoredAcademicDocument(
                fileURL: fileURL,
                version: meta?.version ?? 1,
                savedAt: meta?.savedAt ?? .distantPast
            )
        },
        save: { document, downloadedURL in
            let fileURL = slot(for: document)
            try FileManager.default.createDirectory(
                at: fileURL.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            let data = try Data(contentsOf: downloadedURL)
            try data.write(to: fileURL, options: .atomic)

            let meta = DocumentMeta(version: (readMeta(for: document)?.version ?? 0) + 1, savedAt: .now)
            try JSONEncoder().encode(meta).write(to: metaURL(for: document), options: .atomic)
            return StoredAcademicDocument(fileURL: fileURL, version: meta.version, savedAt: meta.savedAt)
        }
    )
}

private struct DocumentMeta: Codable {
    var version: Int
    var savedAt: Date
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

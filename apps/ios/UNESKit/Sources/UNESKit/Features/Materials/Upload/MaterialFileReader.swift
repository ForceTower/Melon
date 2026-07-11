import Foundation
#if canImport(PDFKit)
import PDFKit
#endif

/// Reads a picker-provided PDF into upload-ready form. The URL is
/// security-scoped — access must be bracketed.
enum MaterialFileReader {
    struct ReadFailed: Error {}

    static func read(_ url: URL) throws -> MaterialPickedFile {
        let scoped = url.startAccessingSecurityScopedResource()
        defer {
            if scoped { url.stopAccessingSecurityScopedResource() }
        }
        let data = try Data(contentsOf: url)
        return MaterialPickedFile(
            fileName: url.lastPathComponent,
            byteCount: data.count,
            pages: pageCount(of: data),
            data: data,
            isScan: false
        )
    }

    private static func pageCount(of data: Data) -> Int {
        #if canImport(PDFKit)
        PDFDocument(data: data)?.pageCount ?? 1
        #else
        1
        #endif
    }
}

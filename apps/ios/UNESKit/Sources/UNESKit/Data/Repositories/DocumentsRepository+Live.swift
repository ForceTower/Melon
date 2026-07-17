import ComposableArchitecture
import CoreGraphics
import CoreText
import Foundation

/// `POST /api/documents/fetch` pulls the PDF fresh from the university portal
/// (carrying the reCAPTCHA answer when one was demanded) and stores it
/// server-side; the response is a short-lived presigned URL this client then
/// downloads into a temp file for QuickLook.
extension DocumentsRepository: DependencyKey {
    static let liveValue: DocumentsRepository = {
        let log = Log.scoped("DocumentsRepository")
        return DocumentsRepository(
            fetch: { document, captchaToken in
                @Dependency(\.apiClient) var api
                log.info("document fetch start kind=\(document.rawValue) captcha=\(captchaToken != nil)")
                do {
                    let fetched: DocumentFetchResponse = try await api.post(
                        to: "api/documents/fetch",
                        body: DocumentFetchRequest(kind: document.rawValue, captchaToken: captchaToken)
                    )
                    let url = try await downloadPDF(from: fetched.download.url, filename: fetched.document.filename)
                    log.info("document fetch ok kind=\(document.rawValue) fresh=\(fetched.fresh)")
                    return FetchedAcademicDocument(
                        fileURL: url,
                        isFresh: fetched.fresh,
                        generatedAt: fetched.document.generatedAt
                    )
                } catch {
                    log.error("document fetch failed kind=\(document.rawValue)", error: error)
                    throw error
                }
            }
        )
    }()
}

private struct DocumentFetchRequest: Encodable {
    let kind: String
    let captchaToken: String?
}

private struct DocumentFetchResponse: Decodable {
    struct Document: Decodable {
        let filename: String
        /// ISO8601 with fractional seconds — kept as a string on the wire,
        /// same convention as `MessageDTO`.
        let createdAt: String

        var generatedAt: Date {
            (try? Date(createdAt, strategy: Date.ISO8601FormatStyle(includingFractionalSeconds: true))) ?? .now
        }
    }

    struct Download: Decodable {
        let url: URL
    }

    let document: Document
    let download: Download
    let fresh: Bool
}

/// The presigned URL is its own bearer token — plain GET, no session header.
private func downloadPDF(from url: URL, filename: String) async throws -> URL {
    let (data, response) = try await URLSession.shared.data(from: url)
    guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode else {
        throw APIError.invalidResponse
    }
    let destination = FileManager.default.temporaryDirectory.appendingPathComponent(safeFileName(filename))
    try data.write(to: destination, options: .atomic)
    return destination
}

/// Draws a one-page A4 placeholder so previews have a real file to show.
/// Pure CoreGraphics/CoreText so it also compiles for the macOS test host and
/// the watch target.
enum MockDocumentPDF {
    struct WriteFailed: Error {}

    static func write(_ document: AcademicDocument) throws -> URL {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(document.fileName)
        var mediaBox = CGRect(x: 0, y: 0, width: 595, height: 842)
        guard let context = CGContext(url as CFURL, mediaBox: &mediaBox, nil) else {
            throw WriteFailed()
        }
        context.beginPDFPage(nil)
        draw(title(for: document), size: 24, y: 780, in: context)
        draw(String.localized(.meDocumentSampleNotice), size: 12, y: 748, in: context)
        context.endPDFPage()
        context.closePDF()
        return url
    }

    private static func title(for document: AcademicDocument) -> String {
        switch document {
        case .enrollmentCertificate: .localized(.meDocumentCertificateTitle)
        case .academicHistory: .localized(.meDocumentHistoryTitle)
        }
    }

    private static func draw(_ text: String, size: CGFloat, y: CGFloat, in context: CGContext) {
        let font = CTFontCreateUIFontForLanguage(.system, size, nil)
        let attributes: [NSAttributedString.Key: Any] = [
            NSAttributedString.Key(kCTFontAttributeName as String): font as Any,
            NSAttributedString.Key(kCTForegroundColorAttributeName as String): CGColor(gray: 0.12, alpha: 1),
        ]
        let line = CTLineCreateWithAttributedString(
            NSAttributedString(string: text, attributes: attributes)
        )
        context.textPosition = CGPoint(x: 48, y: y)
        CTLineDraw(line, context)
    }
}

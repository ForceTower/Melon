import ComposableArchitecture
import Foundation

/// Requests SAGRES-issued documents through the backend. The call carries the
/// reCAPTCHA answer when the portal demands one; the backend solves the fetch
/// upstream and streams the signed PDF back.
@DependencyClient
struct DocumentsRepository: Sendable {
    /// Fetches the PDF and returns the downloaded file plus its freshness.
    var fetch: @Sendable (_ document: AcademicDocument, _ captchaToken: String?) async throws -> FetchedAcademicDocument
}

extension DocumentsRepository: TestDependencyKey {
    static let testValue = DocumentsRepository()

    static let previewValue = DocumentsRepository(
        fetch: { document, _ in
            try await Task.sleep(for: .seconds(1))
            return FetchedAcademicDocument(
                fileURL: try MockDocumentPDF.write(document),
                isFresh: true,
                generatedAt: .now
            )
        }
    )
}

extension DependencyValues {
    var documentsRepository: DocumentsRepository {
        get { self[DocumentsRepository.self] }
        set { self[DocumentsRepository.self] = newValue }
    }
}

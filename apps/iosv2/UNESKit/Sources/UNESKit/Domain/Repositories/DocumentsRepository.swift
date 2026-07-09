import ComposableArchitecture
import Foundation

/// Requests SAGRES-issued documents through the backend. The call carries the
/// reCAPTCHA answer when the portal demands one; the backend solves the fetch
/// upstream and streams the signed PDF back.
@DependencyClient
struct DocumentsRepository: Sendable {
    /// Fetches the PDF and returns the local file it was written to.
    var fetch: @Sendable (_ document: AcademicDocument, _ captchaToken: String?) async throws -> URL
}

extension DocumentsRepository: TestDependencyKey {
    static let testValue = DocumentsRepository()

    static let previewValue = DocumentsRepository(
        fetch: { document, _ in
            try await Task.sleep(for: .seconds(1))
            return try MockDocumentPDF.write(document)
        }
    )
}

extension DependencyValues {
    var documentsRepository: DocumentsRepository {
        get { self[DocumentsRepository.self] }
        set { self[DocumentsRepository.self] = newValue }
    }
}

import Foundation

/// The SAGRES-issued PDFs the app can request through the backend. The raw
/// value doubles as the request identifier the endpoint will take.
enum AcademicDocument: String, Equatable, Sendable, CaseIterable {
    case enrollmentCertificate = "enrollment-certificate"
    case academicHistory = "academic-history"

    /// The name the saved PDF gets — shown on the ready card and in Files.
    var fileName: String {
        switch self {
        case .enrollmentCertificate: "comprovante-matricula.pdf"
        case .academicHistory: "historico-escolar.pdf"
        }
    }
}

/// A downloaded document: the local file plus whether the backend produced
/// it fresh or fell back to the newest stored version after the portal
/// fetch failed.
struct FetchedAcademicDocument: Equatable, Sendable {
    var fileURL: URL
    var isFresh: Bool
    var generatedAt: Date
}

/// The offline copy kept on this device so the document opens without a
/// connection. `version` bumps on every successful refresh.
struct StoredAcademicDocument: Equatable, Sendable {
    var fileURL: URL
    var version: Int
    var savedAt: Date
}

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

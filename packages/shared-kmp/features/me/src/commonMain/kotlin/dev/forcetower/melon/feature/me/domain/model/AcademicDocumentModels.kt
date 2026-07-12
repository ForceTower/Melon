package dev.forcetower.melon.feature.me.domain.model

// The SAGRES-issued PDFs the app can request through the backend. `kind` is
// the request identifier `POST api/documents/fetch` takes; `fileName` is the
// name the offline copy gets on device. Mirrors iOS `AcademicDocument`.
enum class AcademicDocument(val kind: String, val fileName: String) {
    EnrollmentCertificate("enrollment-certificate", "comprovante-matricula.pdf"),
    AcademicHistory("academic-history", "historico-escolar.pdf"),
}

// A downloaded document: the PDF bytes plus whether the backend produced it
// fresh or fell back to the newest stored version after the portal fetch
// failed. `generatedAtIso` is the server copy's creation stamp (ISO8601).
class FetchedAcademicDocument(
    val bytes: ByteArray,
    val fileName: String,
    val fresh: Boolean,
    val generatedAtIso: String,
)

// Why a fetch produced nothing. Mirrors iOS `FailureReason`.
enum class DocumentFetchError {
    // The backend answered 404: the portal did not issue this document for
    // the student.
    Unavailable,

    // Request never landed, or the server errored.
    Connection,
}

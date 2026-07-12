package dev.forcetower.melon.feature.materials.domain.model

// Materiais — the collaborative study-materials shelf. Online-only by design:
// every screen fetches live from `api/materials/*` and nothing is persisted on
// device ("Salvar" is a server-side bookmark). Mirrors iOS
// `Domain/Models/Materials.swift`; raw values are the wire contract.

enum class MaterialType(val wire: String) {
    Exam("exam"),
    SolvedList("list"),
    Summary("summary"),
    FormulaSheet("formula"),
    ;

    companion object {
        fun fromWire(raw: String): MaterialType? = entries.firstOrNull { it.wire == raw }
    }
}

enum class MaterialFileKind(val wire: String) {
    Pdf("pdf"),
    Photo("photo"),
    ;

    companion object {
        fun fromWire(raw: String): MaterialFileKind? = entries.firstOrNull { it.wire == raw }
    }
}

enum class MaterialStatus(val wire: String) {
    Published("published"),
    Pending("pending"),
    Rejected("rejected"),
    ;

    companion object {
        fun fromWire(raw: String): MaterialStatus? = entries.firstOrNull { it.wire == raw }
    }
}

enum class MaterialReportReason(val wire: String) {
    Illegible("illegible"),
    OngoingExam("ongoing_exam"),
    RestrictedByTeacher("restricted_by_teacher"),
    WrongDiscipline("wrong_discipline"),
    Other("other"),
}

// Semi-anonymous attribution: course + entry year, never a name.
data class MaterialUploader(
    val course: String,
    val entryYear: Int,
)

// Embedded on each Material so screens reached directly (saved shelf, deep
// links) can render discipline context without a second fetch.
data class MaterialDisciplineRef(
    val id: String,
    val code: String,
    val name: String,
)

data class Material(
    val id: String,
    val discipline: MaterialDisciplineRef,
    val type: MaterialType,
    val title: String,
    val teacherName: String?,
    val semester: String,
    val pages: Int,
    val fileKind: MaterialFileKind,
    val usefulCount: Int,
    val downloadCount: Int,
    val uploader: MaterialUploader,
    val note: String?,
    val isMine: Boolean,
    val status: MaterialStatus,
    // Present when `status == Rejected` — the moderation note shown on the
    // student's own submission.
    val rejectionReason: String?,
    val isUseful: Boolean,
    val isSaved: Boolean,
)

data class MaterialsDiscipline(
    val id: String,
    val code: String,
    val name: String,
    val teacherName: String?,
    val counts: Map<MaterialType, Int>,
) {
    val total: Int get() = counts.values.sum()
}

data class MaterialsOverview(
    val semester: String,
    val disciplines: List<MaterialsDiscipline>,
    val savedCount: Int,
) {
    val totalCount: Int get() = disciplines.sumOf { it.total }
}

data class MaterialsDisciplineDetails(
    val discipline: MaterialsDiscipline,
    val materials: List<Material>,
) {
    val published: List<Material> get() = materials.filter { it.status == MaterialStatus.Published }

    // The student's own uploads still in moderation (pending/rejected).
    val mine: List<Material> get() = materials.filter { it.isMine && it.status != MaterialStatus.Published }
}

// Upload payload before it's split into presigned slot + metadata calls.
// The client always produces PDF bytes (file picker is PDF-restricted and
// camera scans are flattened into a PDF), so `fileKind` is Pdf today; Photo
// exists for server-returned materials.
class MaterialSubmission(
    val disciplineId: String,
    val type: MaterialType,
    val title: String,
    val semester: String,
    val teacherName: String?,
    val fileKind: MaterialFileKind,
    val pages: Int,
    val fileName: String,
    val bytes: ByteArray,
)

// A downloaded material file, ready to be written to a temp location and
// handed to the platform viewer.
class FetchedMaterialFile(
    val bytes: ByteArray,
    val fileName: String,
)

enum class MaterialsError { Connection }

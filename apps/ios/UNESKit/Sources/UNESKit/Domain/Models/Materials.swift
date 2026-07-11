import Foundation

// MARK: - Materiais — student-contributed study materials

/// What kind of document a material is. Raw values are the wire contract.
enum MaterialType: String, Equatable, Sendable, CaseIterable {
    case exam
    case solvedList = "list"
    case summary
    case formulaSheet = "formula"
}

enum MaterialFileKind: String, Equatable, Sendable {
    case pdf
    case photo
}

/// Moderation state. Everyone browses `published`; `pending` and `rejected`
/// only ever arrive for the student's own uploads.
enum MaterialStatus: String, Equatable, Sendable {
    case published
    case pending
    case rejected
}

/// Semi-anonymous attribution: course + entry year, never a name.
struct MaterialUploader: Equatable, Sendable {
    var course: String
    var entryYear: Int
}

/// The discipline a material belongs to, as embedded on each material so
/// screens entered directly (saved list, future deep links) can render
/// context without a second fetch. `colorIndex` is client-assigned.
struct MaterialDisciplineRef: Equatable, Sendable {
    var id: String
    var code: String
    var name: String
    var colorIndex: Int
}

struct Material: Equatable, Sendable, Identifiable {
    var id: String
    var discipline: MaterialDisciplineRef
    var type: MaterialType
    var title: String
    var teacherName: String?
    /// Semester label the material refers to — "2025.2".
    var semester: String
    var pages: Int
    var fileKind: MaterialFileKind
    var usefulCount: Int
    var downloadCount: Int
    var uploader: MaterialUploader
    /// Optional uploader-written description.
    var note: String?
    /// Uploaded by the current student.
    var isMine: Bool
    var status: MaterialStatus
    /// Moderation copy, present when `status == .rejected`.
    var rejectionReason: String?
    /// Whether the current student voted this material "útil".
    var isUseful: Bool
    /// Whether the current student bookmarked it ("Salvos", server-synced).
    var isSaved: Bool
}

/// One of the student's current disciplines on the hub, with its per-type
/// material tallies.
struct MaterialsDiscipline: Equatable, Sendable, Identifiable {
    var id: String
    var code: String
    var name: String
    var teacherName: String?
    var colorIndex: Int
    /// Published materials per type; absent types count zero.
    var counts: [MaterialType: Int]

    var total: Int { counts.values.reduce(0, +) }

    func count(of type: MaterialType) -> Int { counts[type] ?? 0 }
}

/// The Materiais landing payload.
struct MaterialsOverview: Equatable, Sendable {
    /// Current semester label — "2025.2".
    var semester: String
    var disciplines: [MaterialsDiscipline]
    /// Materials the student bookmarked, across all disciplines.
    var savedCount: Int

    var totalCount: Int { disciplines.reduce(0) { $0 + $1.total } }
}

/// One discipline's shelf: published materials plus the student's own
/// pending/rejected uploads.
struct MaterialsDisciplineDetails: Equatable, Sendable {
    var discipline: MaterialsDiscipline
    var materials: [Material]

    var published: [Material] { materials.filter { $0.status == .published } }
    /// Own uploads still outside the public shelf.
    var mine: [Material] { materials.filter { $0.isMine && $0.status != .published } }
}

/// The fixed report reasons. Raw values are the wire contract.
enum MaterialReportReason: String, Equatable, Sendable, CaseIterable {
    case illegible
    case ongoingExam = "ongoing_exam"
    case restrictedByTeacher = "restricted_by_teacher"
    case wrongDiscipline = "wrong_discipline"
    case other
}

/// A new upload, ready to submit for moderation.
struct MaterialSubmission: Equatable, Sendable {
    var disciplineId: String
    var type: MaterialType
    var title: String
    var semester: String
    var teacherName: String?
    var fileKind: MaterialFileKind
    var pages: Int
    var fileName: String
    var data: Data
}

// MARK: - Previews

extension MaterialsOverview {
    static func preview() -> MaterialsOverview {
        MaterialsOverview(
            semester: "2025.2",
            disciplines: [
                MaterialsDiscipline(
                    id: "d1", code: "EXA805", name: "Algoritmos e Programação II",
                    teacherName: "Camila Ribeiro", colorIndex: 0,
                    counts: [.exam: 2, .solvedList: 2, .summary: 1, .formulaSheet: 1]
                ),
                MaterialsDiscipline(
                    id: "d2", code: "EXA704", name: "Cálculo Diferencial e Integral II",
                    teacherName: "Adriana Matos", colorIndex: 1,
                    counts: [.exam: 2, .solvedList: 1, .summary: 1, .formulaSheet: 1]
                ),
                MaterialsDiscipline(
                    id: "d3", code: "EXA810", name: "Linguagem de Programação Orientada a Objetos",
                    teacherName: "Rafael Almeida", colorIndex: 2,
                    counts: [.solvedList: 1, .summary: 1]
                ),
                MaterialsDiscipline(
                    id: "d4", code: "FIS200", name: "Física II",
                    teacherName: "João Nascimento", colorIndex: 3,
                    counts: [.exam: 1, .summary: 1, .formulaSheet: 1]
                ),
                MaterialsDiscipline(
                    id: "d5", code: "EXA801", name: "Estatística",
                    teacherName: "Lívia Gomes", colorIndex: 4,
                    counts: [:]
                ),
            ],
            savedCount: 3
        )
    }
}

extension MaterialsDisciplineDetails {
    static func preview() -> MaterialsDisciplineDetails {
        let discipline = MaterialsOverview.preview().disciplines[0]
        return MaterialsDisciplineDetails(discipline: discipline, materials: Material.preview())
    }

    static func previewEmpty() -> MaterialsDisciplineDetails {
        MaterialsDisciplineDetails(
            discipline: MaterialsOverview.preview().disciplines[4],
            materials: []
        )
    }
}

extension Material {
    static func preview() -> [Material] {
        let ref = MaterialDisciplineRef(
            id: "d1", code: "EXA805", name: "Algoritmos e Programação II", colorIndex: 0
        )
        return [
            Material(
                id: "a1", discipline: ref, type: .exam,
                title: "Prova 1 — Listas e pilhas", teacherName: "Camila Ribeiro",
                semester: "2025.2", pages: 4, fileKind: .pdf,
                usefulCount: 128, downloadCount: 412,
                uploader: MaterialUploader(course: "Ciência da Computação", entryYear: 2023),
                note: "Prova completa com o espelho de correção da professora nas duas últimas páginas.",
                isMine: false, status: .published, rejectionReason: nil,
                isUseful: false, isSaved: true
            ),
            Material(
                id: "a2", discipline: ref, type: .exam,
                title: "Prova 2 — Árvores e recursão", teacherName: "Camila Ribeiro",
                semester: "2025.1", pages: 3, fileKind: .pdf,
                usefulCount: 86, downloadCount: 274,
                uploader: MaterialUploader(course: "Eng. de Computação", entryYear: 2022),
                note: nil, isMine: false, status: .published, rejectionReason: nil,
                isUseful: false, isSaved: false
            ),
            Material(
                id: "a3", discipline: ref, type: .solvedList,
                title: "Lista 3 resolvida — pilhas e filas", teacherName: "Camila Ribeiro",
                semester: "2025.2", pages: 7, fileKind: .pdf,
                usefulCount: 64, downloadCount: 190,
                uploader: MaterialUploader(course: "Ciência da Computação", entryYear: 2024),
                note: "Resolução comentada de todos os 12 exercícios, feita em grupo de estudos.",
                isMine: false, status: .published, rejectionReason: nil,
                isUseful: true, isSaved: false
            ),
            Material(
                id: "a4", discipline: ref, type: .solvedList,
                title: "Lista 4 — árvores binárias", teacherName: nil,
                semester: "2025.2", pages: 5, fileKind: .photo,
                usefulCount: 22, downloadCount: 71,
                uploader: MaterialUploader(course: "Sistemas de Informação", entryYear: 2023),
                note: nil, isMine: false, status: .published, rejectionReason: nil,
                isUseful: false, isSaved: false
            ),
            Material(
                id: "a5", discipline: ref, type: .summary,
                title: "Resumão — estruturas lineares", teacherName: nil,
                semester: "2025.2", pages: 6, fileKind: .pdf,
                usefulCount: 173, downloadCount: 520,
                uploader: MaterialUploader(course: "Ciência da Computação", entryYear: 2022),
                note: "Mapa mental + tabela de complexidade Big-O de cada operação.",
                isMine: false, status: .published, rejectionReason: nil,
                isUseful: false, isSaved: true
            ),
            Material(
                id: "a6", discipline: ref, type: .formulaSheet,
                title: "Complexidade Big-O — cola", teacherName: nil,
                semester: "2024.2", pages: 1, fileKind: .photo,
                usefulCount: 95, downloadCount: 340,
                uploader: MaterialUploader(course: "Eng. de Computação", entryYear: 2021),
                note: nil, isMine: false, status: .published, rejectionReason: nil,
                isUseful: false, isSaved: false
            ),
            Material(
                id: "a7", discipline: ref, type: .solvedList,
                title: "Lista 5 resolvida — grafos", teacherName: "Camila Ribeiro",
                semester: "2025.2", pages: 8, fileKind: .pdf,
                usefulCount: 0, downloadCount: 0,
                uploader: MaterialUploader(course: "Ciência da Computação", entryYear: 2023),
                note: nil, isMine: true, status: .pending, rejectionReason: nil,
                isUseful: false, isSaved: false
            ),
            Material(
                id: "a8", discipline: ref, type: .exam,
                title: "Prova 1 — turma da manhã", teacherName: "Camila Ribeiro",
                semester: "2026.1", pages: 4, fileKind: .photo,
                usefulCount: 0, downloadCount: 0,
                uploader: MaterialUploader(course: "Ciência da Computação", entryYear: 2023),
                note: nil, isMine: true, status: .rejected,
                rejectionReason: "Provas do semestre atual (2026.1) não podem ser compartilhadas "
                    + "enquanto a avaliação estiver em andamento.",
                isUseful: false, isSaved: false
            ),
        ]
    }
}

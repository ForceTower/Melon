import Foundation

/// Everything the discipline detail screen renders, built from the mirrored
/// semester snapshot. Time-derived fields (grade countdowns, past lectures)
/// are classified at mapping time so the whole screen shares one clock.
struct DisciplineDetail: Equatable, Sendable {
    /// Discipline id — pairs with `semesterId` to address the mirror scope.
    var id: String
    var semesterId: String
    var code: String
    var name: String
    /// Owning department as sent upstream, e.g. "Departamento de Ciências
    /// Exatas" — display through `DisciplinesFormat.departmentLabel`.
    var department: String?
    /// Upstream "ementa" — the syllabus text.
    var ementa: String?
    var teacherName: String?
    var hours: Int
    /// Class-hours of absence, discipline-wide (shared across groups).
    var missedHours: Int
    /// Enrolled groups (theory first). More than one enables the group filter.
    var groups: [DisciplineDetailGroup] = []
    /// Grade sections after cross-group dedup: one merged section (nil
    /// `groupCode`, visible under every filter) when the groups share the
    /// discipline-level grade set — the common SAGRES shape — or one section
    /// per group when the sets genuinely differ.
    var sections: [DisciplineGradeSection] = []
    /// Ascending by date; placeholder rows (no subject, no materials) dropped.
    var lectures: [DisciplineLecture] = []
    /// Newest first.
    var attachments: [DisciplineAttachment] = []
    var finalGrade: Double?
    var approved: Bool?
    var wentToFinals: Bool = false
    var colorIndex: Int = 0
    /// When the mirror last applied a successful refresh.
    var syncedAt: Date?
}

struct DisciplineDetailGroup: Equatable, Sendable, Identifiable {
    /// Class id.
    var id: String
    /// SAGRES group name, e.g. "T01" / "T01P01".
    var code: String?
    /// Group kind, e.g. "Teórica" / "Prática".
    var kind: String?
    var teacherName: String?
}

struct DisciplineGradeSection: Equatable, Sendable, Identifiable {
    var id: String
    /// Kind label of the owning group; nil for the merged section.
    var name: String?
    /// Owning group code; nil when the section applies to every group.
    var groupCode: String?
    var grades: [DisciplineDetailGrade] = []
}

struct DisciplineDetailGrade: Equatable, Sendable, Identifiable {
    var id: String
    /// Compact evaluation label, e.g. "AV1".
    var label: String
    /// Full evaluation name, e.g. "Prova 1".
    var title: String
    var value: Double?
    var weight: Double?
    /// yyyy-MM-dd; nil when not scheduled.
    var date: String?
    /// Whole days until a scheduled, unreleased evaluation; nil once the
    /// date passed, the value landed, or nothing is scheduled.
    var daysUntil: Int?
}

struct DisciplineLecture: Equatable, Sendable, Identifiable {
    var id: String
    /// Owning group code, for the group filter.
    var groupCode: String?
    /// yyyy-MM-dd; nil when not yet scheduled.
    var date: String?
    var subject: String?
    var attachmentCount: Int = 0
    var isPast: Bool = false
}

struct DisciplineAttachment: Equatable, Sendable, Identifiable {
    var id: String
    var name: String
    var url: String
    /// Owning group code, for the group filter and the group chip.
    var groupCode: String?
    /// Date of the lecture the material was posted under (yyyy-MM-dd).
    var lectureDate: String?
}

// MARK: - Derived state

extension DisciplineDetail {
    var hasMultipleGroups: Bool { groups.count > 1 }

    var allowedMissedHours: Int { DisciplineRules.allowedMissedHours(ofTotal: hours) }

    var absenceRisk: AbsenceRisk {
        DisciplineRules.absenceRisk(missed: missedHours, allowed: allowedMissedHours)
    }

    var status: DisciplineStatus {
        DisciplineRules.status(
            approved: approved,
            wentToFinals: wentToFinals,
            finalGrade: finalGrade,
            partialAverage: Self.partialAverage(of: sections.flatMap(\.grades))
        )
    }

    /// Sections visible under a group filter; the merged section (nil
    /// `groupCode`) shows under every filter.
    func sections(forGroup code: String?) -> [DisciplineGradeSection] {
        guard let code else { return sections }
        return sections.filter { $0.groupCode == code || $0.groupCode == nil }
    }

    func grades(forGroup code: String?) -> [DisciplineDetailGrade] {
        sections(forGroup: code).flatMap(\.grades)
    }

    func lectures(forGroup code: String?) -> [DisciplineLecture] {
        guard let code else { return lectures }
        return lectures.filter { $0.groupCode == code || $0.groupCode == nil }
    }

    func attachments(forGroup code: String?) -> [DisciplineAttachment] {
        guard let code else { return attachments }
        return attachments.filter { $0.groupCode == code || $0.groupCode == nil }
    }

    /// True when upstream sent no usable weights or a single weight value —
    /// the "peso igual" caption on the grades card.
    var hasEqualWeights: Bool {
        let weights = sections.flatMap(\.grades).compactMap(\.weight)
        return Set(weights).count <= 1
    }

    static func partialAverage(of grades: [DisciplineDetailGrade]) -> Double? {
        DisciplineRules.partialAverage(
            of: grades.compactMap { grade in grade.value.map { ($0, grade.weight) } }
        )
    }

    /// The mean needed on each pending evaluation for the period to close at
    /// `target`, weight-aware. Nil when nothing was released yet or nothing
    /// is pending; clamped at 0 once the target is already secured. Values
    /// above 10 are unreachable — the caller decides how to say so.
    static func neededOnPending(of grades: [DisciplineDetailGrade], target: Double = 7) -> Double? {
        let released = grades.filter { $0.value != nil }
        let pending = grades.filter { $0.value == nil }
        guard !released.isEmpty, !pending.isEmpty else { return nil }

        let weights = grades.compactMap(\.weight)
        if weights.count == grades.count, weights.reduce(0, +) > 0 {
            let totalWeight = weights.reduce(0, +)
            let earned = released.reduce(0) { $0 + $1.value! * $1.weight! }
            let pendingWeight = pending.reduce(0) { $0 + $1.weight! }
            return max(0, (target * totalWeight - earned) / pendingWeight)
        }
        let earned = released.reduce(0) { $0 + $1.value! }
        return max(0, (target * Double(grades.count) - earned) / Double(pending.count))
    }
}

// MARK: - Preview data

extension DisciplineDetail {
    static func preview(now: Date = .now) -> DisciplineDetail {
        func stamp(daysFromNow: Int) -> String {
            Calendar.current.date(byAdding: .day, value: daysFromNow, to: now)!.dayStamp
        }
        return DisciplineDetail(
            id: "d1",
            semesterId: "sem-2026-1",
            code: "EXA805",
            name: "Algoritmos e Programação II",
            department: "Departamento de Ciências Exatas",
            ementa: """
            Estruturas de dados lineares e não-lineares: listas, pilhas, filas, árvores e grafos. \
            Análise de complexidade. Algoritmos de ordenação e busca. Implementação em C com ênfase \
            em gerenciamento manual de memória e modularização.
            """,
            teacherName: "Camila Ribeiro",
            hours: 60,
            missedHours: 2,
            groups: [
                DisciplineDetailGroup(id: "c1", code: "T01", kind: "Teórica", teacherName: "Camila Ribeiro"),
            ],
            sections: [
                DisciplineGradeSection(id: "merged", grades: [
                    DisciplineDetailGrade(
                        id: "g1", label: "AV1", title: "Prova 1", value: 8.3, weight: 1,
                        date: stamp(daysFromNow: -18)
                    ),
                    DisciplineDetailGrade(
                        id: "g2", label: "AV2", title: "Prova 2", value: nil, weight: 1,
                        date: stamp(daysFromNow: 24), daysUntil: 24
                    ),
                    DisciplineDetailGrade(
                        id: "g3", label: "AV3", title: "Projeto Final", value: nil, weight: 1,
                        date: stamp(daysFromNow: 80), daysUntil: 80
                    ),
                ]),
            ],
            lectures: [
                DisciplineLecture(
                    id: "l1", groupCode: "T01", date: stamp(daysFromNow: -35),
                    subject: "Apresentação da disciplina", attachmentCount: 1, isPast: true
                ),
                DisciplineLecture(
                    id: "l2", groupCode: "T01", date: stamp(daysFromNow: -28),
                    subject: "Revisão de ponteiros e structs", attachmentCount: 2, isPast: true
                ),
                DisciplineLecture(
                    id: "l3", groupCode: "T01", date: stamp(daysFromNow: -21),
                    subject: "Listas ligadas: inserção e remoção", attachmentCount: 3, isPast: true
                ),
                DisciplineLecture(
                    id: "l4", groupCode: "T01", date: stamp(daysFromNow: -14),
                    subject: "Pilhas — aplicações", isPast: true
                ),
                DisciplineLecture(
                    id: "l5", groupCode: "T01", date: stamp(daysFromNow: 2),
                    subject: "Filas e deques", attachmentCount: 2
                ),
                DisciplineLecture(
                    id: "l6", groupCode: "T01", date: stamp(daysFromNow: 4),
                    subject: "Árvores binárias — introdução"
                ),
            ],
            attachments: [
                DisciplineAttachment(
                    id: "m1", name: "Lista de exercícios 03 — pilhas.pdf",
                    url: "https://example.com/lista-03.pdf", groupCode: "T01",
                    lectureDate: stamp(daysFromNow: -14)
                ),
                DisciplineAttachment(
                    id: "m2", name: "Slides — listas duplamente ligadas",
                    url: "https://example.com/slides-listas.pptx", groupCode: "T01",
                    lectureDate: stamp(daysFromNow: -21)
                ),
                DisciplineAttachment(
                    id: "m3", name: "Repo exemplos da aula",
                    url: "https://example.com/repo", groupCode: "T01",
                    lectureDate: stamp(daysFromNow: -28)
                ),
            ],
            colorIndex: 0,
            syncedAt: now.addingTimeInterval(-120)
        )
    }

    /// A theory + practice discipline with genuinely distinct grade sets —
    /// the FIS2 shape from the design.
    static func previewMultiGroup(now: Date = .now) -> DisciplineDetail {
        func stamp(daysFromNow: Int) -> String {
            Calendar.current.date(byAdding: .day, value: daysFromNow, to: now)!.dayStamp
        }
        return DisciplineDetail(
            id: "d3",
            semesterId: "sem-2026-1",
            code: "EXA412",
            name: "Física II",
            department: "Departamento de Ciências Exatas",
            ementa: """
            Termodinâmica, oscilações, ondas mecânicas e acústica. Leis da termodinâmica, máquinas \
            térmicas, entropia. Movimento harmônico simples e amortecido. Ondas em cordas e tubos.
            """,
            teacherName: "João Nascimento",
            hours: 75,
            missedHours: 4,
            groups: [
                DisciplineDetailGroup(id: "c31", code: "T01", kind: "Teórica", teacherName: "João Nascimento"),
                DisciplineDetailGroup(id: "c32", code: "T01P01", kind: "Prática", teacherName: "Beatriz Sampaio"),
            ],
            sections: [
                DisciplineGradeSection(id: "c31", name: "Teórica", groupCode: "T01", grades: [
                    DisciplineDetailGrade(
                        id: "g7", label: "AV1", title: "I Avaliação", value: 6.8, weight: 1,
                        date: stamp(daysFromNow: -11)
                    ),
                    DisciplineDetailGrade(
                        id: "g8", label: "AV2", title: "II Avaliação", value: nil, weight: 1,
                        date: stamp(daysFromNow: 31), daysUntil: 31
                    ),
                    DisciplineDetailGrade(
                        id: "g9", label: "AV3", title: "III Avaliação", value: nil, weight: 1,
                        date: stamp(daysFromNow: 87), daysUntil: 87
                    ),
                ]),
                DisciplineGradeSection(id: "c32", name: "Prática", groupCode: "T01P01", grades: [
                    DisciplineDetailGrade(id: "g10", label: "LAB", title: "Relatórios", value: 9.0, weight: 1),
                ]),
            ],
            attachments: [
                DisciplineAttachment(
                    id: "m4", name: "Slides — oscilações",
                    url: "https://example.com/oscilacoes.pptx", groupCode: "T01",
                    lectureDate: stamp(daysFromNow: -16)
                ),
                DisciplineAttachment(
                    id: "m5", name: "Roteiro lab 01",
                    url: "https://example.com/lab-01.pdf", groupCode: "T01P01",
                    lectureDate: stamp(daysFromNow: -33)
                ),
            ],
            colorIndex: 3,
            syncedAt: now.addingTimeInterval(-120)
        )
    }
}

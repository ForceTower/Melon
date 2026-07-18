import Foundation

/// Everything the Turmas screen renders: the semester containing today (with
/// full per-discipline detail), the downloaded past semesters, and the
/// semesters known upstream whose payload hasn't been pulled yet.
struct DisciplinesOverview: Equatable, Sendable {
    var current: SemesterDisciplines?
    var past: [SemesterDisciplines] = []
    var pending: [PendingSemester] = []

    static let empty = DisciplinesOverview()

    var isEmpty: Bool {
        current == nil && past.isEmpty && pending.isEmpty
    }

    /// The raw code ("20261") of a mirrored semester, current or past.
    func semesterCode(for id: String) -> String? {
        if current?.id == id { return current?.code }
        return past.first { $0.id == id }?.code
    }
}

struct SemesterDisciplines: Equatable, Sendable, Identifiable {
    var id: String
    /// Raw upstream code, e.g. "20261" — display through
    /// `DisciplinesFormat.semesterLabel`.
    var code: String
    var disciplines: [DisciplineSummary] = []
}

/// A semester the student is enrolled in upstream but whose payload was never
/// mirrored. Rendered as a "Baixar" card.
struct PendingSemester: Equatable, Sendable, Identifiable {
    var id: String
    var code: String
    var disciplineCount: Int?
}

/// One discipline card: every group (theory/practice) of the same offer
/// merged into a single entry, grades deduplicated across groups.
struct DisciplineSummary: Equatable, Sendable, Identifiable {
    var id: String
    /// Analytics identity when the discipline has a single offer; nil for
    /// split (theory/practical) disciplines, where no one offer represents
    /// the merged card.
    var offerId: String? = nil
    var code: String
    var name: String
    var teacherName: String?
    /// Catalog class-hours of the whole discipline.
    var hours: Int
    /// Class-hours of absence (SAGRES totalFaltas), discipline-wide.
    var missedHours: Int
    /// "Te · Pr" when enrolled in more than one group kind; nil otherwise.
    var groupsLabel: String?
    /// Deduplicated evaluations in upstream (ordinal) order.
    var grades: [DisciplineGrade] = []
    /// Weighted mean of the released grades; plain mean when upstream sent
    /// no usable weights. Nil until something is released.
    var partialAverage: Double?
    /// Closing mean posted with the result; nil while ongoing.
    var finalGrade: Double?
    /// Authoritative pass/fail from upstream; nil until posted.
    var approved: Bool?
    var wentToFinals: Bool = false
    /// The next unreleased evaluation with a scheduled future date.
    var nextEvaluation: UpcomingEvaluation?
    /// Index into `UNESColor.disciplinePalette` — same name-sorted assignment
    /// Home uses, so a discipline keeps its color across tabs.
    var colorIndex: Int = 0
}

struct DisciplineGrade: Equatable, Sendable, Identifiable {
    var id: String
    /// Compact evaluation label, e.g. "P2".
    var label: String
    /// Full evaluation name, e.g. "Avaliação II" — same derivation as the
    /// detail screen's rows.
    var name: String
    var value: Double?
    /// yyyy-MM-dd; nil when not scheduled.
    var date: String?
}

struct UpcomingEvaluation: Equatable, Sendable {
    var label: String
    /// Whole calendar days from the snapshot day; 0 = today.
    var daysUntil: Int
}

// MARK: - Derived state

enum DisciplineStatus: Equatable, Sendable {
    case approved
    case failed
    case finals
    case lowGrade
    case noGrades
    case ongoing
}

enum AbsenceRisk: Equatable, Sendable {
    case ok, warning, critical
}

extension DisciplineSummary {
    var allowedMissedHours: Int { DisciplineRules.allowedMissedHours(ofTotal: hours) }

    var absenceRisk: AbsenceRisk {
        DisciplineRules.absenceRisk(missed: missedHours, allowed: allowedMissedHours)
    }

    var status: DisciplineStatus {
        DisciplineRules.status(
            approved: approved,
            wentToFinals: wentToFinals,
            finalGrade: finalGrade,
            partialAverage: partialAverage
        )
    }

    /// Pass/fail for history rows; nil when upstream never posted a result.
    var passed: Bool? {
        if let approved { return approved }
        guard let finalGrade else { return nil }
        return wentToFinals ? finalGrade >= 5 : finalGrade >= 7
    }

    /// The card asks for attention when absences are piling up or the
    /// partial mean sits below a comfortable pass trajectory.
    var needsAttention: Bool {
        if absenceRisk != .ok { return true }
        if let partialAverage, partialAverage < 6 { return true }
        return false
    }

    var releasedCount: Int { grades.count { $0.value != nil } }
}

extension SemesterDisciplines {
    /// Plain mean of the per-discipline partial averages.
    var partialMean: Double? {
        let averages = disciplines.compactMap(\.partialAverage)
        guard !averages.isEmpty else { return nil }
        return averages.reduce(0, +) / Double(averages.count)
    }

    /// Plain mean of the posted final grades.
    var finalMean: Double? {
        let finals = disciplines.compactMap(\.finalGrade)
        guard !finals.isEmpty else { return nil }
        return finals.reduce(0, +) / Double(finals.count)
    }

    var approvedCount: Int { disciplines.count { $0.passed == true } }

    var attentionCount: Int { disciplines.count(where: \.needsAttention) }

    /// Movement between the two most recently released grades in the
    /// semester — the hero's delta chip. Nil under two released grades.
    var lastGradeDelta: Double? {
        let released = disciplines
            .flatMap(\.grades)
            .filter { $0.value != nil }
            .sorted { ($0.date ?? "", $0.id) < ($1.date ?? "", $1.id) }
            .compactMap(\.value)
        guard released.count >= 2 else { return nil }
        return released[released.count - 1] - released[released.count - 2]
    }
}

// MARK: - Preview data

extension DisciplinesOverview {
    static func preview(now: Date = .now) -> DisciplinesOverview {
        func stamp(daysFromNow: Int) -> String {
            Calendar.current.date(byAdding: .day, value: daysFromNow, to: now)!.dayStamp
        }
        return DisciplinesOverview(
            current: SemesterDisciplines(
                id: "sem-2026-1",
                code: "20261",
                disciplines: [
                    DisciplineSummary(
                        id: "d1", code: "EXA805", name: "Algoritmos e Programação II",
                        teacherName: "Camila Ribeiro", hours: 60, missedHours: 2,
                        grades: [
                            DisciplineGrade(id: "g1", label: "AV1", name: "Avaliação I", value: 8.3, date: stamp(daysFromNow: -18)),
                            DisciplineGrade(id: "g2", label: "AV2", name: "Avaliação II", value: nil, date: stamp(daysFromNow: 24)),
                            DisciplineGrade(id: "g3", label: "AV3", name: "Avaliação III", value: nil, date: stamp(daysFromNow: 80)),
                        ],
                        partialAverage: 8.3,
                        nextEvaluation: UpcomingEvaluation(label: "AV2", daysUntil: 24),
                        colorIndex: 0
                    ),
                    DisciplineSummary(
                        id: "d2", code: "EXA704", name: "Cálculo Diferencial II",
                        teacherName: "Adriana Matos", hours: 75, missedHours: 4,
                        grades: [
                            DisciplineGrade(id: "g4", label: "AV1", name: "Avaliação I", value: 7.2, date: stamp(daysFromNow: -15)),
                            DisciplineGrade(id: "g5", label: "AV2", name: "Avaliação II", value: nil, date: stamp(daysFromNow: 27)),
                            DisciplineGrade(id: "g6", label: "AV3", name: "Avaliação III", value: nil, date: stamp(daysFromNow: 83)),
                        ],
                        partialAverage: 7.2,
                        nextEvaluation: UpcomingEvaluation(label: "AV2", daysUntil: 27),
                        colorIndex: 1
                    ),
                    DisciplineSummary(
                        id: "d3", code: "EXA412", name: "Física II",
                        teacherName: "João Nascimento", hours: 75, missedHours: 4,
                        groupsLabel: "Te · Pr",
                        grades: [
                            DisciplineGrade(id: "g7", label: "AV1", name: "Avaliação I", value: 6.8, date: stamp(daysFromNow: -11)),
                            DisciplineGrade(id: "g8", label: "AV2", name: "Avaliação II", value: nil, date: stamp(daysFromNow: 31)),
                            DisciplineGrade(id: "g9", label: "LAB", name: "Laboratório", value: 9.0, date: nil),
                        ],
                        partialAverage: 7.9,
                        nextEvaluation: UpcomingEvaluation(label: "AV2", daysUntil: 31),
                        colorIndex: 3
                    ),
                    DisciplineSummary(
                        id: "d4", code: "EXA807", name: "Programação Orientada a Objetos",
                        teacherName: "Rafael Almeida", hours: 60, missedHours: 8,
                        grades: [
                            DisciplineGrade(id: "g10", label: "AV1", name: "Avaliação I", value: nil, date: nil),
                            DisciplineGrade(id: "g11", label: "AV2", name: "Avaliação II", value: nil, date: nil),
                        ],
                        colorIndex: 2
                    ),
                    DisciplineSummary(
                        id: "d5", code: "EXA902", name: "Estatística",
                        teacherName: "Laís Pinheiro", hours: 60, missedHours: 1,
                        grades: [
                            DisciplineGrade(id: "g12", label: "AV1", name: "Avaliação I", value: 9.1, date: stamp(daysFromNow: -17)),
                            DisciplineGrade(id: "g13", label: "AV2", name: "Avaliação II", value: nil, date: stamp(daysFromNow: 25)),
                        ],
                        partialAverage: 9.1,
                        nextEvaluation: UpcomingEvaluation(label: "AV2", daysUntil: 25),
                        colorIndex: 4
                    ),
                ]
            ),
            past: [
                SemesterDisciplines(
                    id: "sem-2025-2",
                    code: "20252",
                    disciplines: [
                        DisciplineSummary(
                            id: "p1", code: "EXA801", name: "Algoritmos e Programação I",
                            teacherName: "Camila Ribeiro", hours: 60, missedHours: 4,
                            partialAverage: 7.9, finalGrade: 7.9, approved: true, colorIndex: 0
                        ),
                        DisciplineSummary(
                            id: "p2", code: "TEC401", name: "Circuitos Digitais",
                            teacherName: "Paulo Serra", hours: 60, missedHours: 8,
                            partialAverage: 5.3, finalGrade: 5.9, approved: false,
                            wentToFinals: true, colorIndex: 1
                        ),
                        DisciplineSummary(
                            id: "p3", code: "LET502", name: "Libras: Noções Básicas",
                            teacherName: "Lidineia Barreiros", hours: 45, missedHours: 3,
                            partialAverage: 9.4, finalGrade: 9.4, approved: true, colorIndex: 2
                        ),
                    ]
                ),
            ],
            pending: [
                PendingSemester(id: "sem-2024-2", code: "20242", disciplineCount: 6),
                PendingSemester(id: "sem-2024-1", code: "20241", disciplineCount: 5),
            ]
        )
    }
}

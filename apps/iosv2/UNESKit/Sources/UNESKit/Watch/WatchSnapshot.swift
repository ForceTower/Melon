import Foundation

/// Everything the watch renders, pushed from the phone over
/// WatchConnectivity: the weekly schedule pattern (shared with the widgets),
/// the headline numbers, and the per-discipline grade/absence summaries for
/// drill-down. The phone is the only producer; the watch caches the latest
/// payload and renders it offline, so date-relative values (countdowns,
/// "em N dias") are always derived at render time, never baked in.
struct WatchSnapshot: Codable, Equatable, Sendable {
    var schedule = WidgetScheduleSnapshot()
    /// The cross-semester coefficient (CR); truncate for display.
    var coefficient: Double?
    /// Movement since the previous coefficient spark point.
    var coefficientDelta: Double?
    /// Presence guaranteed so far, semester-wide.
    var attendancePercent: Int?
    /// Class-hours of absence still allowed by the SAGRES 75% rule.
    var remainingAbsences: Int?
    var nextExam: Exam?
    /// Current-semester disciplines, name-sorted as everywhere else.
    var disciplines: [Discipline] = []
    var syncedAt: Date

    struct Exam: Codable, Equatable, Sendable {
        /// Evaluation name as posted upstream, e.g. "P2".
        var label: String
        var disciplineName: String
        /// yyyy-MM-dd.
        var date: String
        /// "HH:mm" — the class slot on the exam's weekday, when there is one.
        var time: String?
    }

    struct Discipline: Codable, Equatable, Sendable, Identifiable {
        var id: String
        var code: String
        var name: String
        var teacherName: String?
        /// Catalog class-hours of the whole discipline.
        var hours: Int
        /// Class-hours of absence, discipline-wide.
        var missedHours: Int
        /// Deduplicated evaluations in upstream (ordinal) order.
        var grades: [Grade] = []
        /// Weighted mean of the released grades; nil until something lands.
        var partialAverage: Double?
        /// Index into `UNESColor.disciplinePalette`.
        var colorIndex: Int
    }

    struct Grade: Codable, Equatable, Sendable, Identifiable {
        var id: String
        /// Compact evaluation label, e.g. "P2".
        var label: String
        var value: Double?
        /// yyyy-MM-dd; nil when not scheduled.
        var date: String?
    }
}

extension WatchSnapshot.Discipline {
    var allowedMissedHours: Int { DisciplineRules.allowedMissedHours(ofTotal: hours) }

    var absenceRisk: AbsenceRisk {
        DisciplineRules.absenceRisk(missed: missedHours, allowed: allowedMissedHours)
    }

    var releasedCount: Int { grades.count { $0.value != nil } }
}

// MARK: - Preview data

extension WatchSnapshot {
    static func preview(now: Date = .now) -> WatchSnapshot {
        func stamp(daysFromNow: Int) -> String {
            Calendar.current.date(byAdding: .day, value: daysFromNow, to: now)!.dayStamp
        }
        let weekday = Calendar.current.component(.weekday, from: now) - 1
        let nowMinute = Calendar.current.component(.hour, from: now) * 60
            + Calendar.current.component(.minute, from: now)
        return WatchSnapshot(
            schedule: WidgetScheduleSnapshot(
                semesterCode: "20261",
                sessions: [
                    .init(
                        classId: "c1", day: weekday, startMinute: 480, endMinute: 600,
                        code: "ALGI", title: "Algoritmos I", room: "LC-03",
                        teacherName: "Camila Ribeiro", colorIndex: 0, disciplineId: "d1"
                    ),
                    .init(
                        classId: "c2", day: weekday, startMinute: nowMinute + 39, endMinute: nowMinute + 139,
                        code: "CALC", title: "Cálculo II", room: "MT-14",
                        teacherName: "Adriana Matos", colorIndex: 1, disciplineId: "d2"
                    ),
                    .init(
                        classId: "c3", day: weekday, startMinute: 840, endMinute: 960,
                        code: "LPOO", title: "POO", room: "LC-01",
                        teacherName: "Rafael Almeida", colorIndex: 2, disciplineId: "d3"
                    ),
                    .init(
                        classId: "c4", day: weekday, startMinute: 980, endMinute: 1080,
                        code: "FIS2", title: "Física II", room: "PV-22",
                        teacherName: "João Nascimento", colorIndex: 3, disciplineId: "d4"
                    ),
                    .init(
                        classId: "c5", day: (weekday + 1) % 7, startMinute: 620, endMinute: 720,
                        code: "CALC", title: "Cálculo II", room: "MT-14",
                        teacherName: "Adriana Matos", colorIndex: 1, disciplineId: "d2"
                    ),
                    .init(
                        classId: "c6", day: (weekday + 1) % 7, startMinute: 940, endMinute: 1040,
                        code: "PROJ", title: "Projeto de Software", room: "LC-05",
                        teacherName: "Marcos Costa", colorIndex: 4, disciplineId: "d5"
                    ),
                ],
                topics: [
                    .init(classId: "c2", dayStamp: now.dayStamp, subject: "Integrais por partes"),
                ]
            ),
            coefficient: 8.5,
            coefficientDelta: 0.3,
            attendancePercent: 96,
            remainingAbsences: 2,
            nextExam: Exam(
                label: "P2", disciplineName: "Algoritmos I",
                date: stamp(daysFromNow: 5), time: "08:00"
            ),
            disciplines: [
                Discipline(
                    id: "d1", code: "ALGI", name: "Algoritmos I",
                    teacherName: "Camila Ribeiro", hours: 60, missedHours: 2,
                    grades: [
                        Grade(id: "g1", label: "P1", value: 9.2, date: stamp(daysFromNow: -23)),
                        Grade(id: "g2", label: "T1", value: 8.0, date: stamp(daysFromNow: -12)),
                        Grade(id: "g3", label: "P2", value: nil, date: stamp(daysFromNow: 5)),
                    ],
                    partialAverage: 8.8, colorIndex: 0
                ),
                Discipline(
                    id: "d2", code: "CALC", name: "Cálculo II",
                    teacherName: "Adriana Matos", hours: 75, missedHours: 1,
                    grades: [
                        Grade(id: "g4", label: "P1", value: 7.0, date: stamp(daysFromNow: -17)),
                        Grade(id: "g5", label: "L", value: 8.0, date: nil),
                        Grade(id: "g6", label: "P2", value: nil, date: stamp(daysFromNow: 12)),
                    ],
                    partialAverage: 7.5, colorIndex: 1
                ),
                Discipline(
                    id: "d3", code: "LPOO", name: "POO",
                    teacherName: "Rafael Almeida", hours: 60, missedHours: 0,
                    grades: [
                        Grade(id: "g7", label: "P1", value: 9.5, date: stamp(daysFromNow: -15)),
                        Grade(id: "g8", label: "Pj", value: 9.3, date: stamp(daysFromNow: -3)),
                    ],
                    partialAverage: 9.4, colorIndex: 2
                ),
                Discipline(
                    id: "d4", code: "FIS2", name: "Física II",
                    teacherName: "João Nascimento", hours: 75, missedHours: 8,
                    grades: [
                        Grade(id: "g9", label: "P1", value: nil, date: stamp(daysFromNow: 3)),
                    ],
                    partialAverage: nil, colorIndex: 3
                ),
                Discipline(
                    id: "d5", code: "PROJ", name: "Projeto de Software",
                    teacherName: "Marcos Costa", hours: 60, missedHours: 1,
                    grades: [
                        Grade(id: "g10", label: "E1", value: 8.0, date: stamp(daysFromNow: -30)),
                        Grade(id: "g11", label: "E2", value: 8.2, date: stamp(daysFromNow: -6)),
                        Grade(id: "g12", label: "F", value: nil, date: stamp(daysFromNow: 20)),
                    ],
                    partialAverage: 8.1, colorIndex: 4
                ),
            ],
            syncedAt: now
        )
    }
}


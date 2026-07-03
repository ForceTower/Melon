import Foundation

/// Everything the Home ("Hoje") screen renders, computed from the active
/// semester payload plus the first inbox page.
struct HomeOverview: Equatable, Sendable {
    var semesterId: String?
    var semesterCode: String?
    var hero: HomeHeroClass?
    var coefficient: CoefficientSummary?
    var attendance: AttendanceSummary?
    var nextExam: ExamSummary?
    var messages: MessagesSummary?
    var today: [TodayClass] = []
    var disciplines: [DisciplineCard] = []

    static let empty = HomeOverview()
}

/// A Home snapshot served from the local mirror, stamped with the moment of
/// the refresh that produced it.
struct CachedHomeOverview: Equatable, Sendable {
    var overview: HomeOverview
    var syncedAt: Date
}

/// The class the hero spotlights — the next occurrence in the week, or the
/// one running right now. A running class holds the hero until halfway
/// through (the day's last class until it ends), then the next one takes over.
struct HomeHeroClass: Equatable, Sendable {
    var disciplineId: String?
    var disciplineName: String
    var startsAt: Date
    var endsAt: Date?
    /// "HH:mm" strings as encoded upstream.
    var startTime: String
    var endTime: String?
    /// Subject of the lecture planned for that date, when posted.
    var topic: String?
    var room: String?
    var teacherName: String?
    /// The class is running — render the "Agora" treatment instead of the
    /// start countdown.
    var isInProgress = false
}

struct CoefficientSummary: Equatable, Sendable {
    /// The coefficient (CR): mean of every discipline taken across the
    /// mirrored semesters, weighted by class-hours, abandonments counting as
    /// 0. Until a first result closes it falls back to the plain mean of the
    /// posted grades. Truncate (never round) to one decimal for display.
    var value: Double
    /// The CR as it stood after each semester, oldest first — the sparkline.
    /// In fallback mode, the posted grades in evaluation order.
    var spark: [Double]
    /// Movement since the previous spark point; nil under two points.
    var delta: Double?
}

struct AttendanceSummary: Equatable, Sendable {
    /// Presence guaranteed so far: `(1 - missed / total hours) × 100`.
    var percent: Int
    /// Class-hours of absence still allowed by the SAGRES 75% rule,
    /// semester-wide.
    var remainingAbsences: Int
}

struct ExamSummary: Equatable, Sendable {
    /// Evaluation name as posted upstream, e.g. "P2".
    var label: String
    var disciplineName: String
    /// yyyy-MM-dd.
    var date: String
    /// "HH:mm" — the class slot on the exam's weekday, when there is one.
    var time: String?
    /// Whole calendar days from the snapshot day to the exam day.
    var daysUntil: Int
}

struct MessagesSummary: Equatable, Sendable {
    var unreadCount: Int
    var latestSenderName: String?
    var latestPreview: String?
}

/// One class session in the "Seu dia" list. Contiguous upstream slots of the
/// same class are merged into a single session.
struct TodayClass: Equatable, Sendable, Identifiable {
    var id: String
    var classId: String
    var disciplineId: String
    /// Minutes into the day.
    var startMinute: Int
    var endMinute: Int?
    /// "HH:mm".
    var startTime: String
    var code: String
    var title: String
    var room: String?
    var topic: String?
    var colorIndex: Int
}

struct DisciplineCard: Equatable, Sendable, Identifiable {
    var id: String
    var code: String
    var name: String
    /// Plain mean of posted grades; truncate for display.
    var partial: Double?
    var colorIndex: Int
}

/// UFF-style grade display: truncated to one decimal (6,95 → 6,9 — never
/// rounded up), with the current locale's decimal separator (pt-BR "6,9",
/// en "6.9"). `nil` renders as an em dash.
func formatGrade(_ value: Double?, locale: Locale = .autoupdatingCurrent) -> String {
    guard let value else { return "—" }
    let truncated = (value * 10).rounded(.down) / 10
    return truncated.formatted(.number.precision(.fractionLength(1)).locale(locale))
}

extension HomeOverview {
    static func preview(now: Date = .now) -> HomeOverview {
        HomeOverview(
            semesterId: "sem1",
            semesterCode: "2026.1",
            hero: HomeHeroClass(
                disciplineId: "d2",
                disciplineName: "Cálculo II",
                startsAt: now.addingTimeInterval(39 * 60),
                endsAt: now.addingTimeInterval(139 * 60),
                startTime: "10:20",
                endTime: "12:00",
                topic: "Integrais por partes",
                room: "MT-14",
                teacherName: "Adriana Matos"
            ),
            coefficient: CoefficientSummary(value: 8.5, spark: [7.9, 8.0, 7.7, 8.2, 8.1, 8.5], delta: 0.4),
            attendance: AttendanceSummary(percent: 96, remainingAbsences: 2),
            nextExam: ExamSummary(
                label: "P2",
                disciplineName: "Algoritmos I",
                date: "2026-04-22",
                time: "08:00",
                daysUntil: 5
            ),
            messages: MessagesSummary(
                unreadCount: 2,
                latestSenderName: "Profa. Adriana",
                latestPreview: "Gabarito da P1 já está disponível no mural da turma."
            ),
            today: [
                TodayClass(
                    id: "a1", classId: "c1", disciplineId: "d1", startMinute: 480, endMinute: 600,
                    startTime: "08:00", code: "ALGI", title: "Algoritmos I", room: "LC-03", colorIndex: 0
                ),
                TodayClass(
                    id: "a2", classId: "c2", disciplineId: "d2", startMinute: 620, endMinute: 720,
                    startTime: "10:20", code: "CALC", title: "Cálculo II", room: "MT-14",
                    topic: "Integrais por partes", colorIndex: 1
                ),
                TodayClass(
                    id: "a3", classId: "c3", disciplineId: "d3", startMinute: 840, endMinute: 960,
                    startTime: "14:00", code: "LPOO", title: "POO", room: "LC-01",
                    topic: "Herança vs composição", colorIndex: 2
                ),
                TodayClass(
                    id: "a4", classId: "c4", disciplineId: "d4", startMinute: 980, endMinute: 1080,
                    startTime: "16:20", code: "FIS2", title: "Física II", room: "PV-22", colorIndex: 3
                ),
            ],
            disciplines: [
                DisciplineCard(id: "d1", code: "ALGI", name: "Algoritmos I", partial: 8.8, colorIndex: 0),
                DisciplineCard(id: "d2", code: "CALC", name: "Cálculo II", partial: 7.5, colorIndex: 1),
                DisciplineCard(id: "d3", code: "LPOO", name: "POO", partial: 9.4, colorIndex: 2),
                DisciplineCard(id: "d4", code: "FIS2", name: "Física II", partial: nil, colorIndex: 3),
                DisciplineCard(id: "d5", code: "PROJ", name: "Projeto de Software", partial: 8.1, colorIndex: 4),
            ]
        )
    }
}

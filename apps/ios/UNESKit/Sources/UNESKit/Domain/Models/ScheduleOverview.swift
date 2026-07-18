import Foundation

/// Everything the Horário screen renders: the calendar week containing the
/// refresh moment, one entry per day, with each day's merged class sessions.
struct ScheduleOverview: Equatable, Sendable {
    var semesterId: String?
    var semesterCode: String?
    /// ISO-8601 week-of-year of `days`, for the "Semana N" eyebrow.
    var weekOfYear = 0
    /// Monday-first, 7 entries.
    var days: [ScheduleDay] = []

    static let empty = ScheduleOverview()

    /// Index into `days` of the day containing `now`; nil once the mirrored
    /// week has gone stale.
    func todayIndex(now: Date) -> Int? {
        days.firstIndex { $0.dayStamp == now.dayStamp }
    }
}

struct ScheduleDay: Equatable, Sendable, Identifiable {
    /// yyyy-MM-dd — the day's identity.
    var dayStamp: String
    /// Day of month, for the week strip.
    var dayNumber: Int
    var classes: [ScheduleClass] = []

    var id: String { dayStamp }
}

/// One merged session on the weekly timetable.
struct ScheduleClass: Equatable, Sendable, Identifiable {
    var id: String
    var classId: String
    var disciplineId: String
    /// Analytics identity for the discipline tap (cross-platform itemId).
    var offerId: String? = nil
    var code: String
    var title: String
    /// Minutes into the day.
    var startMinute: Int
    var endMinute: Int?
    var teacherName: String?
    /// Subject of the lecture posted for this week's date, when filled in.
    var topic: String?
    /// Building/module label — a short code ("MT") or descriptive prose
    /// ("Pavilhão de aula padrão 2° andar").
    var modulo: String?
    /// Room number/code within the module.
    var room: String?
    var campus: String?
    var colorIndex: Int
}

/// Where a session stands relative to now, on the selected day.
enum ScheduleClassState: Equatable, Sendable {
    case done, now, next, later, future
}

extension ScheduleClass {
    var durationMinutes: Int? { endMinute.map { $0 - startMinute } }

    var isOnline: Bool {
        modulo == nil && room == nil
            && campus?.caseInsensitiveCompare("online") == .orderedSame
    }

    func state(isToday: Bool, nowMinutes: Int) -> ScheduleClassState {
        guard isToday else { return .future }
        if nowMinutes >= (endMinute ?? startMinute) { return .done }
        if nowMinutes >= startMinute { return .now }
        if startMinute - nowMinutes < 60 { return .next }
        return .later
    }
}

enum ScheduleWeek {
    /// Monday-first dates of the calendar week containing `now`, regardless
    /// of the device's first-weekday setting.
    static func dates(containing now: Date, calendar: Calendar = .current) -> [Date] {
        let start = calendar.startOfDay(for: now)
        let offsetFromMonday = (calendar.component(.weekday, from: now) + 5) % 7
        guard let monday = calendar.date(byAdding: .day, value: -offsetFromMonday, to: start) else { return [] }
        return (0..<7).compactMap { calendar.date(byAdding: .day, value: $0, to: monday) }
    }

    /// ISO-8601 week-of-year, matching printed academic calendars.
    static func weekOfYear(containing now: Date, calendar: Calendar = .current) -> Int {
        var iso = Calendar(identifier: .iso8601)
        iso.timeZone = calendar.timeZone
        return iso.component(.weekOfYear, from: now)
    }
}

// MARK: - Preview data

extension ScheduleOverview {
    static func preview(now: Date = .now) -> ScheduleOverview {
        let calendar = Calendar.current
        let dates = ScheduleWeek.dates(containing: now, calendar: calendar)

        let algi = { (id: String, start: Int, end: Int) in
            ScheduleClass(
                id: id, classId: "c1", disciplineId: "d1", code: "ALGI", title: "Algoritmos I",
                startMinute: start, endMinute: end, teacherName: "Camila Ribeiro",
                modulo: "MT", room: "LC-03", campus: "Feira", colorIndex: 0
            )
        }
        let week: [[ScheduleClass]] = [
            [
                algi("mon1", 7 * 60, 8 * 60 + 40),
                ScheduleClass(
                    id: "mon2", classId: "c2", disciplineId: "d2", code: "CALC", title: "Cálculo Diferencial II",
                    startMinute: 8 * 60 + 40, endMinute: 10 * 60 + 20, teacherName: "Adriana Matos",
                    modulo: "MT", room: "MT-14", campus: "Feira", colorIndex: 1
                ),
                ScheduleClass(
                    id: "mon3", classId: "c3", disciplineId: "d3", code: "LPOO", title: "Prog. Orientada a Objetos",
                    startMinute: 14 * 60, endMinute: 15 * 60 + 40, teacherName: "Rafael Almeida",
                    room: "LC-01", colorIndex: 2
                ),
            ],
            [
                ScheduleClass(
                    id: "tue1", classId: "c4", disciplineId: "d4", code: "FIS2", title: "Física II",
                    startMinute: 8 * 60 + 40, endMinute: 10 * 60 + 20, teacherName: "João Nascimento",
                    modulo: "PV", room: "PV-22", campus: "Feira", colorIndex: 3
                ),
                ScheduleClass(
                    id: "tue2", classId: "c2", disciplineId: "d2", code: "CALC", title: "Cálculo Diferencial II",
                    startMinute: 10 * 60 + 20, endMinute: 12 * 60, teacherName: "Adriana Matos",
                    modulo: "MT", room: "MT-14", campus: "Feira", colorIndex: 1
                ),
                ScheduleClass(
                    id: "tue3", classId: "c5", disciplineId: "d5", code: "EST", title: "Estatística",
                    startMinute: 15 * 60 + 40, endMinute: 17 * 60 + 20, teacherName: "Laís Pinheiro",
                    colorIndex: 4
                ),
            ],
            [
                algi("wed1", 7 * 60, 8 * 60 + 40),
                ScheduleClass(
                    id: "wed2", classId: "c3", disciplineId: "d3", code: "LPOO", title: "Prog. Orientada a Objetos",
                    startMinute: 10 * 60 + 20, endMinute: 12 * 60, teacherName: "Rafael Almeida",
                    modulo: "Pavilhão Central de aulas", room: "PC-12", campus: "Feira", colorIndex: 2
                ),
                ScheduleClass(
                    id: "wed3", classId: "c6", disciplineId: "d3", code: "LAB", title: "Laboratório de POO",
                    startMinute: 14 * 60, endMinute: 17 * 60 + 20, teacherName: "Rafael Almeida",
                    campus: "Online", colorIndex: 2
                ),
            ],
            [
                algi("thu1", 8 * 60, 9 * 60 + 40),
                ScheduleClass(
                    id: "thu2", classId: "c2", disciplineId: "d2", code: "CALC", title: "Cálculo Diferencial II",
                    startMinute: 10 * 60 + 20, endMinute: 12 * 60, teacherName: "Adriana Matos",
                    topic: "Integrais por partes",
                    modulo: "Pavilhão de aula padrão 2° andar", room: "PP-201", campus: "Feira", colorIndex: 1
                ),
                ScheduleClass(
                    id: "thu3", classId: "c3", disciplineId: "d3", code: "LPOO", title: "Prog. Orientada a Objetos",
                    startMinute: 14 * 60, endMinute: 15 * 60 + 40, teacherName: "Rafael Almeida",
                    modulo: "LC", room: "LC-01", campus: "Feira", colorIndex: 2
                ),
                ScheduleClass(
                    id: "thu4", classId: "c4", disciplineId: "d4", code: "FIS2", title: "Física II",
                    startMinute: 16 * 60 + 20, endMinute: 18 * 60, teacherName: "João Nascimento",
                    modulo: "PV", room: "PV-22", campus: "Feira", colorIndex: 3
                ),
            ],
            [
                ScheduleClass(
                    id: "fri1", classId: "c5", disciplineId: "d5", code: "EST", title: "Estatística",
                    startMinute: 8 * 60 + 40, endMinute: 10 * 60 + 20, teacherName: "Laís Pinheiro",
                    room: "MT-09", colorIndex: 4
                ),
                ScheduleClass(
                    id: "fri2", classId: "c7", disciplineId: "d4", code: "FIS2", title: "Física II (Prática)",
                    startMinute: 10 * 60 + 20, endMinute: 12 * 60, teacherName: "João Nascimento",
                    modulo: "PV", campus: "Feira", colorIndex: 3
                ),
            ],
            [],
            [],
        ]

        return ScheduleOverview(
            semesterId: "sem-2026-1",
            semesterCode: "20261",
            weekOfYear: ScheduleWeek.weekOfYear(containing: now, calendar: calendar),
            days: zip(dates, week).map { date, classes in
                ScheduleDay(
                    dayStamp: date.dayStamp,
                    dayNumber: calendar.component(.day, from: date),
                    classes: classes
                )
            }
        )
    }
}

import SwiftUI

struct ScheduleClass: Identifiable, Hashable {
    let id = UUID()
    let start: String   // "HH:mm"
    let end: String
    let code: String
    let title: String
    let prof: String
    let color: Color
    let modulo: String?
    let room: String?
    let campus: String?
    let topic: String?

    var startMin: Int { ScheduleTime.toMin(start) }
    var endMin: Int { ScheduleTime.toMin(end) }
    var durationMin: Int { endMin - startMin }

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: ScheduleClass, rhs: ScheduleClass) -> Bool { lhs.id == rhs.id }
}

enum ScheduleClassState { case done, now, next, later, future }

struct ScheduleSlot: Hashable {
    let start: String
    let end: String
}

enum ScheduleTime {
    static func toMin(_ t: String) -> Int {
        let parts = t.split(separator: ":").compactMap { Int($0) }
        guard parts.count == 2 else { return 0 }
        return parts[0] * 60 + parts[1]
    }
}

enum ScheduleFixtures {
    static let daysShort: [String] = ["seg", "ter", "qua", "qui", "sex", "sáb", "dom"]
    static let daysLong:  [String] = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"]
    static let dates: [Int] = [14, 15, 16, 17, 18, 19, 20]
    static let todayIdx: Int = 3 // quinta
    static let nowMin: Int = 10 * 60 + 52
    static let weekNumber: Int = 16

    static let week: [[ScheduleClass]] = [
        // Segunda
        [
            .init(start: "07:00", end: "08:40", code: "ALGI", title: "Algoritmos I",
                  prof: "C. Ribeiro", color: ColorFor.coral,
                  modulo: "MT", room: "LC-03", campus: "Feira", topic: nil),
            .init(start: "08:40", end: "10:20", code: "CALC", title: "Cálculo Diferencial II",
                  prof: "A. Matos", color: ColorFor.teal,
                  modulo: "MT", room: "MT-14", campus: "Feira", topic: nil),
            .init(start: "14:00", end: "15:40", code: "LPOO", title: "Prog. Orientada a Objetos",
                  prof: "R. Almeida", color: ColorFor.magenta,
                  modulo: nil, room: "LC-01", campus: nil, topic: nil),
        ],
        // Terça
        [
            .init(start: "08:40", end: "10:20", code: "FIS2", title: "Física II",
                  prof: "J. Nascimento", color: ColorFor.plum,
                  modulo: "PV", room: "PV-22", campus: "Feira", topic: nil),
            .init(start: "10:20", end: "12:00", code: "CALC", title: "Cálculo Diferencial II",
                  prof: "A. Matos", color: ColorFor.teal,
                  modulo: "MT", room: "MT-14", campus: "Feira", topic: nil),
            .init(start: "15:40", end: "17:20", code: "EST", title: "Estatística",
                  prof: "L. Pinheiro", color: ColorFor.amber,
                  modulo: nil, room: nil, campus: nil, topic: nil),
        ],
        // Quarta
        [
            .init(start: "07:00", end: "08:40", code: "ALGI", title: "Algoritmos I",
                  prof: "C. Ribeiro", color: ColorFor.coral,
                  modulo: "MT", room: "LC-03", campus: "Feira", topic: nil),
            .init(start: "10:20", end: "12:00", code: "LPOO", title: "Prog. Orientada a Objetos",
                  prof: "R. Almeida", color: ColorFor.magenta,
                  modulo: "LC", room: "LC-01", campus: "Feira", topic: nil),
            .init(start: "14:00", end: "17:20", code: "LAB",  title: "Laboratório de POO",
                  prof: "R. Almeida", color: ColorFor.magenta,
                  modulo: nil, room: nil, campus: "Online", topic: nil),
        ],
        // Quinta (hoje)
        [
            .init(start: "08:00", end: "09:40", code: "ALGI", title: "Algoritmos I",
                  prof: "C. Ribeiro", color: ColorFor.coral,
                  modulo: "MT", room: "LC-03", campus: "Feira", topic: nil),
            .init(start: "10:20", end: "12:00", code: "CALC", title: "Cálculo Diferencial II",
                  prof: "A. Matos", color: ColorFor.teal,
                  modulo: "MT", room: "MT-14", campus: "Feira", topic: "Integrais por partes"),
            .init(start: "14:00", end: "15:40", code: "LPOO", title: "Prog. Orientada a Objetos",
                  prof: "R. Almeida", color: ColorFor.magenta,
                  modulo: "LC", room: "LC-01", campus: "Feira", topic: nil),
            .init(start: "16:20", end: "18:00", code: "FIS2", title: "Física II",
                  prof: "J. Nascimento", color: ColorFor.plum,
                  modulo: "PV", room: "PV-22", campus: "Feira", topic: nil),
        ],
        // Sexta
        [
            .init(start: "08:40", end: "10:20", code: "EST", title: "Estatística",
                  prof: "L. Pinheiro", color: ColorFor.amber,
                  modulo: nil, room: "MT-09", campus: nil, topic: nil),
            .init(start: "10:20", end: "12:00", code: "FIS2", title: "Física II (Prática)",
                  prof: "J. Nascimento", color: ColorFor.plum,
                  modulo: "PV", room: nil, campus: "Feira", topic: nil),
        ],
        // Sábado
        [],
        // Domingo
        [],
    ]

    /// Unique (start,end) pairs across the week, sorted by start time.
    /// Drives the rows of the matrix grid.
    static let slots: [ScheduleSlot] = {
        var seen = Set<String>()
        var out: [ScheduleSlot] = []
        for day in week {
            for c in day {
                let key = c.start + "|" + c.end
                if seen.insert(key).inserted {
                    out.append(ScheduleSlot(start: c.start, end: c.end))
                }
            }
        }
        return out.sorted { ScheduleTime.toMin($0.start) < ScheduleTime.toMin($1.start) }
    }()

    static func state(for cls: ScheduleClass, isToday: Bool) -> ScheduleClassState {
        guard isToday else { return .future }
        if nowMin >= cls.endMin { return .done }
        if nowMin >= cls.startMin { return .now }
        if cls.startMin - nowMin < 60 { return .next }
        return .later
    }
}

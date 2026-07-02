import Foundation

/// One entry of the academic calendar (`api/me/events`).
struct AcademicEvent: Equatable, Sendable, Identifiable {
    let id: String
    var summary: String
    /// yyyy-MM-dd.
    var start: String
    var end: String?
    var origin: Origin

    enum Origin: String, Equatable, Sendable {
        case manual = "MANUAL"
        case evaluation = "EVALUATION"
        case finalExam = "FINAL_EXAM"
        case secondCall = "SECOND_CALL"
        case secondEpoch = "SECOND_EPOCH"
        case unknown
    }
}

extension [AcademicEvent] {
    static func preview(now: Date = .now) -> [AcademicEvent] {
        func stamp(daysFromNow: Int) -> String {
            Calendar.current.date(byAdding: .day, value: daysFromNow, to: now)!.dayStamp
        }
        return [
            AcademicEvent(id: "e1", summary: "Trancamento parcial", start: stamp(daysFromNow: 3), end: nil, origin: .manual),
            AcademicEvent(id: "e2", summary: "Prova 2 · Cálculo II", start: stamp(daysFromNow: 10), end: nil, origin: .evaluation),
            AcademicEvent(id: "e3", summary: "Semana de recesso", start: stamp(daysFromNow: 17), end: stamp(daysFromNow: 22), origin: .manual),
            AcademicEvent(id: "e4", summary: "Provas finais", start: stamp(daysFromNow: 28), end: nil, origin: .finalExam),
            AcademicEvent(id: "e5", summary: "Segunda chamada", start: stamp(daysFromNow: 31), end: nil, origin: .secondCall),
        ]
    }
}

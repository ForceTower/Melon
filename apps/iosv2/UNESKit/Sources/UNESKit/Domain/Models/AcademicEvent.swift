import Foundation

/// One entry of the academic calendar (`api/me/events`).
struct AcademicEvent: Equatable, Sendable, Identifiable {
    let id: String
    var summary: String
    /// yyyy-MM-dd.
    var start: String
    var end: String?
    /// Repeats every year (national holidays and the like).
    var fixed: Bool
    /// Campus-shut day — renders as a holiday regardless of origin.
    var closed: Bool
    var scope: Scope
    var origin: Origin

    enum Scope: String, Equatable, Sendable {
        case general = "GENERAL"
        case faculty = "FACULTY"
        case course = "COURSE"
        case classScope = "CLASS"
        case campus = "CAMPUS"
        case unknown
    }

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
        func event(
            _ id: String,
            _ summary: String,
            start: Int,
            end: Int? = nil,
            fixed: Bool = false,
            closed: Bool = false,
            scope: AcademicEvent.Scope = .general,
            origin: AcademicEvent.Origin
        ) -> AcademicEvent {
            AcademicEvent(
                id: id,
                summary: summary,
                start: stamp(daysFromNow: start),
                end: end.map { stamp(daysFromNow: $0) },
                fixed: fixed,
                closed: closed,
                scope: scope,
                origin: origin
            )
        }
        return [
            event("e0", "Feriado — Páscoa", start: -14, end: -12, fixed: true, closed: true, origin: .manual),
            event("e1", "Período para trancamento de disciplinas — Estudante", start: -4, end: 3, origin: .manual),
            event("e2", "P2 — Cálculo Diferencial II", start: 10, scope: .classScope, origin: .evaluation),
            event("e3", "Feriado — Tiradentes", start: 12, fixed: true, closed: true, origin: .manual),
            event("e4", "Semana de aulas especiais — Engenharia", start: 17, end: 22, scope: .faculty, origin: .manual),
            event("e5", "Período de Demanda para a Matrícula Web — Estudante", start: 24, end: 28, origin: .manual),
            event("e6", "Período de prova final", start: 38, end: 42, origin: .finalExam),
            event("e7", "Segunda chamada", start: 45, scope: .classScope, origin: .secondCall),
            event("e8", "Encerramento do semestre", start: 52, origin: .manual),
        ]
    }
}

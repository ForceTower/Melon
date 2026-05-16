import Foundation

/// Seed data mirroring `CAL_EVENTS` in `screens-calendar-data.jsx`. The set
/// straddles `CalendarMath.today` (Apr 17 2026) so the screen always has past,
/// active and future entries to exercise every status branch.
enum CalendarFixtures {
    static let semesterLabel = "2026.1"

    static let events: [CalendarEvent] = [
        // ── February
        ev("e-feb-1", "Período para aprovação e homologação do PIT",
           start: "2026-02-25", end: "2026-04-08",
           scope: .general, origin: .manual),

        // ── March
        ev("e-mar-1", "Período para trancamento de disciplinas (2026.1) — Estudante",
           start: "2026-03-30", end: "2026-04-15",
           scope: .general, origin: .manual),

        // ── April
        ev("e-apr-1", "Feriado — Páscoa",
           start: "2026-04-03", end: "2026-04-05",
           fixed: true, closed: true,
           scope: .general, origin: .manual),
        ev("e-apr-2", "P1 — Cálculo Diferencial II",
           start: "2026-04-09",
           scope: .classScope, origin: .evaluation),
        ev("e-apr-3", "Período para quebra de pré requisito (2026.2) — Estudante",
           start: "2026-04-13", end: "2026-04-20",
           scope: .general, origin: .manual),
        ev("e-apr-4", "P2 — Algoritmos I",
           start: "2026-04-22",
           scope: .classScope, origin: .evaluation),
        ev("e-apr-5", "Feriado — Tiradentes",
           start: "2026-04-21",
           fixed: true, closed: true,
           scope: .general, origin: .manual),
        ev("e-apr-6", "Período de Demanda para a Matrícula Web 2026.2 — Estudante",
           start: "2026-04-27", end: "2026-05-01",
           scope: .general, origin: .manual),

        // ── May
        ev("e-may-1", "Feriado — Dia do Trabalho",
           start: "2026-05-01",
           fixed: true, closed: true,
           scope: .general, origin: .manual),
        ev("e-may-2", "Matrícula Web 2026.2 — Ajuste",
           start: "2026-05-04", end: "2026-05-06",
           scope: .general, origin: .manual),
        ev("e-may-3", "Semana de aulas especiais — Engenharia",
           start: "2026-05-11", end: "2026-05-15",
           scope: .faculty, origin: .manual),
        ev("e-may-4", "P2 — Física II",
           start: "2026-05-18",
           scope: .classScope, origin: .evaluation),
        ev("e-may-5", "Prova final — Cálculo Diferencial II",
           start: "2026-05-28",
           scope: .classScope, origin: .finalExam),

        // ── June
        ev("e-jun-1", "Feriado — Corpus Christi",
           start: "2026-06-04",
           fixed: true, closed: true,
           scope: .general, origin: .manual),
        ev("e-jun-2", "Período de prova final — 2026.1",
           start: "2026-06-08", end: "2026-06-12",
           scope: .general, origin: .finalExam),
        ev("e-jun-3", "Encerramento do semestre 2026.1",
           start: "2026-06-19",
           scope: .general, origin: .manual),
    ]

    private static func ev(
        _ id: String,
        _ description: String,
        start: String,
        end: String? = nil,
        fixed: Bool = false,
        closed: Bool = false,
        scope: CalendarScope,
        origin: CalendarOrigin
    ) -> CalendarEvent {
        CalendarEvent(
            id: id,
            description: description,
            start: parseISODate(start),
            end: end.map(parseISODate),
            fixed: fixed,
            closed: closed,
            scope: scope,
            origin: origin
        )
    }

    /// `YYYY-MM-DD` → local Date at midnight, matching `parseCalDate` in the JSX.
    private static func parseISODate(_ s: String) -> Date {
        let parts = s.split(separator: "-").compactMap { Int($0) }
        var c = DateComponents()
        c.year = parts[0]
        c.month = parts[1]
        c.day = parts[2]
        return Calendar.current.date(from: c)!
    }
}

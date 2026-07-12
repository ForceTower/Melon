package dev.forcetower.unes.ui.feature.calendar

import java.time.LocalDate

// Seed data mirroring `CAL_EVENTS` in `screens-calendar-data.jsx`. Used only
// by `@Preview`s; the live screen always pulls from the KMP flow.
internal object CalendarFixtures {
    val events: List<CalendarEvent> = listOf(
        ev(
            "e-feb-1", "Período para aprovação e homologação do PIT",
            start = "2026-02-25", end = "2026-04-08",
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-mar-1", "Período para trancamento de disciplinas (2026.1) — Estudante",
            start = "2026-03-30", end = "2026-04-15",
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-apr-1", "Feriado — Páscoa",
            start = "2026-04-03", end = "2026-04-05",
            fixed = true, closed = true,
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-apr-2", "P1 — Cálculo Diferencial II",
            start = "2026-04-09",
            scope = CalendarScope.ClassScope, origin = CalendarOrigin.Evaluation,
        ),
        ev(
            "e-apr-3", "Período para quebra de pré requisito (2026.2) — Estudante",
            start = "2026-04-13", end = "2026-04-20",
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-apr-4", "P2 — Algoritmos I",
            start = "2026-04-22",
            scope = CalendarScope.ClassScope, origin = CalendarOrigin.Evaluation,
        ),
        ev(
            "e-apr-5", "Feriado — Tiradentes",
            start = "2026-04-21",
            fixed = true, closed = true,
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-apr-6", "Período de Demanda para a Matrícula Web 2026.2 — Estudante",
            start = "2026-04-27", end = "2026-05-01",
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-may-1", "Feriado — Dia do Trabalho",
            start = "2026-05-01",
            fixed = true, closed = true,
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-may-2", "Matrícula Web 2026.2 — Ajuste",
            start = "2026-05-04", end = "2026-05-06",
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-may-3", "Semana de aulas especiais — Engenharia",
            start = "2026-05-11", end = "2026-05-15",
            scope = CalendarScope.Faculty, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-may-4", "P2 — Física II",
            start = "2026-05-18",
            scope = CalendarScope.ClassScope, origin = CalendarOrigin.Evaluation,
        ),
        ev(
            "e-may-5", "Prova final — Cálculo Diferencial II",
            start = "2026-05-28",
            scope = CalendarScope.ClassScope, origin = CalendarOrigin.FinalExam,
        ),
        ev(
            "e-jun-1", "Feriado — Corpus Christi",
            start = "2026-06-04",
            fixed = true, closed = true,
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
        ev(
            "e-jun-2", "Período de prova final — 2026.1",
            start = "2026-06-08", end = "2026-06-12",
            scope = CalendarScope.General, origin = CalendarOrigin.FinalExam,
        ),
        ev(
            "e-jun-3", "Encerramento do semestre 2026.1",
            start = "2026-06-19",
            scope = CalendarScope.General, origin = CalendarOrigin.Manual,
        ),
    )

    private fun ev(
        id: String,
        description: String,
        start: String,
        end: String? = null,
        fixed: Boolean = false,
        closed: Boolean = false,
        scope: CalendarScope,
        origin: CalendarOrigin,
    ) = CalendarEvent(
        id = id,
        description = description,
        start = LocalDate.parse(start),
        end = end?.let(LocalDate::parse),
        fixed = fixed,
        closed = closed,
        scope = scope,
        origin = origin,
    )
}

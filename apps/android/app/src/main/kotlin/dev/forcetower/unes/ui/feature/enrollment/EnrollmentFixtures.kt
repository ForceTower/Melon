package dev.forcetower.unes.ui.feature.enrollment

import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentDiscipline
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentMeeting
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentPrerequisite
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentShift
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSlot
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindow
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState

// Preview-only sample catalogue mirroring `EnrollmentFixtures.swift` / the dc
// `matricula-data.js` set: a suggested discipline with two sections, a
// theory+practice bundle, a full section exercising the waitlist path, an
// "a definir" section with no slots, and an optativa with an unmet prereq.
internal object EnrollmentFixtures {

    val window = EnrollmentWindow(
        semester = "2026.2",
        state = EnrollmentWindowState.Open,
        startDate = "2026-06-15T08:00-03:00",
        endDate = "2026-06-22T23:59-03:00",
        minHours = 240,
        maxHours = 420,
        useQueue = true,
        courseId = 42,
    )

    private fun slot(day: Int, start: String, end: String) =
        EnrollmentSlot(day = day, start = start, end = end)

    val disciplines = listOf(
        EnrollmentDiscipline(
            id = 201, code = "EXA427", name = "Estruturas de Dados",
            workload = 60, mandatory = true, gradePeriod = 4, suggestion = true,
            prerequisites = listOf(
                EnrollmentPrerequisite(code = "EXA418", name = "Algoritmos e Programação II", met = true),
            ),
            sections = listOf(
                EnrollmentSection(
                    id = 30101, label = "T01", coursePreferential = true, suggestion = true,
                    vacancies = 50, proposalsCount = 31, allowsOtherDefault = true,
                    waitlistCount = 0, selected = true,
                    meetings = listOf(
                        EnrollmentMeeting(
                            kind = "Teórica", shift = EnrollmentShift.Afternoon,
                            professors = listOf("Matheus Andrade"), room = "PAT76 · UEFS",
                            slots = listOf(slot(1, "13:30", "15:30"), slot(3, "13:30", "15:30")),
                        ),
                    ),
                ),
                EnrollmentSection(
                    id = 30102, label = "T02", coursePreferential = false, suggestion = false,
                    vacancies = 50, proposalsCount = 47, allowsOtherDefault = true,
                    waitlistCount = 0, selected = false,
                    meetings = listOf(
                        EnrollmentMeeting(
                            kind = "Teórica", shift = EnrollmentShift.Night,
                            professors = listOf("Cláudia Ribeiro"), room = "MA09 · UEFS",
                            slots = listOf(slot(2, "18:50", "20:50"), slot(4, "18:50", "20:50")),
                        ),
                    ),
                ),
            ),
        ),
        EnrollmentDiscipline(
            id = 202, code = "TEC499", name = "Sistemas Digitais",
            workload = 60, mandatory = true, gradePeriod = 4, suggestion = true,
            prerequisites = emptyList(),
            sections = listOf(
                EnrollmentSection(
                    id = 30201, label = "T01P01", coursePreferential = true, suggestion = true,
                    vacancies = 40, proposalsCount = 22, allowsOtherDefault = true,
                    waitlistCount = 0, selected = false,
                    meetings = listOf(
                        EnrollmentMeeting(
                            kind = "Teórica", shift = EnrollmentShift.Afternoon,
                            professors = listOf("Roberto Sales"), room = "PAT54 · UEFS",
                            slots = listOf(slot(1, "13:30", "15:30")),
                        ),
                        EnrollmentMeeting(
                            kind = "Prática", shift = EnrollmentShift.Afternoon,
                            professors = listOf("Roberto Sales"), room = "Lab. Hardware · UEFS",
                            slots = listOf(slot(5, "15:30", "17:30")),
                        ),
                    ),
                ),
            ),
        ),
        EnrollmentDiscipline(
            id = 203, code = "EXA866", name = "Probabilidade e Estatística",
            workload = 60, mandatory = true, gradePeriod = 4, suggestion = true,
            prerequisites = emptyList(),
            sections = listOf(
                EnrollmentSection(
                    id = 30301, label = "T01", coursePreferential = true, suggestion = true,
                    vacancies = 45, proposalsCount = 45, allowsOtherDefault = true,
                    waitlistCount = 6, selected = true,
                    meetings = listOf(
                        EnrollmentMeeting(
                            kind = "Teórica", shift = EnrollmentShift.Morning,
                            professors = listOf("Sônia Vasconcelos"), room = "MA12 · UEFS",
                            slots = listOf(slot(2, "07:30", "09:30"), slot(4, "07:30", "09:30")),
                        ),
                    ),
                ),
            ),
        ),
        EnrollmentDiscipline(
            id = 205, code = "TEC505", name = "Banco de Dados",
            workload = 60, mandatory = true, gradePeriod = 4, suggestion = false,
            prerequisites = emptyList(),
            sections = listOf(
                EnrollmentSection(
                    id = 30502, label = "T02", coursePreferential = false, suggestion = false,
                    vacancies = 45, proposalsCount = 3, allowsOtherDefault = true,
                    waitlistCount = 0, selected = false,
                    meetings = listOf(
                        EnrollmentMeeting(
                            kind = "Teórica", shift = EnrollmentShift.Undefined,
                            professors = emptyList(), room = null, slots = emptyList(),
                        ),
                    ),
                ),
            ),
        ),
        EnrollmentDiscipline(
            id = 302, code = "TEC540", name = "Introdução à Inteligência Artificial",
            workload = 60, mandatory = false, gradePeriod = 0, suggestion = false,
            prerequisites = listOf(
                EnrollmentPrerequisite(code = "EXA427", name = "Estruturas de Dados", met = false),
            ),
            sections = listOf(
                EnrollmentSection(
                    id = 30801, label = "T01", coursePreferential = false, suggestion = false,
                    vacancies = 30, proposalsCount = 27, allowsOtherDefault = false,
                    waitlistCount = 0, selected = false,
                    meetings = listOf(
                        EnrollmentMeeting(
                            kind = "Teórica", shift = EnrollmentShift.Afternoon,
                            professors = listOf("Daniel Prado"), room = "LCC2 · UEFS",
                            slots = listOf(slot(2, "15:30", "17:30"), slot(4, "15:30", "17:30")),
                        ),
                    ),
                ),
            ),
        ),
    )

    val state = EnrollmentUiState(
        phase = EnrollmentPhase.Loaded,
        available = true,
        window = window,
        disciplines = disciplines,
        picks = preseedPicks(window, disciplines),
        studentName = "Mariana Alves",
        courseName = "Ciência da Computação",
        semesterOrdinal = 4,
        referenceNowMillis = 1_781_708_400_000L, // 2026-06-17T10:00-03:00 — inside the window
    )
}

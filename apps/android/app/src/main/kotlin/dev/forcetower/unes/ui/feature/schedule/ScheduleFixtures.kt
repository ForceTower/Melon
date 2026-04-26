package dev.forcetower.unes.ui.feature.schedule

import dev.forcetower.melon.feature.schedule.domain.model.ScheduleClass as KmpScheduleClass
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleDay as KmpScheduleDay
import dev.forcetower.melon.feature.schedule.domain.model.ScheduleWeek as KmpScheduleWeek

// Preview-only fixtures. The runtime screen reads from `ScheduleViewModel`;
// `@Preview` builds a `KmpScheduleWeek` and routes it through the same
// `mapWeek` the runtime path uses, so what designers see in the IDE matches
// what the device renders. Mirrors `unes/project/screens-schedule.jsx` and
// `apps/ios/UNES/Features/Schedule/Models/ScheduleModels.swift`.
internal object ScheduleFixtures {
    private const val TODAY_INDEX = 3 // Thursday (Mon-first indexing)
    private const val WEEK_NUMBER = 16
    private val DATE_ISOS = listOf(
        "2025-04-14", "2025-04-15", "2025-04-16", "2025-04-17",
        "2025-04-18", "2025-04-19", "2025-04-20",
    )

    fun kmpWeek(): KmpScheduleWeek = KmpScheduleWeek(
        semesterId = "preview",
        semesterCode = "2025.1",
        weekNumber = WEEK_NUMBER,
        weekStartIso = DATE_ISOS.first(),
        todayDayIndex = TODAY_INDEX,
        days = DATE_ISOS.mapIndexed { idx, iso ->
            KmpScheduleDay(dayIndex = idx, dateIso = iso, classes = classesFor(idx))
        },
    )

    private fun classesFor(dayIdx: Int): List<KmpScheduleClass> = when (dayIdx) {
        0 -> listOf(
            kmp("ALGI-MON-1", "ALGI", "Algoritmos I", "07:00", "08:40", "C. Ribeiro", "MT", "LC-03", "Feira"),
            kmp("CALC-MON-1", "CALC", "Cálculo Diferencial II", "08:40", "10:20", "A. Matos", "MT", "MT-14", "Feira"),
            kmp("LPOO-MON-1", "LPOO", "Prog. Orientada a Objetos", "14:00", "15:40", "R. Almeida", null, "LC-01", null),
        )
        1 -> listOf(
            kmp("FIS2-TUE-1", "FIS2", "Física II", "08:40", "10:20", "J. Nascimento", "PV", "PV-22", "Feira"),
            kmp("CALC-TUE-1", "CALC", "Cálculo Diferencial II", "10:20", "12:00", "A. Matos", "MT", "MT-14", "Feira"),
            kmp("EST-TUE-1", "EST", "Estatística", "15:40", "17:20", "L. Pinheiro", null, null, null),
        )
        2 -> listOf(
            kmp("ALGI-WED-1", "ALGI", "Algoritmos I", "07:00", "08:40", "C. Ribeiro", "MT", "LC-03", "Feira"),
            kmp("LPOO-WED-1", "LPOO", "Prog. Orientada a Objetos", "10:20", "12:00", "R. Almeida", "LC", "LC-01", "Feira"),
            kmp("LAB-WED-1", "LAB", "Laboratório de POO", "14:00", "17:20", "R. Almeida", null, null, "Online"),
        )
        3 -> listOf(
            kmp("ALGI-THU-1", "ALGI", "Algoritmos I", "08:00", "09:40", "C. Ribeiro", "MT", "LC-03", "Feira"),
            kmp(
                "CALC-THU-1", "CALC", "Cálculo Diferencial II", "10:20", "12:00", "A. Matos",
                "MT", "MT-14", "Feira", topic = "Integrais por partes",
            ),
            kmp("LPOO-THU-1", "LPOO", "Prog. Orientada a Objetos", "14:00", "15:40", "R. Almeida", "LC", "LC-01", "Feira"),
            kmp("FIS2-THU-1", "FIS2", "Física II", "16:20", "18:00", "J. Nascimento", "PV", "PV-22", "Feira"),
        )
        4 -> listOf(
            kmp("EST-FRI-1", "EST", "Estatística", "08:40", "10:20", "L. Pinheiro", null, "MT-09", null),
            kmp("FIS2-FRI-1", "FIS2", "Física II (Prática)", "10:20", "12:00", "J. Nascimento", "PV", null, "Feira"),
        )
        else -> emptyList()
    }

    private fun kmp(
        allocationId: String,
        code: String,
        title: String,
        start: String,
        end: String,
        teacher: String,
        modulo: String?,
        room: String?,
        campus: String?,
        topic: String? = null,
    ): KmpScheduleClass = KmpScheduleClass(
        allocationId = allocationId,
        classId = allocationId,
        offerId = allocationId,
        code = code,
        title = title,
        startTime = start,
        endTime = end,
        teacherName = teacher,
        modulo = modulo,
        room = room,
        campus = campus,
        topic = topic,
    )
}

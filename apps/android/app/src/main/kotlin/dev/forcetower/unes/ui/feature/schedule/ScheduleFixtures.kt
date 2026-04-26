package dev.forcetower.unes.ui.feature.schedule

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import dev.forcetower.unes.designsystem.theme.melon

// Static fixtures mirroring `unes/project/screens-schedule.jsx` and
// `apps/ios/UNES/Features/Schedule/Models/ScheduleModels.swift`.
internal object ScheduleFixtures {
    const val TODAY_INDEX = 3 // Thursday (Mon-first indexing)
    const val NOW_MIN = 10 * 60 + 52 // 10:52
    const val WEEK_NUMBER = 16

    val dates: List<Int> = listOf(14, 15, 16, 17, 18, 19, 20)

    @Composable
    @ReadOnlyComposable
    fun week(): List<List<ScheduleClass>> {
        val palette = MaterialTheme.melon.palette
        val coral = palette.coral
        val teal = palette.teal
        val magenta = palette.magenta
        val plum = palette.plum
        val amber = MaterialTheme.melon.brand.amber

        return listOf(
            // Monday
            listOf(
                ScheduleClass(
                    start = "07:00", end = "08:40",
                    code = "ALGI", title = "Algoritmos I",
                    prof = "C. Ribeiro", color = coral,
                    modulo = "MT", room = "LC-03", campus = "Feira", topic = null,
                ),
                ScheduleClass(
                    start = "08:40", end = "10:20",
                    code = "CALC", title = "Cálculo Diferencial II",
                    prof = "A. Matos", color = teal,
                    modulo = "MT", room = "MT-14", campus = "Feira", topic = null,
                ),
                ScheduleClass(
                    start = "14:00", end = "15:40",
                    code = "LPOO", title = "Prog. Orientada a Objetos",
                    prof = "R. Almeida", color = magenta,
                    modulo = null, room = "LC-01", campus = null, topic = null,
                ),
            ),
            // Tuesday
            listOf(
                ScheduleClass(
                    start = "08:40", end = "10:20",
                    code = "FIS2", title = "Física II",
                    prof = "J. Nascimento", color = plum,
                    modulo = "PV", room = "PV-22", campus = "Feira", topic = null,
                ),
                ScheduleClass(
                    start = "10:20", end = "12:00",
                    code = "CALC", title = "Cálculo Diferencial II",
                    prof = "A. Matos", color = teal,
                    modulo = "MT", room = "MT-14", campus = "Feira", topic = null,
                ),
                ScheduleClass(
                    start = "15:40", end = "17:20",
                    code = "EST", title = "Estatística",
                    prof = "L. Pinheiro", color = amber,
                    modulo = null, room = null, campus = null, topic = null,
                ),
            ),
            // Wednesday
            listOf(
                ScheduleClass(
                    start = "07:00", end = "08:40",
                    code = "ALGI", title = "Algoritmos I",
                    prof = "C. Ribeiro", color = coral,
                    modulo = "MT", room = "LC-03", campus = "Feira", topic = null,
                ),
                ScheduleClass(
                    start = "10:20", end = "12:00",
                    code = "LPOO", title = "Prog. Orientada a Objetos",
                    prof = "R. Almeida", color = magenta,
                    modulo = "LC", room = "LC-01", campus = "Feira", topic = null,
                ),
                ScheduleClass(
                    start = "14:00", end = "17:20",
                    code = "LAB", title = "Laboratório de POO",
                    prof = "R. Almeida", color = magenta,
                    modulo = null, room = null, campus = "Online", topic = null,
                ),
            ),
            // Thursday (today)
            listOf(
                ScheduleClass(
                    start = "08:00", end = "09:40",
                    code = "ALGI", title = "Algoritmos I",
                    prof = "C. Ribeiro", color = coral,
                    modulo = "MT", room = "LC-03", campus = "Feira", topic = null,
                ),
                ScheduleClass(
                    start = "10:20", end = "12:00",
                    code = "CALC", title = "Cálculo Diferencial II",
                    prof = "A. Matos", color = teal,
                    modulo = "MT", room = "MT-14", campus = "Feira",
                    topic = "Integrais por partes",
                ),
                ScheduleClass(
                    start = "14:00", end = "15:40",
                    code = "LPOO", title = "Prog. Orientada a Objetos",
                    prof = "R. Almeida", color = magenta,
                    modulo = "LC", room = "LC-01", campus = "Feira", topic = null,
                ),
                ScheduleClass(
                    start = "16:20", end = "18:00",
                    code = "FIS2", title = "Física II",
                    prof = "J. Nascimento", color = plum,
                    modulo = "PV", room = "PV-22", campus = "Feira", topic = null,
                ),
            ),
            // Friday
            listOf(
                ScheduleClass(
                    start = "08:40", end = "10:20",
                    code = "EST", title = "Estatística",
                    prof = "L. Pinheiro", color = amber,
                    modulo = null, room = "MT-09", campus = null, topic = null,
                ),
                ScheduleClass(
                    start = "10:20", end = "12:00",
                    code = "FIS2", title = "Física II (Prática)",
                    prof = "J. Nascimento", color = plum,
                    modulo = "PV", room = null, campus = "Feira", topic = null,
                ),
            ),
            emptyList(), // Saturday
            emptyList(), // Sunday
        )
    }
}

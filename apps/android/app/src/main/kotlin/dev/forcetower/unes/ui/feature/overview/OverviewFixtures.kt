package dev.forcetower.unes.ui.feature.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import androidx.compose.material3.MaterialTheme

// Static fixtures that mirror `unes/project/screens-home.jsx` and the iOS
// `OverviewFixtures` enum. The Connected shell renders these directly until
// the KMP-backed ViewModel ships — keeping the JSX prototype, iOS, and
// Android visually identical while the data layer catches up.

internal data class OverviewNowClass(
    val code: String,
    val title: String,
    val prof: String,
    val room: String,
    val startsInMinutes: Int,
    val timeRange: String,
    val topic: String?,
    val color: Color,
    val meshVariant: MeshVariant,
)

internal enum class OverviewClassState { Done, Now, Next, Later }

internal data class OverviewTodayItem(
    val time: String,
    val code: String,
    val title: String,
    val room: String,
    val color: Color,
    val state: OverviewClassState,
    val topic: String?,
)

internal data class OverviewDiscipline(
    val code: String,
    val title: String,
    val grade: String,
    val color: Color,
)

internal data class OverviewGradeTileData(
    val value: Double,
    val deltaLabel: String?,
    val comparisonSemester: String?,
)

internal data class OverviewMessagesTileData(
    val unreadCount: Int,
    val lastSender: String?,
    val lastPreview: String?,
)

internal data class OverviewNextTestTileData(
    val label: String,
    val disciplineName: String,
    val daysUntil: Int,
    val dateLabel: String,
)

internal data class OverviewAttendanceTileData(
    val percentage: Int,
    val days: List<Boolean>,
    val allowedAbsences: Int,
    val periodDays: Int,
)

internal object OverviewFixtures {
    const val SEMESTER_LABEL = "2026.1"
    const val LAST_UPDATED_MINUTES = 2
    const val DATE_EYEBROW = "quinta · 17 abr"

    @Composable
    @ReadOnlyComposable
    fun nowClass(): OverviewNowClass = OverviewNowClass(
        code = "CALC II",
        title = "Cálculo Diferencial II",
        prof = "Prof. Adriana Matos",
        room = "MT-14",
        startsInMinutes = 72,
        timeRange = "10:20 – 12:00",
        topic = "Integrais por partes — continuação do exercício 4.2",
        color = MaterialTheme.melon.palette.teal,
        meshVariant = MeshVariant.Cool,
    )

    @Composable
    @ReadOnlyComposable
    fun today(): List<OverviewTodayItem> {
        val palette = MaterialTheme.melon.palette
        return listOf(
            OverviewTodayItem(
                time = "08:00", code = "ALGI", title = "Algoritmos I",
                room = "LC-03", color = palette.coral,
                state = OverviewClassState.Done, topic = null,
            ),
            OverviewTodayItem(
                time = "10:20", code = "CALC", title = "Cálculo II",
                room = "MT-14", color = palette.teal,
                state = OverviewClassState.Now, topic = "Integrais por partes",
            ),
            OverviewTodayItem(
                time = "14:00", code = "LPOO", title = "Prog. Orientada a Obj.",
                room = "LC-01", color = palette.magenta,
                state = OverviewClassState.Next, topic = "Herança vs composição",
            ),
            OverviewTodayItem(
                time = "16:20", code = "FIS2", title = "Física II",
                room = "PV-22", color = palette.plum,
                state = OverviewClassState.Later, topic = null,
            ),
        )
    }

    @Composable
    @ReadOnlyComposable
    fun disciplines(): List<OverviewDiscipline> {
        val palette = MaterialTheme.melon.palette
        return listOf(
            OverviewDiscipline("ALGI", "Algoritmos I", "8,8", palette.coral),
            OverviewDiscipline("CALC", "Cálculo II", "7,5", palette.teal),
            OverviewDiscipline("LPOO", "POO", "9,4", palette.magenta),
            OverviewDiscipline("FIS2", "Física II", "—", palette.plum),
            OverviewDiscipline("PROJ", "Projeto de Software", "8,1", MaterialTheme.melon.brand.amber),
        )
    }

    val gradeTile = OverviewGradeTileData(
        value = 8.5,
        deltaLabel = "+0,3",
        comparisonSemester = "2025.2",
    )

    val messagesTile = OverviewMessagesTileData(
        unreadCount = 2,
        lastSender = "Prof. Adriana",
        lastPreview = "Gabarito da P1",
    )

    val nextTestTile = OverviewNextTestTileData(
        label = "P2",
        disciplineName = "Algoritmos I",
        daysUntil = 5,
        dateLabel = "22 abr · 08:00",
    )

    val attendanceTile = OverviewAttendanceTileData(
        percentage = 96,
        days = List(14) { it < 12 },
        allowedAbsences = 2,
        periodDays = 14,
    )
}

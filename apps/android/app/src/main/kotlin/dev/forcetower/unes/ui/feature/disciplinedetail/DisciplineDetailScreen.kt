package dev.forcetower.unes.ui.feature.disciplinedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DetailStatCard
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineAttachmentsBlock
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineClassesBlock
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineDetailHero
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineEmentaBlock
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineFaltasBlock
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineGradesBlock
import dev.forcetower.unes.ui.feature.disciplines.AbsenceRisk
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.absenceRisk

// Single-scroll discipline detail. Top to bottom: hero / stats row / grades /
// ementa / classes / attachments / presença. Mirrors iOS
// `DisciplineDetailView` — same section order, same fade-up reveal cadence.
@Composable
internal fun DisciplineDetailScreen(
    discipline: Discipline,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Ambient color wash carrying the discipline's tint across the top of
        // the screen — fades into the surface. iOS uses a RadialGradient with
        // a 320pt radius from the top-center; the linear approximation here
        // reads close enough on phones and avoids the per-pixel cost of a
        // real radial.
        AmbientWash(discipline = discipline)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item("hero") {
                DisciplineDetailHero(
                    discipline = discipline,
                    selectedGroup = selectedGroup,
                    onSelectGroup = onSelectGroup,
                    onBack = onBack,
                )
            }
            item("stats") {
                StatsRow(
                    discipline = discipline,
                    modifier = Modifier
                        .fadeUpOnAppear(delayMs = 100)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 22.dp),
                )
            }
            item("grades") {
                DisciplineGradesBlock(
                    discipline = discipline,
                    selectedGroup = selectedGroup,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 180),
                )
            }
            item("ementa") {
                DisciplineEmentaBlock(
                    discipline = discipline,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 240),
                )
            }
            item("classes") {
                DisciplineClassesBlock(
                    discipline = discipline,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 300),
                )
            }
            item("attachments") {
                DisciplineAttachmentsBlock(
                    discipline = discipline,
                    selectedGroup = selectedGroup,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 360),
                )
            }
            item("faltas") {
                DisciplineFaltasBlock(
                    discipline = discipline,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 420),
                )
            }
        }
    }
}

@Composable
private fun AmbientWash(discipline: Discipline) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        discipline.color.copy(alpha = 0.20f),
                        surface.copy(alpha = 0f),
                    ),
                    center = Offset(540f, -120f),
                    radius = 1100f,
                ),
            ),
    )
}

@Composable
private fun StatsRow(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val absenceColor = when (discipline.absenceRisk) {
        AbsenceRisk.Risk -> DisciplineScoreColor.danger()
        AbsenceRisk.Warn -> DisciplineScoreColor.caution()
        AbsenceRisk.Ok -> MaterialTheme.colorScheme.onBackground
    }
    val absenceIconTint = if (discipline.absenceRisk == AbsenceRisk.Ok) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        DisciplineScoreColor.caution()
    }
    val remaining = (discipline.allowedAbsences - discipline.absences).coerceAtLeast(0)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DetailStatCard(
            label = stringResource(R.string.discipline_detail_stat_hours),
            value = stringResource(R.string.disciplines_card_hours_format, discipline.hours),
            sub = stringResource(R.string.discipline_detail_stat_hours_sub),
            modifier = Modifier.weight(1f),
        ) {
            dev.forcetower.unes.ui.feature.disciplinedetail.components.ClockIcon(tint = discipline.color)
        }
        DetailStatCard(
            label = stringResource(R.string.discipline_detail_stat_absences),
            value = discipline.absences.toString(),
            sub = stringResource(R.string.discipline_detail_stat_absences_sub_format, remaining),
            valueColor = absenceColor,
            modifier = Modifier.weight(1f),
        ) {
            dev.forcetower.unes.ui.feature.disciplinedetail.components.AbsencesIcon(tint = absenceIconTint)
        }
    }
}

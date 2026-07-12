package dev.forcetower.unes.ui.feature.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentAppBar
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentBanner
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentBannerTone
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentDock
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentSectionCard
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentTagPill
import dev.forcetower.unes.ui.feature.enrollment.components.workloadSignal
import dev.forcetower.unes.ui.feature.overview.ColorFor

// Turma picker for one discipline (dc `MatriculaScreen` discipline view):
// tinted eyebrow, prereq banner, section cards with conflict/queue handling
// and the "aceitar outra turma" toggle once a section is picked.
@Composable
internal fun EnrollmentDisciplineScreen(
    disciplineId: Long,
    onBack: () -> Unit,
    onOpenTimetable: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: EnrollmentViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    EnrollmentDisciplineContent(
        state = state,
        disciplineId = disciplineId,
        onBack = onBack,
        onSectionTapped = { vm.onIntent(EnrollmentIntent.SectionTapped(disciplineId, it)) },
        onAllowsOtherChanged = { vm.onIntent(EnrollmentIntent.AllowsOtherChanged(disciplineId, it)) },
        onOpenTimetable = onOpenTimetable,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun EnrollmentDisciplineContent(
    state: EnrollmentUiState,
    disciplineId: Long,
    onBack: () -> Unit,
    onSectionTapped: (Long) -> Unit,
    onAllowsOtherChanged: (Boolean) -> Unit,
    onOpenTimetable: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val discipline = state.disciplineById(disciplineId)
    val window = state.window
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset + 120.dp),
        ) {
            EnrollmentAppBar(
                title = discipline?.code ?: stringResource(R.string.enrollment_title),
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )

            if (discipline == null) {
                Text(
                    text = stringResource(R.string.enrollment_no_results),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                )
                return@Column
            }

            val hue = ColorFor.discipline(discipline.code)
            val selectedSection = state.selectedSectionOf(discipline.id)

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 20)) {
                    Text(
                        text = stringResource(
                            R.string.enrollment_discipline_eyebrow_format,
                            discipline.code,
                            stringResource(
                                if (discipline.mandatory) R.string.enrollment_kind_mandatory
                                else R.string.enrollment_kind_optional,
                            ),
                        ).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.48.sp,
                        ),
                        color = hue,
                    )
                    Text(
                        text = discipline.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 27.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.81).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.enrollment_hours_format, discipline.workload) + " · " +
                                pluralStringResource(
                                    R.plurals.enrollment_sections_count,
                                    discipline.sections.size,
                                    discipline.sections.size,
                                ),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    if (discipline.suggestion) {
                        EnrollmentTagPill(
                            text = stringResource(R.string.enrollment_suggested_by_course),
                            hue = MaterialTheme.melon.status.ok,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }

                if (discipline.prerequisites.isNotEmpty()) {
                    val unmet = discipline.hasUnmetPrerequisite
                    val pendingSuffix = stringResource(R.string.enrollment_prereq_pending_suffix)
                    val unmetNote = stringResource(R.string.enrollment_prereq_unmet_note)
                    EnrollmentBanner(
                        tone = if (unmet) EnrollmentBannerTone.Danger else EnrollmentBannerTone.Ok,
                        icon = if (unmet) Icons.Filled.Warning else Icons.Filled.Check,
                        title = stringResource(
                            if (unmet) R.string.enrollment_prereq_unmet_title
                            else R.string.enrollment_prereq_met_title,
                        ),
                        text = buildString {
                            append(
                                discipline.prerequisites.joinToString(", ") { prereq ->
                                    "${prereq.code} ${prereq.name}" + if (prereq.met) "" else pendingSuffix
                                },
                            )
                            append(".")
                            if (unmet) {
                                append(" ")
                                append(unmetNote)
                            }
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fadeUpOnAppear(delayMs = 80),
                    )
                }

                Text(
                    text = stringResource(R.string.enrollment_choose_section),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.57).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 20.dp, bottom = 12.dp)
                        .fadeUpOnAppear(delayMs = 120),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fadeUpOnAppear(delayMs = 160),
                ) {
                    discipline.sections.forEach { section ->
                        EnrollmentSectionCard(
                            section = section,
                            hue = hue,
                            isSelected = selectedSection?.id == section.id,
                            clash = state.clashFor(discipline, section),
                            useQueue = window?.useQueue == true,
                            onTap = { onSectionTapped(section.id) },
                            readonly = state.isReadonly,
                        )
                    }
                }

                if (selectedSection != null && !state.isReadonly) {
                    val pick = state.pickFor(discipline.id)
                    AllowsOtherCard(
                        sectionLabel = selectedSection.label,
                        disciplineCode = discipline.code,
                        checked = pick?.allowsOther == true,
                        onChanged = onAllowsOtherChanged,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }

        if (window != null) {
            val signal = workloadSignal(state.totalHours, window)
            val conflictCount = state.conflicts.size
            EnrollmentDock(
                totalHours = state.totalHours,
                maxHours = window.maxHours,
                hoursColor = signal.color,
                subText = if (conflictCount > 0) {
                    pluralStringResource(R.plurals.enrollment_dock_conflicts, conflictCount, conflictCount)
                } else {
                    pluralStringResource(
                        R.plurals.enrollment_dock_disciplines,
                        state.resolvedPicks.size,
                        state.resolvedPicks.size,
                    )
                },
                subColor = if (conflictCount > 0) MaterialTheme.melon.status.bad else MaterialTheme.colorScheme.outline,
                primaryLabel = stringResource(R.string.enrollment_dock_done),
                onPrimary = onBack,
                onGrade = onOpenTimetable,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset),
            )
        }
    }
}

// "Aceitar outra turma" — the M3 switch card shown once a section is picked.
@Composable
private fun AllowsOtherCard(
    sectionLabel: String,
    disciplineCode: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.enrollment_allows_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.enrollment_allows_text_format, sectionLabel, disciplineCode),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.5.sp),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Preview
@Composable
private fun EnrollmentDisciplinePreview() {
    MelonTheme {
        EnrollmentDisciplineContent(
            state = EnrollmentFixtures.state,
            disciplineId = EnrollmentFixtures.disciplines.first().id,
            onBack = {},
            onSectionTapped = {},
            onAllowsOtherChanged = {},
            onOpenTimetable = {},
        )
    }
}

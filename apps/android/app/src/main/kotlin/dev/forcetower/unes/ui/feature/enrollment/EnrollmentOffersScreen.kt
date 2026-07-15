package dev.forcetower.unes.ui.feature.enrollment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentDiscipline
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentAppBar
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentCodeChip
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentDock
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentTagPill
import dev.forcetower.unes.ui.feature.enrollment.components.workloadSignal
import dev.forcetower.unes.ui.feature.overview.ColorFor
import java.text.Normalizer

// Ofertadas catalogue (dc `MatriculaScreen` offers view): search pill, M3
// filter chips, disciplines grouped by curriculum period with the optativas
// section last, selection state inline on each card.
@Composable
internal fun EnrollmentOffersScreen(
    onBack: () -> Unit,
    onOpenDiscipline: (Long) -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: EnrollmentViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    EnrollmentOffersContent(
        state = state,
        onBack = onBack,
        onQueryChanged = { vm.onIntent(EnrollmentIntent.QueryChanged(it)) },
        onFilterChanged = { vm.onIntent(EnrollmentIntent.FilterChanged(it)) },
        onOpenDiscipline = onOpenDiscipline,
        onOpenTimetable = onOpenTimetable,
        onOpenReview = onOpenReview,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun EnrollmentOffersContent(
    state: EnrollmentUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onFilterChanged: (EnrollmentFilter) -> Unit,
    onOpenDiscipline: (Long) -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val groups = filteredGroups(state)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomInset + 120.dp),
        ) {
            item(key = "chrome") {
                Column {
                    EnrollmentAppBar(
                        title = stringResource(R.string.enrollment_offers_appbar),
                        onBack = onBack,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                    )
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = stringResource(R.string.enrollment_offers_title),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                lineHeight = 31.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.9).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.enrollment_offers_sub_format, state.disciplines.size, state.disciplines.size),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        OffersSearchField(
                            query = state.query,
                            onQueryChanged = onQueryChanged,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        ) {
                            OffersFilterChip(
                                label = stringResource(R.string.enrollment_filter_all),
                                selected = state.filter == EnrollmentFilter.All,
                                onClick = { onFilterChanged(EnrollmentFilter.All) },
                            )
                            OffersFilterChip(
                                label = stringResource(R.string.enrollment_filter_mandatory),
                                selected = state.filter == EnrollmentFilter.Mandatory,
                                onClick = { onFilterChanged(EnrollmentFilter.Mandatory) },
                            )
                            OffersFilterChip(
                                label = stringResource(R.string.enrollment_filter_optional),
                                selected = state.filter == EnrollmentFilter.Optional,
                                onClick = { onFilterChanged(EnrollmentFilter.Optional) },
                            )
                        }
                    }
                }
            }

            if (groups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.enrollment_no_results),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp, horizontal = 20.dp),
                    )
                }
            }

            groups.forEach { group ->
                item(key = "period-${group.period}") {
                    Text(
                        text = if (group.period == 0) {
                            stringResource(R.string.enrollment_period_optional)
                        } else {
                            stringResource(R.string.enrollment_period_format, group.period)
                        }.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.78.sp,
                        ),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 12.dp),
                    )
                }
                items(group.disciplines, key = { it.id }) { discipline ->
                    OfferCard(
                        discipline = discipline,
                        selectedLabel = state.selectedSectionOf(discipline.id)?.label,
                        onClick = { onOpenDiscipline(discipline.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }
        }

        val window = state.window
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
                primaryLabel = stringResource(R.string.enrollment_dock_review),
                primaryIcon = Icons.AutoMirrored.Filled.ArrowForward,
                onPrimary = onOpenReview,
                onGrade = onOpenTimetable,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset),
            )
        }
    }
}

// Accent-fold both sides so "estatistica" matches "Estatística".
private fun fold(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

@Composable
private fun filteredGroups(state: EnrollmentUiState): List<EnrollmentPeriodGroup> {
    val filtered = state.disciplines
        .filter {
            when (state.filter) {
                EnrollmentFilter.All -> true
                EnrollmentFilter.Mandatory -> it.mandatory
                EnrollmentFilter.Optional -> !it.mandatory
            }
        }
        .filter {
            val term = fold(state.query.trim())
            term.isEmpty() || fold(it.code).contains(term) || fold(it.name).contains(term)
        }
    return groupedByPeriod(filtered)
}

@Composable
private fun OffersSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.enrollment_search_placeholder),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            val clearLabel = stringResource(R.string.enrollment_search_clear)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(role = Role.Button, onClickLabel = clearLabel) { onQueryChanged("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = clearLabel,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun OffersFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun OfferCard(
    discipline: EnrollmentDiscipline,
    selectedLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hue = ColorFor.discipline(discipline.code)
    val shape = RoundedCornerShape(20.dp)
    val selected = selectedLabel != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, if (selected) hue.copy(alpha = 0.4f) else MaterialTheme.melon.surface.line, shape)
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 14.dp)
                    .width(3.5.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(hue),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (selected) 12.5.dp else 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EnrollmentCodeChip(code = discipline.code, hue = hue)
                if (discipline.suggestion) {
                    EnrollmentTagPill(
                        text = stringResource(R.string.enrollment_suggested),
                        hue = MaterialTheme.melon.status.ok,
                    )
                }
                Box(modifier = Modifier.weight(1f))
                if (selected) {
                    EnrollmentTagPill(
                        text = selectedLabel.orEmpty(),
                        hue = MaterialTheme.colorScheme.primary,
                        icon = Icons.Filled.Check,
                        solid = true,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = discipline.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.42).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 9.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp),
            ) {
                Text(
                    text = stringResource(R.string.enrollment_hours_format, discipline.workload),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.enrollment_sections_count,
                        discipline.sections.size,
                        discipline.sections.size,
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline,
                )
                Box(modifier = Modifier.weight(1f))
                KindPill(mandatory = discipline.mandatory)
                if (discipline.hasUnmetPrerequisite) {
                    EnrollmentTagPill(
                        text = stringResource(R.string.enrollment_prereq_tag),
                        hue = MaterialTheme.melon.status.bad,
                        icon = Icons.Filled.PriorityHigh,
                    )
                }
            }
        }
    }
}

@Composable
private fun KindPill(mandatory: Boolean) {
    if (mandatory) {
        Text(
            text = stringResource(R.string.enrollment_kind_mandatory),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    } else {
        Text(
            text = stringResource(R.string.enrollment_kind_optional),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.melon.surface.line, CircleShape)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Preview
@Composable
private fun EnrollmentOffersPreview() {
    MelonTheme {
        EnrollmentOffersContent(
            state = EnrollmentFixtures.state,
            onBack = {},
            onQueryChanged = {},
            onFilterChanged = {},
            onOpenDiscipline = {},
            onOpenTimetable = {},
            onOpenReview = {},
        )
    }
}

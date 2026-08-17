package dev.forcetower.unes.ui.feature.courseprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.courseprogress.components.CourseProgressGaugeCard
import dev.forcetower.unes.ui.feature.courseprogress.components.CourseProgressNotice
import dev.forcetower.unes.ui.feature.courseprogress.components.CourseProgressSectionHeader
import dev.forcetower.unes.ui.feature.courseprogress.components.ComplementaryHoursSheet
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumFlowCard
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumRemainingCard
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumRequirementsCard
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumVersionButton
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumVersionPickerSheet
import dev.forcetower.unes.ui.feature.courseprogress.components.requirementsHint

// "Progresso do curso" — how much carga horária is done against the
// curriculum, broken down by natureza, plus the door into the fluxograma
// (dc `ProgressoScreen`, iOS `CourseProgressView`).
//
// Offline-first: the screen renders whatever the mirror holds and re-pulls
// `api/curriculum` on every entry, so a failed refresh never blanks a payload
// the student already has.
@Composable
internal fun CourseProgressScreen(
    onBack: () -> Unit,
    onOpenFlowchart: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CourseProgressViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.onIntent(CourseProgressIntent.Load) }

    CourseProgressContent(
        state = state,
        onBack = onBack,
        onOpenFlowchart = {
            vm.trackFlowchartOpen()
            onOpenFlowchart()
        },
        onRetry = { vm.onIntent(CourseProgressIntent.Retry) },
        onOpenExplainer = { vm.onIntent(CourseProgressIntent.ExplainerTapped) },
        onDismissExplainer = { vm.onIntent(CourseProgressIntent.ExplainerDismissed) },
        onOpenVersionPicker = { vm.onIntent(CourseProgressIntent.VersionPickerTapped) },
        onDismissVersionPicker = { vm.onIntent(CourseProgressIntent.VersionPickerDismissed) },
        onPickVersion = { vm.onIntent(CourseProgressIntent.VersionSelected(it)) },
        onAutomaticVersion = { vm.onIntent(CourseProgressIntent.AutomaticVersionTapped) },
        onDismissVersionSwitchFailure = { vm.onIntent(CourseProgressIntent.VersionSwitchFailureDismissed) },
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseProgressContent(
    state: CourseProgressUiState,
    onBack: () -> Unit,
    onOpenFlowchart: () -> Unit,
    onRetry: () -> Unit,
    onOpenExplainer: () -> Unit,
    onDismissExplainer: () -> Unit,
    onOpenVersionPicker: () -> Unit,
    onDismissVersionPicker: () -> Unit,
    onPickVersion: (String) -> Unit,
    onAutomaticVersion: () -> Unit,
    onDismissVersionSwitchFailure: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    // The large headline collapses into the bar title as the page scrolls —
    // the M3 behaviour the design asks for, straight out of the box.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val background = MaterialTheme.colorScheme.background

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        LargeTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.course_progress_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                    ),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.course_progress_back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = background,
                scrolledContainerColor = background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            scrollBehavior = scrollBehavior,
        )

        val progress = state.progress
        when {
            progress != null -> LoadedContent(
                state = state,
                progress = progress,
                onOpenFlowchart = onOpenFlowchart,
                onOpenExplainer = onOpenExplainer,
                onOpenVersionPicker = onOpenVersionPicker,
                bottomInset = bottomInset,
            )
            state.failed -> LoadFailure(onRetry = onRetry)
            else -> Loading()
        }
    }

    val explainerRequirement = state.progress?.requirements?.firstOrNull { !it.derivable }
    if (state.explainerOpen && explainerRequirement != null) {
        ComplementaryHoursSheet(
            requirement = explainerRequirement,
            onDismiss = onDismissExplainer,
        )
    }

    if (state.versionPickerOpen && state.progress != null) {
        CurriculumVersionPickerSheet(
            progress = state.progress,
            course = state.course,
            switchingVersionId = state.switchingVersionId,
            onPick = onPickVersion,
            onAutomatic = onAutomaticVersion,
            onDismiss = onDismissVersionPicker,
        )
    }

    if (state.versionSwitchFailed) {
        AlertDialog(
            onDismissRequest = onDismissVersionSwitchFailure,
            title = { Text(text = stringResource(R.string.course_progress_version_switch_failed_title)) },
            text = { Text(text = stringResource(R.string.course_progress_version_switch_failed_body)) },
            confirmButton = {
                TextButton(onClick = onDismissVersionSwitchFailure) {
                    Text(text = stringResource(R.string.course_progress_version_switch_failed_ok))
                }
            },
        )
    }
}

@Composable
private fun LoadedContent(
    state: CourseProgressUiState,
    progress: CourseProgress,
    onOpenFlowchart: () -> Unit,
    onOpenExplainer: () -> Unit,
    onOpenVersionPicker: () -> Unit,
    bottomInset: Dp,
) {
    val curriculum = progress.curriculum
    val hasUnmeasurable = progress.requirements.any { !it.derivable }
    val totalKnown = progress.summary.requiredHours != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val subtitle = headerSubtitle(state, progress)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        if (progress.canPickVersion) {
            CurriculumVersionButton(
                progress = progress,
                course = state.course,
                onClick = onOpenVersionPicker,
                modifier = Modifier.fadeUpOnAppear(delayMs = 20),
            )
        }

        if (curriculum != null && curriculum.stale) {
            val checkedOn = CourseProgressFormat.asOf(curriculum.asOf)
            CourseProgressNotice(
                tone = MaterialTheme.melon.palette.orange,
                icon = Icons.Filled.Schedule,
                title = stringResource(
                    R.string.course_progress_stale_title_format,
                    curriculum.codeLabel,
                ),
                body = if (checkedOn != null) {
                    stringResource(R.string.course_progress_stale_body_format, checkedOn)
                } else {
                    stringResource(R.string.course_progress_stale_body_undated)
                },
                modifier = Modifier.fadeUpOnAppear(delayMs = 40),
            )
        }

        CourseProgressGaugeCard(
            progress = progress,
            modifier = Modifier.fadeUpOnAppear(delayMs = 80),
        )

        if (curriculum == null) {
            CourseProgressNotice(
                tone = MaterialTheme.melon.palette.sky,
                icon = Icons.Filled.ErrorOutline,
                title = stringResource(R.string.course_progress_no_curriculum_title),
                body = stringResource(R.string.course_progress_no_curriculum_body),
                modifier = Modifier.fadeUpOnAppear(delayMs = 120),
            )
        }

        Column(modifier = Modifier.fadeUpOnAppear(delayMs = 160)) {
            CourseProgressSectionHeader(
                title = stringResource(R.string.course_progress_section_curriculum),
                hint = stringResource(
                    if (curriculum == null) {
                        R.string.course_progress_section_curriculum_hint_unavailable
                    } else {
                        R.string.course_progress_section_curriculum_hint
                    },
                ),
            )
            CurriculumFlowCard(progress = progress, onClick = onOpenFlowchart)
        }

        if (curriculum != null && !progress.hasBreakdown) {
            CourseProgressNotice(
                tone = MaterialTheme.melon.palette.sky,
                icon = Icons.Filled.ErrorOutline,
                title = stringResource(R.string.course_progress_no_breakdown_title),
                body = stringResource(R.string.course_progress_no_breakdown_body),
                modifier = Modifier.fadeUpOnAppear(delayMs = 200),
            )
        }

        if (progress.hasBreakdown) {
            Column(modifier = Modifier.fadeUpOnAppear(delayMs = 200)) {
                CourseProgressSectionHeader(
                    title = stringResource(R.string.course_progress_section_requirements),
                    hint = if (totalKnown) {
                        requirementsHint(progress.requirements.size)
                    } else {
                        stringResource(R.string.course_progress_no_total_for_curriculum)
                    },
                )
                CurriculumRequirementsCard(
                    requirements = progress.requirements,
                    totalKnown = totalKnown,
                )
                if (hasUnmeasurable) {
                    OutlinedButton(
                        onClick = onOpenExplainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = stringResource(R.string.course_progress_explainer_button),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (totalKnown && curriculum != null) {
                CurriculumRemainingCard(
                    requirements = progress.requirements,
                    curriculumCode = curriculum.codeLabel,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 240),
                )
            }
        }

        Text(
            text = stringResource(
                R.string.course_progress_footer_format,
                CourseProgressFormat.syncedAt(progress.syncedAt.toEpochMilliseconds()),
            ),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
        Spacer(Modifier.height(14.dp))
    }
}

// "Psicologia · currículo 20232 · 3º semestre" — whichever of the three the
// payload and profile actually know.
@Composable
private fun headerSubtitle(state: CourseProgressUiState, progress: CourseProgress): String? {
    val parts = buildList {
        state.course?.takeIf { it.isNotBlank() }?.let { add(it) }
        progress.curriculum?.let {
            add(stringResource(R.string.course_progress_curriculum_code, it.codeLabel))
        }
        progress.currentPeriod?.let {
            add(
                stringResource(
                    R.string.course_progress_semester_format,
                    CourseProgressFormat.ordinal(it),
                ),
            )
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun Loading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.course_progress_loading),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

@Composable
private fun LoadFailure(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.padding(bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Text(
            text = stringResource(R.string.course_progress_load_failed_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.course_progress_load_failed_body),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 20.dp)) {
            Text(text = stringResource(R.string.course_progress_retry))
        }
    }
}

@Preview
@Composable
private fun CourseProgressScreenPreview() {
    MelonTheme {
        CourseProgressContent(
            state = CourseProgressUiState(
                progress = CourseProgressPreviewData.progress,
                loading = false,
                course = "Psicologia",
            ),
            onBack = {},
            onOpenFlowchart = {},
            onRetry = {},
            onOpenExplainer = {},
            onDismissExplainer = {},
            onOpenVersionPicker = {},
            onDismissVersionPicker = {},
            onPickVersion = {},
            onAutomaticVersion = {},
            onDismissVersionSwitchFailure = {},
        )
    }
}

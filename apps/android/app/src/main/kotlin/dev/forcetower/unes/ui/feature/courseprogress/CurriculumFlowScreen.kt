package dev.forcetower.unes.ui.feature.courseprogress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumEntrySheet
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumGridLens
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumMapLens
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumPeriodRail
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumPeriodsLens
import dev.forcetower.unes.ui.feature.courseprogress.components.CurriculumTrailBanner

// "Fluxograma" — the curriculum grid through three lenses (dc `ProgressoScreen`
// tela 2, iOS `CurriculumFlowView`). Reads the same activity-scoped ViewModel
// as the progress screen, so it opens on the mirrored payload with no fetch of
// its own and keeps its lens/selection across a pop.
@Composable
internal fun CurriculumFlowScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CourseProgressViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    // A refresh landing while the grid is open updates it in place.
    LaunchedEffect(Unit) { vm.onIntent(CourseProgressIntent.Load) }

    CurriculumFlowContent(
        state = state,
        onBack = onBack,
        onLensChange = { vm.onIntent(CourseProgressIntent.LensChanged(it)) },
        onSelectPeriod = { vm.onIntent(CourseProgressIntent.PeriodSelected(it)) },
        onOpenEntry = { vm.onIntent(CourseProgressIntent.EntryTapped(it)) },
        onDismissEntry = { vm.onIntent(CourseProgressIntent.EntrySheetDismissed) },
        onShowTrail = { vm.onIntent(CourseProgressIntent.TrailRequested(it)) },
        onClearTrail = { vm.onIntent(CourseProgressIntent.TrailCleared) },
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurriculumFlowContent(
    state: CourseProgressUiState,
    onBack: () -> Unit,
    onLensChange: (CurriculumLens) -> Unit,
    onSelectPeriod: (Int) -> Unit,
    onOpenEntry: (String) -> Unit,
    onDismissEntry: () -> Unit,
    onShowTrail: (String) -> Unit,
    onClearTrail: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val progress = state.progress
    val background = MaterialTheme.colorScheme.background

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.course_progress_flowchart_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
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
            actions = {
                progress?.curriculum?.let { curriculum ->
                    Text(
                        text = curriculum.codeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(end = 14.dp),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = background,
                scrolledContainerColor = background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        if (progress == null) return@Column

        val lensPickerLabel = stringResource(R.string.course_progress_lens_picker)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = lensPickerLabel },
        ) {
            CurriculumLens.entries.forEachIndexed { index, lens ->
                SegmentedButton(
                    selected = state.lens == lens,
                    onClick = { onLensChange(lens) },
                    shape = SegmentedButtonDefaults.itemShape(index, CurriculumLens.entries.size),
                ) {
                    Text(text = stringResource(lensLabelRes(lens)))
                }
            }
        }

        val trail = state.trail
        if (trail != null) {
            CurriculumTrailBanner(
                focusCode = trail.focus,
                linkedCount = trail.codes.size,
                onClear = onClearTrail,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        }

        if (state.lens == CurriculumLens.Periods) {
            CurriculumPeriodRail(
                periods = progress.scheduledPeriods,
                selected = state.selectedPeriod,
                currentPeriod = progress.currentPeriod,
                onSelect = onSelectPeriod,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = bottomInset),
        ) {
            when (state.lens) {
                CurriculumLens.Periods -> state.selectedPeriodEntries?.let { period ->
                    CurriculumPeriodsLens(
                        progress = progress,
                        period = period,
                        trail = trail,
                        onSelectPeriod = onSelectPeriod,
                        onOpenEntry = onOpenEntry,
                    )
                }
                CurriculumLens.Map -> CurriculumMapLens(
                    progress = progress,
                    trail = trail,
                    onOpenEntry = onOpenEntry,
                    onSelectPeriod = onSelectPeriod,
                )
                CurriculumLens.Grid -> CurriculumGridLens(
                    progress = progress,
                    trail = trail,
                    onOpenEntry = onOpenEntry,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    val openedEntry = state.openedEntry
    if (openedEntry != null && progress != null) {
        CurriculumEntrySheet(
            entry = openedEntry,
            progress = progress,
            onOpenEntry = onOpenEntry,
            onShowTrail = onShowTrail,
            onDismiss = onDismissEntry,
        )
    }
}

private fun lensLabelRes(lens: CurriculumLens): Int = when (lens) {
    CurriculumLens.Periods -> R.string.course_progress_lens_periods
    CurriculumLens.Map -> R.string.course_progress_lens_map
    CurriculumLens.Grid -> R.string.course_progress_lens_grid
}

@Preview
@Composable
private fun CurriculumFlowScreenPreview() {
    MelonTheme {
        CurriculumFlowContent(
            state = CourseProgressUiState(
                progress = CourseProgressPreviewData.progress,
                loading = false,
                selectedPeriod = 3,
            ),
            onBack = {},
            onLensChange = {},
            onSelectPeriod = {},
            onOpenEntry = {},
            onDismissEntry = {},
            onShowTrail = {},
            onClearTrail = {},
        )
    }
}

package dev.forcetower.unes.ui.feature.disciplines

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonPaletteColors
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.components.ActiveDisciplineCard
import dev.forcetower.unes.ui.feature.disciplines.components.CurrentSemesterSummary
import dev.forcetower.unes.ui.feature.disciplines.components.PastSemesterCard
import dev.forcetower.unes.ui.feature.disciplines.components.UndownloadedSemesterCard
import dev.forcetower.unes.ui.feature.overview.ColorFor
import java.util.Locale

// "Disciplinas" tab — the Boletim screen. Current semester expanded as rich
// cards on top, then a "Histórico" section with collapsible past-semester
// cards and tap-to-fetch placeholder cards for semesters not yet downloaded.
//
// Mirrors iOS `DisciplinesListView` and consumes the same KMP use cases
// (`ObserveDisciplinesListUseCase`, `SyncSemesterUseCase`) through
// `DisciplinesListViewModel`.
@Composable
internal fun DisciplinesScreen(
    onOpenDiscipline: (Discipline) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: DisciplinesListViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val palette = MaterialTheme.melon.palette

    // Color resolution depends on the palette CompositionLocal, so we tint the
    // KMP-mapped projection here (the ViewModel left `color = Unspecified`).
    val current = remember(state.current, palette) { state.current?.tinted(palette) }
    val past = remember(state.past, palette) { state.past.map { it.tinted(palette) } }
    val pending = state.pending

    // Between semesters, upstream returns no running semester — promote the
    // most-recent past into the "current" slot so the screen still opens with
    // a meaningful card stack instead of an empty header. Matches iOS
    // `effectiveCurrent` / `effectivePast` in `DisciplinesListView.swift`.
    val effectiveCurrent = current ?: past.firstOrNull()
    val effectivePast = if (current != null) past else past.drop(1)

    DisciplinesContent(
        effectiveCurrent = effectiveCurrent,
        effectivePast = effectivePast,
        pending = pending,
        downloading = state.downloading,
        onOpenDiscipline = onOpenDiscipline,
        onDownload = { vm.onIntent(DisciplinesIntent.Download(it)) },
        bottomInset = bottomInset,
        modifier = modifier,
    )
}

@Composable
private fun DisciplinesContent(
    effectiveCurrent: Semester?,
    effectivePast: List<Semester>,
    pending: List<Semester>,
    downloading: Set<String>,
    onOpenDiscipline: (Discipline) -> Unit,
    onDownload: (String) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            AmbientMeshTop(surface = surface)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item("disciplines-header") {
                Header(
                    semesterCode = effectiveCurrent?.id,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 20),
                )
            }
            if (effectiveCurrent != null) {
                item("disciplines-summary") {
                    CurrentSemesterSummary(
                        disciplines = effectiveCurrent.disciplines,
                        modifier = Modifier
                            .fadeUpOnAppear(delayMs = 80)
                            .padding(bottom = 14.dp),
                    )
                }
                items(
                    items = effectiveCurrent.disciplines,
                    key = { it.fullCode + (it.offerId ?: "") },
                ) { d ->
                    ActiveDisciplineCard(
                        discipline = d,
                        onOpen = { onOpenDiscipline(d) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 5.dp),
                    )
                }
            }

            item("disciplines-history-divider") {
                HistoryDivider(
                    modifier = Modifier.fadeUpOnAppear(delayMs = 500),
                )
            }

            // Past semesters — most recent open by default. iOS scopes this to
            // `effectivePast.first()`, mirrored here via the index check.
            itemsIndexed(
                items = effectivePast,
                key = { _, sem -> "past-${sem.id}" },
            ) { index, sem ->
                PastSemesterCard(
                    semester = sem,
                    onOpenDiscipline = onOpenDiscipline,
                    defaultOpen = index == 0,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                )
            }

            // Pending placeholders — tap kicks off `SyncSemesterUseCase`. KMP
            // re-emits with the semester moved into `past`, so the placeholder
            // disappears once the fetch lands.
            items(
                items = pending,
                key = { "pending-${it.id}" },
            ) { sem ->
                UndownloadedSemesterCard(
                    semesterCode = sem.id,
                    estimatedCount = sem.estimatedCount,
                    isLoading = sem.id in downloading,
                    onDownload = { onDownload(sem.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun Header(semesterCode: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(
                R.string.disciplines_eyebrow_semester_format,
                (semesterCode ?: "—").uppercase(Locale.ROOT),
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.disciplines_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.64).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun HistoryDivider(modifier: Modifier = Modifier) {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.melon.surface.line
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(line),
        )
        Text(
            text = stringResource(R.string.disciplines_history_divider).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
            ),
            color = ink4,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(line),
        )
    }
}

@Composable
private fun AmbientMeshTop(surface: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        Mesh(
            variant = MeshVariant.Warm,
            intensity = 0.22f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.95f to surface,
                    ),
                ),
        )
    }
}

// ───────── Tinting ─────────

// The ViewModel emits Disciplines with `color = Unspecified` (palette colors
// need a Composable context). Resolve them here against the live palette so
// the tint is theme-adaptive.
private fun Semester.tinted(palette: MelonPaletteColors): Semester =
    copy(disciplines = disciplines.map { it.tinted(palette) })

private fun Discipline.tinted(palette: MelonPaletteColors): Discipline =
    copy(color = ColorFor.discipline(palette, code))


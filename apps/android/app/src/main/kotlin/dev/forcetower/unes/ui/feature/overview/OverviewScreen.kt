package dev.forcetower.unes.ui.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.MelonPaletteColors
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.components.DisciplinesStrip
import dev.forcetower.unes.ui.feature.overview.components.NowCard
import dev.forcetower.unes.ui.feature.overview.components.OverviewHeader
import dev.forcetower.unes.ui.feature.overview.components.OverviewTileGrid
import dev.forcetower.unes.ui.feature.overview.components.TodayTimeline
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.forcetower.melon.feature.overview.domain.model.OverviewAttendanceTile as KmpOverviewAttendanceTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewClassState as KmpOverviewClassState
import dev.forcetower.melon.feature.overview.domain.model.OverviewDiscipline as KmpOverviewDiscipline
import dev.forcetower.melon.feature.overview.domain.model.OverviewGradeTile as KmpOverviewGradeTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewMessagesTile as KmpOverviewMessagesTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewNextTestTile as KmpOverviewNextTestTile
import dev.forcetower.melon.feature.overview.domain.model.OverviewNowClass as KmpOverviewNowClass
import dev.forcetower.melon.feature.overview.domain.model.OverviewTodayItem as KmpOverviewTodayItem

// "Hoje" tab — the at-a-glance dashboard rendered as the first tab inside
// `ConnectedScreen`. Driven by `OverviewViewModel`, which fans out nine KMP
// flows + a 30s clock ticker; the screen maps the raw KMP payloads into the
// local UI projection types declared in `OverviewFixtures.kt` (those types are
// also reused by Compose previews on the components).
@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: OverviewViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val surface = MaterialTheme.colorScheme.surface
    val palette = MaterialTheme.melon.palette

    val now = state.nowRaw?.let { mapNow(it, palette) }
    val today = mapToday(state.todayRaw, palette)
    val disciplines = mapDisciplines(state.disciplinesRaw, palette)
    val gradeTile = state.gradeTileRaw?.let(::mapGradeTile)
    val messagesTile = state.messagesTileRaw?.let(::mapMessagesTile)
    val nextTestTile = state.nextTestTileRaw?.let(::mapNextTestTile)
    val attendanceTile = state.attendanceTileRaw?.let(::mapAttendanceTile)

    val greeting = stringResource(
        when (state.greetingKind) {
            GreetingKind.Morning -> R.string.overview_greeting_morning
            GreetingKind.Afternoon -> R.string.overview_greeting_afternoon
            GreetingKind.Evening -> R.string.overview_greeting_evening
        },
    )
    val name = state.firstName ?: stringResource(R.string.overview_default_user)
    val avatarInitial = state.avatarInitial ?: "?"
    val lastUpdatedLabel = lastUpdatedLabel(state.lastUpdatedKind)

    Box(modifier = modifier
        .fillMaxSize()
        .background(surface)) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            AmbientMeshTop(surface = surface)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OverviewHeader(
                greeting = greeting,
                name = name,
                avatarInitial = avatarInitial,
                dateEyebrow = state.dateEyebrow,
                modifier = Modifier.fadeUpOnAppear(delayMs = 20),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (now != null) {
                    NowCard(
                        now = now,
                        modifier = Modifier.scaleInOnAppear(delayMs = 120, fromScale = 0.985f),
                    )
                }
                if (today.isNotEmpty()) {
                    TodayTimeline(
                        items = today,
                        modifier = Modifier.fadeUpOnAppear(delayMs = 240),
                    )
                }
                OverviewTileGrid(
                    grade = gradeTile,
                    messages = messagesTile,
                    nextTest = nextTestTile,
                    attendance = attendanceTile,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 340),
                )
            }

            DisciplinesStrip(
                items = disciplines,
                semesterLabel = state.semesterLabel,
                modifier = Modifier.fadeUpOnAppear(delayMs = 440),
            )

            Text(
                text = lastUpdatedLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.26.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeUpOnAppear(delayMs = 520)
                    .padding(top = 8.dp, bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AmbientMeshTop(surface: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
    ) {
        Mesh(
            variant = MeshVariant.Warm,
            intensity = 0.2f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to surface,
                    ),
                ),
        )
    }
}

@Composable
private fun lastUpdatedLabel(kind: LastUpdatedKind): String = when (kind) {
    LastUpdatedKind.Syncing -> stringResource(R.string.overview_last_updated_syncing)
    LastUpdatedKind.JustNow -> stringResource(R.string.overview_last_updated_now)
    is LastUpdatedKind.Minutes -> stringResource(R.string.overview_last_updated_format, kind.n)
    is LastUpdatedKind.Hours -> stringResource(R.string.overview_last_updated_hours_format, kind.n)
    is LastUpdatedKind.Days -> stringResource(R.string.overview_last_updated_days_format, kind.n)
}

// ───────── KMP → UI projection mappers ─────────

private fun mapNow(raw: KmpOverviewNowClass, palette: MelonPaletteColors): OverviewNowClass =
    OverviewNowClass(
        code = raw.code,
        title = raw.title,
        prof = raw.teacherName.orEmpty(),
        room = raw.roomLocation.orEmpty(),
        // Eyebrow copy is "próxima aula · em N min"; clamp negatives (class
        // already running) to 0 so the format string stays sensible until we
        // introduce a dedicated "happening now" eyebrow variant.
        startsInMinutes = raw.startsInMinutes.coerceAtLeast(0),
        timeRange = formatTimeRange(raw.startTime, raw.endTime),
        topic = raw.topic,
        color = ColorFor.discipline(palette, raw.code),
        meshVariant = ColorFor.meshVariant(raw.code),
    )

private fun mapToday(
    raw: List<KmpOverviewTodayItem>,
    palette: MelonPaletteColors,
): List<OverviewTodayItem> = raw.map { item ->
    OverviewTodayItem(
        time = item.startTime.take(5),
        code = item.code,
        title = item.title,
        room = item.roomLocation.orEmpty(),
        color = ColorFor.discipline(palette, item.code),
        state = mapClassState(item.state),
        topic = item.topic,
    )
}

private fun mapDisciplines(
    raw: List<KmpOverviewDiscipline>,
    palette: MelonPaletteColors,
): List<OverviewDiscipline> = raw.map { item ->
    OverviewDiscipline(
        code = item.code,
        title = item.title,
        grade = item.gradeLabel,
        color = ColorFor.discipline(palette, item.code),
    )
}

private fun mapGradeTile(raw: KmpOverviewGradeTile): OverviewGradeTileData = OverviewGradeTileData(
    value = raw.cr,
    deltaLabel = raw.crDelta?.let { formatDelta(it) },
    comparisonSemester = raw.comparisonSemesterCode,
)

private fun mapMessagesTile(raw: KmpOverviewMessagesTile): OverviewMessagesTileData =
    OverviewMessagesTileData(
        unreadCount = raw.unreadCount,
        lastSender = raw.lastSender,
        lastPreview = raw.lastPreview,
    )

private fun mapNextTestTile(raw: KmpOverviewNextTestTile): OverviewNextTestTileData =
    OverviewNextTestTileData(
        label = raw.label,
        disciplineName = raw.disciplineName,
        daysUntil = raw.daysUntil,
        dateLabel = formatShortDate(raw.date),
    )

private fun mapAttendanceTile(raw: KmpOverviewAttendanceTile): OverviewAttendanceTileData =
    OverviewAttendanceTileData(
        percentage = raw.percentage,
        days = raw.lastDays,
        allowedAbsences = raw.allowedAbsences,
        periodDays = raw.periodDays,
    )

private fun mapClassState(state: KmpOverviewClassState): OverviewClassState = when (state) {
    KmpOverviewClassState.DONE -> OverviewClassState.Done
    KmpOverviewClassState.NOW -> OverviewClassState.Now
    KmpOverviewClassState.NEXT -> OverviewClassState.Next
    KmpOverviewClassState.LATER -> OverviewClassState.Later
}

// ───────── formatting helpers ─────────

private fun formatTimeRange(start: String, end: String?): String {
    val s = start.take(5)
    val e = end?.take(5)?.takeIf { it.isNotEmpty() } ?: return s
    return "$s – $e"
}

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

private val ShortDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", PtBr)

private fun formatShortDate(iso: String): String = runCatching {
    ShortDateFormatter.format(LocalDate.parse(iso))
        .replace(".", "")
        .lowercase(PtBr)
}.getOrDefault(iso)

private fun formatDelta(delta: Double): String {
    // pt-BR locale → comma decimal separator. iOS prints e.g. "+0,3" / "-0,2".
    val formatted = String.format(PtBr, "%.1f", delta)
    return if (delta >= 0) "+$formatted" else formatted
}

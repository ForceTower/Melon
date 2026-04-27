package dev.forcetower.unes.ui.feature.calendar

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.components.CalCategoryFilterRow
import dev.forcetower.unes.ui.feature.calendar.components.CalHeroCard
import dev.forcetower.unes.ui.feature.calendar.components.CalMonthSection
import dev.forcetower.unes.ui.feature.calendar.components.CalScopeFilterRow
import java.util.Locale

// Academic-calendar screen — surfaces every UEFS-side date the student should
// know about (deadlines, exams, holidays) in one agenda. Pushed from the
// "Calendário" shortcut on the Me hub. Mirrors iOS `CalendarView` (timeline
// variant — the only one shipped in production).
@Composable
internal fun CalendarScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: CalendarViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    var category by rememberSaveable { mutableStateOf(CalendarCategoryFilter.All) }
    var scope by rememberSaveable { mutableStateOf(CalendarScopeFilter.All) }

    val filtered = remember(state.events, category, scope) {
        state.events.filter { category.matches(it) && scope.matches(it) }
    }
    // Hide past events from the body, but keep events that are still active
    // even though they started in the past — so the hero + agenda agree.
    val visible = remember(filtered) {
        filtered.filter { CalendarMath.status(it) != CalendarStatus.Past }
    }
    val monthGroups = remember(visible) { visible.groupedByMonth() }
    val hero = remember(filtered) { CalendarMath.nextDeadline(filtered) }

    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Ambient warm mesh wash pinned to the top, fading into the surface.
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)) {
            Mesh(
                variant = MeshVariant.Warm,
                intensity = 0.5f,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.92f to surface,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset + 32.dp),
        ) {
            Header(
                semesterCode = state.semesterCode,
                onBack = onBack,
                modifier = Modifier.fadeUpOnAppear(delayMs = 20),
            )

            if (hero != null) {
                CalHeroCard(
                    event = hero,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 0.dp)
                        .padding(bottom = 14.dp)
                        .fadeUpOnAppear(delayMs = 120),
                )
            }

            CalCategoryFilterRow(
                active = category,
                onChange = { category = it },
                modifier = Modifier.fadeUpOnAppear(delayMs = 200),
            )
            CalScopeFilterRow(
                active = scope,
                onChange = { scope = it },
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 10.dp)
                    .fadeUpOnAppear(delayMs = 260),
            )

            if (monthGroups.isEmpty()) {
                EmptyState()
            } else {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fadeUpOnAppear(delayMs = 340),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    monthGroups.forEach { group ->
                        key(group.id) {
                            CalMonthSection(group = group)
                        }
                    }
                }
            }

            SyncFooter(modifier = Modifier.fadeUpOnAppear(delayMs = 440))
        }
    }
}

@Composable
private fun Header(
    semesterCode: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val effectiveCode = semesterCode ?: CalendarFixtures.SemesterLabel

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 10.dp),
        ) {
            BackChevronButton(onBack = onBack)
            Text(
                text = stringResource(R.string.calendar_header_eyebrow_format, effectiveCode)
                    .uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                ),
                color = ink3,
            )
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = ink)) { append(stringResource(R.string.calendar_header_title_lead)) }
                append(" ")
                withStyle(
                    SpanStyle(
                        color = accent,
                        fontStyle = FontStyle.Italic,
                    ),
                ) { append(stringResource(R.string.calendar_header_title_emphasis)) }
            },
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.64).sp,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.calendar_header_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = ink3,
        )
    }
}

@Composable
private fun BackChevronButton(onBack: () -> Unit) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val backLabel = stringResource(R.string.calendar_back_label)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(card)
            .border(1.dp, cardLine, CircleShape)
            .clickable(onClick = onBack)
            .semantics {
                role = Role.Button
                contentDescription = backLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            val sx = w / 14f
            val sy = h / 14f
            val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = Path().apply {
                moveTo(8.5f * sx, 3f * sy)
                lineTo(4.5f * sx, 7f * sy)
                lineTo(8.5f * sx, 11f * sy)
            }
            drawPath(path = path, color = ink, style = stroke)
        }
    }
}

@Composable
private fun SyncFooter(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.calendar_sync_footer).uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.26.sp,
        ),
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun EmptyState() {
    Text(
        text = stringResource(R.string.calendar_empty_state),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 80.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

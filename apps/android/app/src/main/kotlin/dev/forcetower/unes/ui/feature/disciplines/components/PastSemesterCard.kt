package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineScoreColor
import dev.forcetower.unes.ui.feature.disciplines.DisciplineStatus
import dev.forcetower.unes.ui.feature.disciplines.Semester
import dev.forcetower.unes.ui.feature.disciplines.status
import java.util.Locale

// Collapsible summary card for a past semester. Tap to expand a stack of
// `PastDisciplineRow`s. Mirrors iOS `PastSemesterCard`.
@Composable
internal fun PastSemesterCard(
    semester: Semester,
    onOpenDiscipline: (Discipline) -> Unit,
    modifier: Modifier = Modifier,
    defaultOpen: Boolean = false,
) {
    var expanded by rememberSaveable(semester.id) { mutableStateOf(defaultOpen) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SemesterHeader(semester = semester, expanded = expanded, onToggle = { expanded = !expanded })
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                semester.disciplines.forEach { d ->
                    PastDisciplineRow(discipline = d, onOpen = { onOpenDiscipline(d) })
                }
            }
        }
    }
}

@Composable
private fun SemesterHeader(semester: Semester, expanded: Boolean, onToggle: () -> Unit) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(18.dp)

    val disciplines = semester.disciplines
    val finals = disciplines.mapNotNull { it.finalGrade }
    val mean = if (finals.isEmpty()) null else finals.sum() / finals.size
    // Pass count from the resolved `status` — defers to upstream's `approved`
    // flag so disciplines closed via the final exam (mean in the 5–7 range)
    // still count as passed when upstream marked them so.
    val approved = disciplines.count { it.status.key == DisciplineStatus.Key.Approved }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MelonMotion.spring(),
        label = "PastSemesterChevron",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = semester.id,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        letterSpacing = (-0.18).sp,
                    ),
                    color = ink,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.disciplines_past_count,
                        disciplines.size,
                        disciplines.size,
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = ink4,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (mean != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.disciplines_past_mean_label),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = ink3,
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", mean),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = DisciplineScoreColor.colorFor(mean),
                        )
                    }
                    Text(
                        text = "·",
                        color = ink3.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.disciplines_past_approved_format,
                        approved,
                        disciplines.size,
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = ink3,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            disciplines.take(5).forEach { d ->
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            d.color.copy(
                                alpha = if (d.status.key == DisciplineStatus.Key.Approved) 1f else 0.4f,
                            ),
                        ),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .rotate(rotation),
            contentAlignment = Alignment.Center,
        ) {
            ChevronDown(color = ink3)
        }
    }
}

@Composable
private fun ChevronDown(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.375f)
            lineTo(size.width * 0.5f, size.height * 0.625f)
            lineTo(size.width * 0.75f, size.height * 0.375f)
        }
        drawPath(
            path = path,
            brush = SolidColor(color),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

// Compact row inside an expanded past-semester card. Discipline code, title,
// final grade with pass/fail indicator. Mirrors iOS `PastDisciplineRow`.
@Composable
internal fun PastDisciplineRow(
    discipline: Discipline,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(16.dp)
    val final = discipline.finalGrade
    val statusKey = discipline.status.key
    val railOpacity = if (statusKey == DisciplineStatus.Key.Approved) 1f else 0.4f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .drawBehind {
                // Same rail technique used by `ActiveDisciplineCard` —
                // 3dp wide, 10dp inset top/bottom to match the iOS row.
                val railWidthPx = 3.dp.toPx()
                val railInsetPx = 10.dp.toPx()
                val railHeight = (size.height - railInsetPx * 2f).coerceAtLeast(0f)
                drawRect(
                    color = discipline.color.copy(alpha = railOpacity),
                    topLeft = Offset(0f, railInsetPx),
                    size = Size(railWidthPx, railHeight),
                )
            }
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = discipline.fullCode,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                    ),
                    color = discipline.color,
                )
                Text(
                    text = discipline.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 15.sp,
                        lineHeight = 16.sp,
                        letterSpacing = (-0.15).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = final?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        lineHeight = 22.sp,
                        letterSpacing = (-0.44).sp,
                    ),
                    color = DisciplineScoreColor.colorFor(final),
                )
                val statusLabel = when (statusKey) {
                    DisciplineStatus.Key.Approved ->
                        stringResource(R.string.disciplines_past_status_approved)
                    DisciplineStatus.Key.Failed ->
                        stringResource(R.string.disciplines_past_status_failed)
                    DisciplineStatus.Key.Final ->
                        stringResource(R.string.disciplines_past_status_final)
                    // Past-semester rows shouldn't normally land in the
                    // ongoing/low/pending buckets, but when they do show the
                    // upstream label rather than guessing pass/fail.
                    else -> discipline.status.label
                }
                val statusColor = when (statusKey) {
                    DisciplineStatus.Key.Approved -> DisciplineScoreColor.excellent()
                    DisciplineStatus.Key.Failed -> DisciplineScoreColor.danger()
                    DisciplineStatus.Key.Final -> DisciplineScoreColor.caution()
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = statusLabel.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                    ),
                    color = statusColor,
                )
            }
        }
    }
    Spacer(Modifier.height(0.dp))
}

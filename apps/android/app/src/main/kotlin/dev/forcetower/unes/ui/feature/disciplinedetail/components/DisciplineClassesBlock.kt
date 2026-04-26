package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.ClassEntry
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import java.util.Locale

// Vertical timeline of registered classes. Past items are dimmed; the next
// upcoming class is highlighted with a color wash and ring. Mirrors iOS
// `DisciplineClassesBlock`.
@Composable
internal fun DisciplineClassesBlock(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val classes = discipline.classes
    val nextIndex = classes.indexOfFirst { it.isNext }.takeIf { it >= 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 18.dp),
    ) {
        DisciplineSectionHeader(title = stringResource(R.string.discipline_detail_classes_title)) {
            if (classes.isNotEmpty()) {
                Text(
                    text = pluralStringResource(R.plurals.discipline_detail_classes_count, classes.size, classes.size)
                        .uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        if (classes.isEmpty()) {
            EmptyClassesCard()
        } else {
            Timeline(
                classes = classes,
                accent = discipline.color,
                nextIndex = nextIndex,
            )
        }
    }
}

@Composable
private fun EmptyClassesCard() {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.discipline_detail_classes_empty),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val RailWidth = 22.dp

@Composable
private fun Timeline(
    classes: List<ClassEntry>,
    accent: Color,
    nextIndex: Int?,
) {
    // Inter-row spacing is baked into each row's bottom padding instead of
    // Arrangement.spacedBy so the per-row spine connectors can bridge the
    // gap between consecutive rows seamlessly.
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        classes.forEachIndexed { idx, entry ->
            ClassRow(
                entry = entry,
                accent = accent,
                isNext = idx == nextIndex,
                isFirst = idx == 0,
                isLast = idx == classes.size - 1,
            )
        }
    }
}

@Composable
private fun ClassRow(
    entry: ClassEntry,
    accent: Color,
    isNext: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    val rowShape = RoundedCornerShape(14.dp)
    val cardBg = if (isNext) accent.copy(alpha = 0.06f) else card
    val cardBorder = if (isNext) accent.copy(alpha = 0.33f) else cardLine

    val line = MaterialTheme.melon.surface.line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = if (isLast) 0.dp else 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Dot column draws its own spine connector segment so a Column of
        // rows reproduces the iOS continuous spine without needing a single
        // background-painted line that has to know the parent's height.
        DotColumn(
            accent = accent,
            surface = surface,
            line = line,
            past = entry.past,
            isNext = isNext,
            isFirst = isFirst,
            isLast = isLast,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(rowShape)
                .background(cardBg)
                .border(1.dp, cardBorder, rowShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (isNext) {
                Text(
                    text = stringResource(R.string.discipline_detail_classes_next_eyebrow),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.26.sp,
                    ),
                    color = accent,
                )
            }
            Text(
                text = entry.title.ifEmpty { stringResource(R.string.discipline_detail_classes_no_title) },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.15).sp,
                ),
                color = if (entry.past) ink2 else ink,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.date ?: stringResource(R.string.discipline_detail_classes_no_date),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    ),
                    color = ink4,
                )
                val attCount = entry.attachments
                if (attCount != null && attCount > 0) {
                    Text(
                        text = stringResource(R.string.discipline_detail_classes_separator),
                        color = ink4.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PaperclipIcon(tint = ink4)
                        Text(
                            text = attCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            ),
                            color = ink4,
                        )
                    }
                }
            }
        }
    }
}

// Draws the dot for this row plus the spine connector segments above and
// below. The Canvas spans the row's intrinsic height (the parent Row uses
// IntrinsicSize.Min), so connectors automatically span from edge to edge —
// the next row's top connector continues the line seamlessly.
@Composable
private fun DotColumn(
    accent: Color,
    surface: Color,
    line: Color,
    past: Boolean,
    isNext: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
) {
    Canvas(
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight(),
    ) {
        val centerX = size.width / 2f
        val dotCenterY = (12.dp.toPx() + 10.5.dp.toPx())
        val dotRadius = 6.5.dp.toPx() - 1.dp.toPx()
        val spineStrokePx = 1.dp.toPx()

        if (!isFirst) {
            drawLine(
                color = line,
                start = Offset(centerX, 0f),
                end = Offset(centerX, dotCenterY - dotRadius),
                strokeWidth = spineStrokePx,
            )
        }
        if (!isLast) {
            drawLine(
                color = line,
                start = Offset(centerX, dotCenterY + dotRadius),
                end = Offset(centerX, size.height),
                strokeWidth = spineStrokePx,
            )
        }

        // Halo behind the dot for the "PRÓXIMA AULA" row.
        if (isNext) {
            drawCircle(
                color = accent.copy(alpha = 0.13f),
                radius = 10.5.dp.toPx(),
                center = Offset(centerX, dotCenterY),
            )
        }
        // Dot fill.
        drawCircle(
            color = if (past) accent else surface,
            radius = dotRadius,
            center = Offset(centerX, dotCenterY),
        )
        // Dot stroke.
        drawCircle(
            color = if (past || isNext) accent else line,
            radius = dotRadius,
            center = Offset(centerX, dotCenterY),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun PaperclipIcon(tint: Color) {
    Canvas(modifier = Modifier.size(10.dp)) {
        val strokePx = 1.dp.toPx()
        val path = Path().apply {
            // Approximation of the iOS paperclip path — a single curved hook.
            moveTo(size.width * 0.65f, size.height * 0.30f)
            lineTo(size.width * 0.35f, size.height * 0.60f)
            cubicTo(
                size.width * 0.30f, size.height * 0.75f,
                size.width * 0.55f, size.height * 0.95f,
                size.width * 0.70f, size.height * 0.80f,
            )
            lineTo(size.width * 0.95f, size.height * 0.55f)
            cubicTo(
                size.width * 1.10f, size.height * 0.30f,
                size.width * 0.75f, size.height * -0.10f,
                size.width * 0.50f, size.height * 0.05f,
            )
            lineTo(size.width * 0.14f, size.height * 0.39f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}


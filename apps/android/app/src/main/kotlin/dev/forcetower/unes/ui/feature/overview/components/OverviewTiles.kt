package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.OverviewAttendanceTileData
import dev.forcetower.unes.ui.feature.overview.OverviewGradeTileData
import dev.forcetower.unes.ui.feature.overview.OverviewMessagesTileData
import dev.forcetower.unes.ui.feature.overview.OverviewNextTestTileData
import androidx.compose.ui.platform.LocalResources

private val TileShape = RoundedCornerShape(22.dp)
private val TileMinHeight = 150.dp

@Composable
internal fun OverviewTileGrid(
    grade: OverviewGradeTileData?,
    messages: OverviewMessagesTileData?,
    nextTest: OverviewNextTestTileData?,
    attendance: OverviewAttendanceTileData?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradeTile(data = grade, modifier = Modifier.weight(1f))
            MessagesTile(data = messages, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TestsTile(data = nextTest, modifier = Modifier.weight(1f))
            StreakTile(data = attendance, modifier = Modifier.weight(1f))
        }
    }
}

// ───────── Shared shells ─────────

@Composable
private fun TileShell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TileMinHeight)
            .clip(TileShape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.cardLine, TileShape)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun TileEyebrow(label: String, color: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp,
            letterSpacing = 1.33.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = color,
    )
}

// ───────── Grade ─────────

@Composable
private fun GradeTile(data: OverviewGradeTileData?, modifier: Modifier = Modifier) {
    TileShell(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            TileEyebrow(
                label = stringResource(R.string.overview_tile_grade_eyebrow),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (data != null) {
                GradeValue(value = data.value)
                Spacer(Modifier.height(4.dp))
                if (data.deltaLabel != null && data.comparisonSemester != null) {
                    GradeDelta(
                        delta = data.deltaLabel,
                        comparisonSemester = data.comparisonSemester,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.overview_tile_grade_empty_value),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 52.sp,
                        lineHeight = 52.sp,
                        letterSpacing = (-1.56).sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.overview_tile_grade_empty_caption),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GradeValue(value: Double) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val scaled = ((value * 10).toInt())
    val whole = (scaled / 10).toString()
    val tenth = (scaled % 10).toString()
    val separator = stringResource(R.string.overview_tile_grade_decimal_separator)
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append(whole) }
        withStyle(SpanStyle(color = ink4)) { append(separator) }
        withStyle(SpanStyle(color = ink)) { append(tenth) }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 52.sp,
            lineHeight = 52.sp,
            letterSpacing = (-1.56).sp,
            fontWeight = FontWeight.Normal,
        ),
    )
}

@Composable
private fun GradeDelta(delta: String, comparisonSemester: String) {
    val successText = SuccessText
    val successIcon = SuccessIcon
    Row(verticalAlignment = Alignment.CenterVertically) {
        TrendUpGlyph(color = successIcon)
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.overview_tile_grade_delta_format, delta),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = successText,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.overview_tile_grade_comparison_format, comparisonSemester),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrendUpGlyph(color: Color) {
    Canvas(modifier = Modifier.size(10.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.7f)
            lineTo(w * 0.5f, h * 0.4f)
            lineTo(w * 0.8f, h * 0.7f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 1.5f * density,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

// ───────── Messages (always-dark mesh card) ─────────

@Composable
private fun MessagesTile(data: OverviewMessagesTileData?, modifier: Modifier = Modifier) {
    val alwaysDark = MaterialTheme.melon.brand.alwaysDarkBg
    val onAlwaysDark = Color(0xFFFBF7F2)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TileMinHeight)
            .clip(TileShape)
            .background(alwaysDark),
    ) {
        Mesh(
            variant = MeshVariant.Rose,
            intensity = 0.75f,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            TileEyebrow(
                label = stringResource(R.string.overview_tile_messages_eyebrow),
                color = onAlwaysDark.copy(alpha = 0.7f),
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${data?.unreadCount ?: 0}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 48.sp,
                        lineHeight = 48.sp,
                        letterSpacing = (-1.44).sp,
                    ),
                    color = onAlwaysDark,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.overview_tile_messages_unread_caption),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = onAlwaysDark.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = previewLine(data),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = onAlwaysDark.copy(alpha = 0.75f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun previewLine(data: OverviewMessagesTileData?): String {
    if (data == null) return stringResource(R.string.overview_tile_messages_empty_preview)
    val sender = data.lastSender
    val preview = data.lastPreview
    return when {
        sender != null && preview != null ->
            stringResource(R.string.overview_tile_messages_preview_full, sender, preview)
        sender != null -> sender
        preview != null -> preview
        else -> stringResource(R.string.overview_tile_messages_empty_preview)
    }
}

// ───────── Tests ─────────

@Composable
private fun TestsTile(data: OverviewNextTestTileData?, modifier: Modifier = Modifier) {
    TileShell(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            TileEyebrow(
                label = stringResource(R.string.overview_tile_tests_eyebrow),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (data != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${data.daysUntil}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 48.sp,
                            lineHeight = 48.sp,
                            letterSpacing = (-0.96).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = LocalResources.current.getQuantityString(
                            R.plurals.overview_tile_tests_days_unit,
                            data.daysUntil,
                        ),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.overview_tile_tests_subject_format,
                        data.disciplineName,
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = data.dateLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.overview_tile_tests_empty_value),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 48.sp,
                        lineHeight = 48.sp,
                        letterSpacing = (-0.96).sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.overview_tile_tests_empty_caption),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ───────── Streak (frequência) ─────────

@Composable
private fun StreakTile(data: OverviewAttendanceTileData?, modifier: Modifier = Modifier) {
    TileShell(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            TileEyebrow(
                label = stringResource(R.string.overview_tile_attendance_eyebrow),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            PercentageLabel(percent = data?.percentage)
            Spacer(Modifier.height(10.dp))
            DaysStrip(data = data)
            Spacer(Modifier.height(4.dp))
            Text(
                text = footer(data),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PercentageLabel(percent: Int?) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    if (percent != null) {
        val text: AnnotatedString = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = ink,
                    fontSize = 32.sp,
                ),
            ) { append(stringResource(R.string.overview_tile_attendance_percent_format, percent)) }
            withStyle(
                SpanStyle(
                    color = ink3,
                    fontSize = 18.sp,
                ),
            ) { append(stringResource(R.string.overview_tile_attendance_percent_suffix)) }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.64).sp,
                fontWeight = FontWeight.Normal,
            ),
        )
    } else {
        Text(
            text = stringResource(R.string.overview_tile_grade_empty_value),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.64).sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun DaysStrip(data: OverviewAttendanceTileData?) {
    val period = data?.periodDays ?: 14
    val days = data?.days.orEmpty()
    val padded = if (days.size >= period) days.take(period) else days + List(period - days.size) { false }
    val amber = MaterialTheme.melon.brand.amber
    val surface3 = MaterialTheme.colorScheme.surfaceContainerHigh

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        padded.forEachIndexed { index, present ->
            val color = if (present) {
                amber.copy(alpha = (0.4f + (index.toFloat() / padded.size) * 0.6f).coerceIn(0f, 1f))
            } else {
                surface3
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun footer(data: OverviewAttendanceTileData?): String {
    val period = data?.periodDays ?: 14
    val allowed = data?.allowedAbsences ?: 0
    val allowedText = LocalResources.current.getQuantityString(
        R.plurals.overview_tile_attendance_allowed,
        allowed,
        allowed,
    )
    return stringResource(R.string.overview_tile_attendance_footer_format, period, allowedText)
}

// Success greens for grade delta. Same hex used in iOS `OverviewFixtures` —
// not yet promoted to the design-system palette because they're scoped to
// this single tile; promote when a second screen needs them.
private val SuccessText = Color(0xFF2F6B48)
private val SuccessIcon = Color(0xFF4AA679)

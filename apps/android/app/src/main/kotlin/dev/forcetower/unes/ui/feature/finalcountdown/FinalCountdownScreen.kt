package dev.forcetower.unes.ui.feature.finalcountdown

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCBreakdown
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCGradeRow
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCStar
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCVerdictHero

// "Final Countdown" — calculadora de notas. Fixture-driven on both platforms:
// state lives entirely in the screen, no KMP wiring. Mirrors iOS
// `FinalCountdownView` layout: warm mesh halo at the top, FCHeader, then a
// vertical stack of (verdict hero / breakdown / weighted toggle / editable
// rows / clear button / legend / signature).
@Composable
internal fun FinalCountdownScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    var rows by rememberSaveable(stateSaver = FCRowsSaver) {
        mutableStateOf(FinalCountdownFixtures.borderline)
    }
    var weighted by rememberSaveable { mutableStateOf(false) }

    val verdict = remember(rows, weighted) {
        FinalCountdownMath.verdict(rows = rows, weighted = weighted)
    }

    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Warm mesh backdrop fading into the surface.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        ) {
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
                            0.4f to Color.Transparent,
                            1f to surface,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            FCHeader(
                onBack = onBack,
                modifier = Modifier.fadeUpOnAppear(delayMs = 20),
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FCVerdictHero(
                    verdict = verdict,
                    weighted = weighted,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 140),
                )

                FCBreakdown(
                    rows = rows,
                    weighted = weighted,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 220),
                )

                FCWeightedToggle(
                    weighted = weighted,
                    onToggle = { weighted = !weighted },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 280),
                )

                EvaluationsHeader(
                    count = rows.size,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 320),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fadeUpOnAppear(delayMs = 380),
                ) {
                    rows.forEachIndexed { index, row ->
                        FCGradeRow(
                            row = row,
                            weighted = weighted,
                            canRemove = rows.size > 1,
                            onUpdate = { next ->
                                rows = rows.toMutableList().also { it[index] = next }
                            },
                            onRemove = {
                                rows = rows.toMutableList().also { it.removeAt(index) }
                            },
                        )
                    }
                    AddRowButton(onClick = {
                        rows = rows + FCRow(label = "AV${rows.size + 1}")
                    })
                }

                ClearButton(
                    onClick = {
                        rows = FinalCountdownFixtures.defaultRows
                        weighted = false
                    },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 440),
                )

                LegendFooter(modifier = Modifier.fadeUpOnAppear(delayMs = 500))

                Signature(modifier = Modifier.padding(top = 14.dp))

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FCHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BackChevronButton(onBack = onBack, card = card, cardLine = cardLine, ink = ink)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Text(
                    text = stringResource(R.string.final_countdown_header_status).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 1.33.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = ink4,
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.final_countdown_header_eyebrow).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink3,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "Dá pra" italic + accent / " passar?" upright + ink. Mirrors iOS
        // `FCHeader`'s composed Text, achieved here via AnnotatedString spans.
        val title = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = accent,
                    fontStyle = FontStyle.Italic,
                ),
            ) {
                append(stringResource(R.string.final_countdown_header_title_accent))
            }
            withStyle(SpanStyle(color = ink)) {
                append(" ")
                append(stringResource(R.string.final_countdown_header_title_rest))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 38.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.76).sp,
            ),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.final_countdown_header_body),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = ink3,
            modifier = Modifier.fillMaxWidth(0.85f),
        )
    }
}

@Composable
private fun BackChevronButton(
    onBack: () -> Unit,
    card: Color,
    cardLine: Color,
    ink: Color,
) {
    val backLabel = stringResource(R.string.final_countdown_back)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(card)
            .border(1.dp, cardLine, CircleShape)
            .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(17.dp)) {
            val s = size.minDimension / 18f
            val stroke = Stroke(
                width = 1.6f * s,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val path = Path().apply {
                moveTo(11f * s, 4f * s)
                lineTo(6f * s, 9f * s)
                lineTo(11f * s, 14f * s)
            }
            drawPath(path = path, color = ink, style = stroke)
        }
    }
}

@Composable
private fun FCWeightedToggle(
    weighted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val surface = MaterialTheme.colorScheme.surface
    val surface3 = MaterialTheme.colorScheme.surfaceContainerHigh

    val background = if (weighted) ink else card
    val foreground = if (weighted) surface else ink
    val borderColor = if (weighted) ink else cardLine
    val knobTrack = if (weighted) surface else surface3
    val knobFill = if (weighted) ink else surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToggleKnob(active = weighted, track = knobTrack, fill = knobFill)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.final_countdown_weighted_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = foreground,
            )
            Text(
                text = stringResource(
                    if (weighted) R.string.final_countdown_weighted_hint_on
                    else R.string.final_countdown_weighted_hint_off,
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.72.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = foreground.copy(alpha = 0.65f),
            )
        }
        ScaleGlyph(color = foreground)
    }
}

@Composable
private fun ToggleKnob(active: Boolean, track: Color, fill: Color) {
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(fill)
                .align(if (active) Alignment.CenterEnd else Alignment.CenterStart),
        )
    }
}

@Composable
private fun ScaleGlyph(color: Color) {
    Canvas(modifier = Modifier.size(15.dp)) {
        val s = size.minDimension / 18f
        val stroke = Stroke(width = 1.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(9f * s, 3f * s); lineTo(9f * s, 16f * s)
            moveTo(3f * s, 16f * s); lineTo(15f * s, 16f * s)
            moveTo(6f * s, 7f * s); lineTo(3f * s, 12f * s); lineTo(9f * s, 12f * s); close()
            moveTo(12f * s, 7f * s); lineTo(9f * s, 12f * s); lineTo(15f * s, 12f * s); close()
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

@Composable
private fun EvaluationsHeader(count: Int, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val amber = MaterialTheme.melon.brand.amber

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.final_countdown_evaluations_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink3,
            )
            val title = buildAnnotatedString {
                withStyle(SpanStyle(color = ink)) {
                    append(stringResource(R.string.final_countdown_evaluations_title_prefix))
                    append(" ")
                }
                withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Italic)) {
                    append(stringResource(R.string.final_countdown_evaluations_title_accent))
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.3).sp,
                ),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FCStar(color = amber, modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.final_countdown_wildcard_label).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.72.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink4,
            )
        }
    }
}

@Composable
private fun AddRowButton(onClick: () -> Unit) {
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val line = MaterialTheme.melon.surface.line
    val label = stringResource(R.string.final_countdown_add_row)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                    ),
                )
                drawRoundRect(
                    color = line,
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                )
            }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(13.dp)) {
            val s = size.minDimension / 18f
            val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = Path().apply {
                moveTo(9f * s, 3f * s); lineTo(9f * s, 15f * s)
                moveTo(3f * s, 9f * s); lineTo(15f * s, 9f * s)
            }
            drawPath(path = path, color = ink3, style = stroke)
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink2.copy(alpha = 0.78f),
        )
    }
}

@Composable
private fun ClearButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink2 = MaterialTheme.colorScheme.onSurface
    val label = stringResource(R.string.final_countdown_clear)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink2,
        )
    }
}

@Composable
private fun LegendFooter(modifier: Modifier = Modifier) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val amber = MaterialTheme.melon.brand.amber

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(amber.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(13.dp)) {
                val s = size.minDimension / 18f
                val stroke = Stroke(
                    width = 1.4f * s,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                drawCircle(
                    color = amber,
                    radius = 6.5f * s,
                    center = Offset(9f * s, 9f * s),
                    style = stroke,
                )
                val path = Path().apply {
                    moveTo(9f * s, 8.5f * s); lineTo(9f * s, 12.5f * s)
                    moveTo(9f * s, 6f * s); lineTo(9f * s, 6.3f * s)
                }
                drawPath(path = path, color = amber, style = stroke)
            }
        }
        val text = buildAnnotatedString {
            append(stringResource(R.string.final_countdown_legend_intro))
            append(" ")
            withStyle(SpanStyle(color = ink, fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.final_countdown_legend_pass_value))
            }
            append(stringResource(R.string.final_countdown_legend_middle))
            withStyle(SpanStyle(color = ink, fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.final_countdown_legend_final_band))
            }
            append(stringResource(R.string.final_countdown_legend_after_final))
            withStyle(SpanStyle(color = ink, fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.final_countdown_legend_fail_band))
            }
            append(stringResource(R.string.final_countdown_legend_outro))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
            ),
            color = ink3,
        )
    }
}

@Composable
private fun Signature(modifier: Modifier = Modifier) {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = stringResource(R.string.final_countdown_signature_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-0.08).sp,
            ),
            color = ink3,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.final_countdown_signature_caption).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.26.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink4,
            textAlign = TextAlign.Center,
        )
    }
}

// Saver for the rows list — flattens each FCRow to a 5-tuple list so
// rememberSaveable can stash the calculator state across config changes
// without requiring `@Parcelize`.
private val FCRowsSaver = androidx.compose.runtime.saveable.listSaver<List<FCRow>, Any?>(
    save = { rows ->
        rows.flatMap { row ->
            listOf(row.id, row.label, row.score, row.weight, row.wildcard)
        }
    },
    restore = { items ->
        items.chunked(5).map { chunk ->
            FCRow(
                id = chunk[0] as String,
                label = chunk[1] as String,
                score = chunk[2] as Double?,
                weight = (chunk[3] as Number).toInt(),
                wildcard = chunk[4] as Boolean,
            )
        }
    },
)


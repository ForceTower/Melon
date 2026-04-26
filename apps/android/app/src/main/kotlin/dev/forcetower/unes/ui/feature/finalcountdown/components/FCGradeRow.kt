package dev.forcetower.unes.ui.feature.finalcountdown.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.FCRow
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownMath
import androidx.compose.foundation.Canvas

// Editable evaluation row in the "suas avaliações" list. Label (editable,
// 5-char cap), serif score field accepting comma-decimal 0–10, optional
// weight stepper (weighted mode only), and a trailing delete button when
// more than one row exists. Mirrors iOS `FCGradeRow`.
@Composable
internal fun FCGradeRow(
    row: FCRow,
    weighted: Boolean,
    canRemove: Boolean,
    onUpdate: (FCRow) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LabelBlock(
            value = row.label,
            onChange = { next -> onUpdate(row.copy(label = next.take(5))) },
            ink = ink,
            ink4 = ink4,
        )

        ScoreField(
            row = row,
            onUpdate = onUpdate,
            ink = ink,
            surface2 = surface2,
            line = line,
            accent = accent,
            modifier = Modifier.weight(1f),
        )

        if (weighted) {
            WeightStepper(
                weight = row.weight,
                onChange = { w -> onUpdate(row.copy(weight = w)) },
                surface2 = surface2,
                line = line,
                ink = ink,
                ink3 = ink3,
            )
        }

        if (canRemove) {
            val removeLabel = stringResource(R.string.final_countdown_row_remove)
            Box(
                modifier = Modifier
                    .size(width = 22.dp, height = 28.dp)
                    .clickable(role = Role.Button, onClickLabel = removeLabel, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                XGlyph(color = ink4, size = 11.dp)
            }
        }
    }
}

@Composable
private fun LabelBlock(
    value: String,
    onChange: (String) -> Unit,
    ink: androidx.compose.ui.graphics.Color,
    ink4: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier.width(42.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 11.sp,
                lineHeight = 13.sp,
                letterSpacing = 0.22.sp,
                color = ink,
            ),
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.final_countdown_row_label_caption).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 0.64.sp,
            ),
            color = ink4,
        )
    }
}

@Composable
private fun ScoreField(
    row: FCRow,
    onUpdate: (FCRow) -> Unit,
    ink: androidx.compose.ui.graphics.Color,
    surface2: androidx.compose.ui.graphics.Color,
    line: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val display = row.score?.let { FinalCountdownMath.formatGrade(it) } ?: ""
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val borderColor = if (focused) accent else line

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(surface2)
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = display,
            onValueChange = { raw -> onUpdate(row.copy(score = parseScore(raw))) },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
                color = ink,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (display.isEmpty()) {
                    Text(
                        text = stringResource(R.string.final_countdown_row_score_placeholder),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            lineHeight = 22.sp,
                            letterSpacing = (-0.2).sp,
                            textAlign = TextAlign.Center,
                        ),
                        color = ink4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun WeightStepper(
    weight: Int,
    onChange: (Int) -> Unit,
    surface2: androidx.compose.ui.graphics.Color,
    line: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
    ink3: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surface2)
            .border(1.dp, line, RoundedCornerShape(12.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StepButton(
            onClick = { onChange((weight - 1).coerceAtLeast(1)) },
            color = ink3,
            label = stringResource(R.string.final_countdown_weight_decrement),
        ) { c -> MinusGlyph(color = c, size = 14.dp) }
        Text(
            text = "×$weight",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            ),
            color = ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(22.dp),
        )
        StepButton(
            onClick = { onChange((weight + 1).coerceAtMost(9)) },
            color = ink3,
            label = stringResource(R.string.final_countdown_weight_increment),
        ) { c -> PlusGlyph(color = c, size = 14.dp) }
    }
}

@Composable
private fun StepButton(
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color,
    label: String,
    glyph: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        glyph(color)
    }
}

@Composable
private fun PlusGlyph(color: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = size.toPx() / 18f
        val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(9f * s, 3f * s); lineTo(9f * s, 15f * s)
            moveTo(3f * s, 9f * s); lineTo(15f * s, 9f * s)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

@Composable
private fun MinusGlyph(color: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = size.toPx() / 18f
        val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(3f * s, 9f * s); lineTo(15f * s, 9f * s)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

@Composable
private fun XGlyph(color: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = size.toPx() / 18f
        val stroke = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(4f * s, 4f * s); lineTo(14f * s, 14f * s)
            moveTo(14f * s, 4f * s); lineTo(4f * s, 14f * s)
        }
        drawPath(path = path, color = color, style = stroke)
    }
}

// Parse the user's keystrokes back into a 0..10 Double, or null when empty /
// unparseable. Accepts both `,` and `.` as the decimal separator and clamps
// out-of-range entries (matches iOS `scoreText` binding).
private fun parseScore(raw: String): Double? {
    if (raw.isBlank()) return null
    val cleaned = raw.replace(',', '.').filter { it == '.' || it.isDigit() }
    val parsed = cleaned.toDoubleOrNull() ?: return null
    return parsed.coerceIn(0.0, 10.0)
}

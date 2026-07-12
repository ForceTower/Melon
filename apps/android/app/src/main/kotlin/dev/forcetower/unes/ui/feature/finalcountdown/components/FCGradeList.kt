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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.FCRow
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownIntent

// Editable evaluation list — label + score inputs per row, the ×N weight
// stepper in weighted mode, a remove affordance, and the "Adicionar
// avaliação" footer. Mirrors the dc grade-list card.
@Composable
internal fun FCGradeList(
    rows: List<FCRow>,
    weighted: Boolean,
    onIntent: (FinalCountdownIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.melon.surface.line
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, line, shape),
    ) {
        rows.forEachIndexed { index, row ->
            GradeRow(
                row = row,
                weighted = weighted,
                canRemove = rows.size > 1,
                onIntent = onIntent,
            )
            if (index < rows.lastIndex) {
                HorizontalDivider(color = line)
            }
        }

        HorizontalDivider(color = line)
        val addLabel = stringResource(R.string.final_countdown_add_row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = addLabel) {
                    onIntent(FinalCountdownIntent.AddRow)
                }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = addLabel,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp,
                ),
                color = accent,
            )
        }
    }
}

@Composable
private fun GradeRow(
    row: FCRow,
    weighted: Boolean,
    canRemove: Boolean,
    onIntent: (FinalCountdownIntent) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        BasicTextField(
            value = row.label,
            onValueChange = { onIntent(FinalCountdownIntent.RowLabelChanged(row.id, it)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.15).sp,
                color = ink,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.width(46.dp),
        )

        ScoreField(
            row = row,
            onIntent = onIntent,
            modifier = Modifier.weight(1f),
        )

        if (weighted) {
            WeightStepper(row = row, onIntent = onIntent)
        }

        if (canRemove) {
            val removeLabel = stringResource(R.string.final_countdown_row_remove)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = removeLabel) {
                        onIntent(FinalCountdownIntent.RemoveRow(row.id))
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = removeLabel,
                    tint = ink4,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ScoreField(
    row: FCRow,
    onIntent: (FinalCountdownIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = if (focused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val textStyle = MaterialTheme.typography.headlineSmall.copy(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.44).sp,
        color = ink,
        textAlign = TextAlign.Center,
    )

    BasicTextField(
        value = row.scoreText,
        onValueChange = { onIntent(FinalCountdownIntent.RowScoreChanged(row.id, it)) },
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interaction,
        modifier = modifier,
        decorationBox = { innerField ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (row.scoreText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.final_countdown_row_score_placeholder),
                        style = textStyle,
                        color = ink4,
                    )
                }
                innerField()
            }
        },
    )
}

@Composable
private fun WeightStepper(
    row: FCRow,
    onIntent: (FinalCountdownIntent) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StepperButton(
            icon = Icons.Filled.Remove,
            label = stringResource(R.string.final_countdown_weight_decrement),
            tint = ink2,
            onClick = { onIntent(FinalCountdownIntent.RowWeightChanged(row.id, delta = -1)) },
        )
        Text(
            text = stringResource(R.string.final_countdown_weight_format, row.weight),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 24.dp),
        )
        StepperButton(
            icon = Icons.Filled.Add,
            label = stringResource(R.string.final_countdown_weight_increment),
            tint = ink2,
            onClick = { onIntent(FinalCountdownIntent.RowWeightChanged(row.id, delta = 1)) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
    }
}

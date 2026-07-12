package dev.forcetower.unes.ui.feature.finalcountdown.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.FCDiscipline
import dev.forcetower.unes.ui.feature.overview.ColorFor

// The attached-discipline card at the top of the calculator: colored code
// tile, name + teacher/semester caption, and the "Trocar" pill that opens the
// selector sheet. A null discipline renders the "modo livre" placeholder.
@Composable
internal fun FCDisciplineRow(
    discipline: FCDiscipline?,
    onChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        DisciplineTile(
            code = discipline?.code,
            color = discipline?.let { ColorFor.discipline(it.code) }
                ?: MaterialTheme.colorScheme.outline,
            size = 42.dp,
            cornerRadius = 12.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = discipline?.name ?: stringResource(R.string.final_countdown_free_name),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.32).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = discipline?.subLabel() ?: stringResource(R.string.final_countdown_free_sub),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        val changeLabel = stringResource(R.string.final_countdown_change)
        Text(
            text = changeLabel,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(role = Role.Button, onClickLabel = changeLabel, onClick = onChange)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

// "Trocar disciplina" sheet — modo livre first, then the current semester's
// disciplines, active pick marked with a filled check.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FCDisciplineSelectorSheet(
    choices: List<FCDiscipline>,
    activeOfferId: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.melon.surface.card,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.final_countdown_selector_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.38).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            val closeLabel = stringResource(R.string.final_countdown_selector_close)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable(role = Role.Button, onClickLabel = closeLabel, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = closeLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            SelectorRow(
                code = null,
                color = MaterialTheme.colorScheme.outline,
                name = stringResource(R.string.final_countdown_free_name),
                sub = stringResource(R.string.final_countdown_free_sub),
                active = activeOfferId == null,
                onClick = { onPick(null) },
            )
            choices.forEach { choice ->
                SelectorRow(
                    code = choice.code,
                    color = ColorFor.discipline(choice.code),
                    name = choice.name,
                    sub = choice.subLabel(),
                    active = choice.offerId == activeOfferId,
                    onClick = { onPick(choice.offerId) },
                )
            }
        }
    }
}

@Composable
private fun SelectorRow(
    code: String?,
    color: Color,
    name: String,
    sub: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        DisciplineTile(code = code, color = color, size = 40.dp, cornerRadius = 11.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.15).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        if (active) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// Colored badge: the discipline code, or the sparkles glyph for modo livre
// (iOS uses SF `sparkles` on the slate tile — `AutoAwesome` is its Material
// counterpart).
@Composable
private fun DisciplineTile(
    code: String?,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(6.dp, shape, spotColor = color, ambientColor = color)
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (code == null) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = code,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = if (code.length > 3) 8.5.sp else 13.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.melon.fixed.onHero,
                maxLines = 1,
            )
        }
    }
}

// "Adriana Matos · 2026.1" / "2026.1" when the teacher is unknown.
@Composable
private fun FCDiscipline.subLabel(): String = if (teacher != null) {
    stringResource(R.string.final_countdown_disc_sub_format, teacher, semesterLabel)
} else {
    semesterLabel
}

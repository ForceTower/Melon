package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.overview.OverviewDiscipline

@Composable
internal fun DisciplinesStrip(
    items: List<OverviewDiscipline>,
    semesterLabel: String,
    modifier: Modifier = Modifier,
    onOpen: (OverviewDiscipline) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.overview_disciplines_eyebrow, semesterLabel),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.44.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.overview_disciplines_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.22).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        ) {
            items(items, key = { it.code }) { item ->
                DisciplineCard(
                    item = item,
                    onClick = if (item.offerId != null) {
                        { onOpen(item) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun DisciplineCard(item: OverviewDiscipline, onClick: (() -> Unit)?) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val empty = stringResource(R.string.overview_disciplines_grade_empty)

    Box(
        modifier = Modifier
            .size(width = 142.dp, height = 168.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // Color glow in the top-right corner.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .size(90.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.12f)),
        )

        Column(
            modifier = Modifier
                .padding(14.dp),
        ) {
            CodeBadge(code = item.code, color = item.color)
            Spacer(Modifier.height(10.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 17.sp,
                    lineHeight = 19.sp,
                    letterSpacing = (-0.17).sp,
                ),
                color = ink,
                maxLines = 2,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.overview_disciplines_status_partial),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.08.sp,
                ),
                color = ink3,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.grade,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.52).sp,
                ),
                color = if (item.grade == empty) ink4 else ink,
            )
        }
    }
}

@Composable
private fun CodeBadge(code: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.9.sp,
            ),
            color = color,
        )
    }
}

package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline

// Ementa (syllabus) block. Collapsible when the text is long. Mirrors iOS
// `DisciplineEmentaBlock` — same 160-char threshold, same color rail.
@Composable
internal fun DisciplineEmentaBlock(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val ementa = discipline.ementa?.takeIf { it.isNotEmpty() } ?: return
    val long = ementa.length > 160
    var expanded by remember(ementa) { mutableStateOf(false) }
    val shown = if (expanded || !long) ementa else ementa.take(160) + "…"

    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink2 = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 18.dp),
    ) {
        DisciplineSectionHeader(title = stringResource(R.string.discipline_detail_ementa_title))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(shape)
                .background(card)
                .border(1.dp, cardLine, shape),
        ) {
            // 3dp accent rail on the left edge — fills the card's height via
            // IntrinsicSize.Min on the Row, clipped by the parent shape so the
            // rounded corner stays clean.
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(discipline.color),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = shown,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    ),
                    color = ink2,
                )
                if (long) {
                    Text(
                        text = if (expanded) {
                            stringResource(R.string.discipline_detail_ementa_show_less)
                        } else {
                            stringResource(R.string.discipline_detail_ementa_show_more)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = discipline.color,
                        modifier = Modifier.clickable { expanded = !expanded },
                    )
                }
            }
        }
    }
}

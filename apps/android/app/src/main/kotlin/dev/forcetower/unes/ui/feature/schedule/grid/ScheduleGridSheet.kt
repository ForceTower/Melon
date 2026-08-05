package dev.forcetower.unes.ui.feature.schedule.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.LocalMelonDarkTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.schedule.ScheduleClass
import dev.forcetower.unes.ui.feature.schedule.components.formatHourLabel
import dev.forcetower.unes.ui.feature.schedule.durationMin
import dev.forcetower.unes.ui.feature.schedule.formatShortDayMonth
import dev.forcetower.unes.ui.feature.schedule.formatShortDayName
import java.util.Locale

// M3 detail sheet for a grid block / agenda row tap (dc `ScheduleGridScreen`
// sheet): code chip + weekday caption, the discipline name, a tonal list of
// Horário / Local / Professor (+ Conteúdo when the lecture has a subject),
// and the "Ver disciplina" + "Fechar" button pair — everything tinted with
// the discipline hue.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleGridSheet(
    target: GridSheetTarget,
    state: GridClassState,
    dateIso: String?,
    onOpenDiscipline: (ScheduleClass) -> Unit,
    onDismiss: () -> Unit,
) {
    val cls = target.cls
    val dark = LocalMelonDarkTheme.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                CodeChip(code = cls.code, color = cls.color)
                val caption = listOf(formatShortDayName(dateIso), formatShortDayMonth(dateIso))
                    .filter { it.isNotEmpty() }
                    .joinToString(" · ")
                if (caption.isNotEmpty()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (state == GridClassState.Now) {
                    Text(
                        text = stringResource(R.string.schedule_grid_now_badge)
                            .uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                        ),
                        color = cls.color,
                    )
                }
            }
            Text(
                text = cls.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.48).sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 9.dp),
            )

            val listShape = RoundedCornerShape(22.dp)
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp)
                    .fillMaxWidth()
                    .clip(listShape)
                    .background(
                        cls.color
                            .copy(alpha = if (dark) 0.12f else 0.08f)
                            .compositeOver(MaterialTheme.melon.surface.card),
                    )
                    .border(1.dp, cls.color.copy(alpha = 0.22f), listShape)
                    .padding(vertical = 4.dp),
            ) {
                DetailRow(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.schedule_grid_sheet_time_label),
                    value = stringResource(
                        R.string.schedule_grid_sheet_time_format,
                        cls.start,
                        cls.end,
                        formatHourLabel(cls.durationMin),
                    ),
                    tint = cls.color,
                )
                val location = listOfNotNull(cls.modulo, cls.room, cls.campus).joinToString(" · ")
                DetailRow(
                    icon = Icons.Outlined.LocationOn,
                    label = stringResource(R.string.schedule_grid_sheet_location_label),
                    value = location.ifEmpty { stringResource(R.string.schedule_location_unknown) },
                    tint = cls.color,
                )
                if (cls.prof.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Outlined.Person,
                        label = stringResource(R.string.schedule_grid_sheet_teacher_label),
                        value = cls.prof,
                        tint = cls.color,
                    )
                }
                cls.topic?.let { topic ->
                    DetailRow(
                        icon = Icons.Outlined.EditNote,
                        label = stringResource(R.string.schedule_grid_sheet_topic_label),
                        value = topic,
                        tint = cls.color,
                    )
                }
            }

            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Turma detail needs the offer id, which fixture/pre-sync rows
                // don't carry — the tonal close button then stands alone.
                if (cls.offerId != null) {
                    SheetButton(
                        label = stringResource(R.string.schedule_grid_sheet_open_discipline),
                        container = cls.color,
                        content = MaterialTheme.melon.fixed.onHero,
                        onClick = {
                            onDismiss()
                            onOpenDiscipline(cls)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                SheetButton(
                    label = stringResource(R.string.schedule_grid_sheet_close),
                    container = cls.color.copy(alpha = 0.16f),
                    content = cls.color,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CodeChip(code: String, color: Color) {
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.20f))
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.48.sp,
            ),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SheetButton(
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = container,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = content,
            )
        }
    }
}

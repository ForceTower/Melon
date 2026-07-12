package dev.forcetower.unes.ui.feature.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentSection
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentShift
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentClash
import dev.forcetower.unes.ui.feature.enrollment.EnrollmentFormat
import dev.forcetower.unes.ui.feature.enrollment.hasSchedule
import dev.forcetower.unes.ui.feature.enrollment.scheduleLines
import dev.forcetower.unes.ui.feature.enrollment.seats

// Turma picker card (dc `MatriculaScreen` discipline view): header with the
// section label, shift chip, preferential dot and the seat meter; schedule
// lines; session rows; conflict/lotada notices; and the full-width footer
// action that selects/removes/queues.
@Composable
internal fun EnrollmentSectionCard(
    section: EnrollmentSection,
    hue: Color,
    isSelected: Boolean,
    clash: EnrollmentClash?,
    useQueue: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    // Comprovante mode: the card is informational only — no footer action.
    readonly: Boolean = false,
) {
    val seats = section.seats
    val blocked = clash != null && !isSelected
    val noSchedule = !section.hasSchedule
    val status = MaterialTheme.melon.status
    val seatColor = when {
        seats.isFull -> status.bad
        seats.isTight -> status.warn
        else -> status.ok
    }
    val shape = RoundedCornerShape(20.dp)
    val borderColor = when {
        isSelected -> hue
        blocked -> status.bad.copy(alpha = 0.3f)
        else -> MaterialTheme.melon.surface.line
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, borderColor, shape),
    ) {
        Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 14.dp, bottom = 13.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.36).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                ShiftChip(shift = section.meetings.firstOrNull()?.shift ?: EnrollmentShift.Undefined, hue = hue, muted = noSchedule)
                if (section.coursePreferential) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(status.ok),
                    )
                }
                Box(modifier = Modifier.weight(1f))
                SeatMeter(filled = seats.filled, total = seats.total, fraction = seats.fraction, color = seatColor, label = seatLabel(seats.isFull, seats.isTight))
            }

            Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (noSchedule) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = stringResource(R.string.enrollment_schedule_tbd),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                } else {
                    scheduleLines(section).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(
                                text = line.days,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = line.time,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                section.meetings.forEach { meeting ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = meeting.kind,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = buildString {
                                append(meeting.room ?: stringResource(R.string.enrollment_room_tbd))
                                append(" · ")
                                append(
                                    if (meeting.professors.isEmpty()) stringResource(R.string.enrollment_prof_tbd)
                                    else meeting.professors.joinToString(", "),
                                )
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (blocked && clash != null) {
                SectionNotice(
                    hue = status.bad,
                    icon = Icons.Filled.Warning,
                    title = stringResource(R.string.enrollment_conflict_title),
                    text = stringResource(
                        R.string.enrollment_conflict_text_format,
                        clash.discipline.code,
                        clash.section.label,
                        EnrollmentFormat.dayFull(clash.day),
                    ),
                )
            }
            if (seats.isFull && !blocked && !isSelected) {
                SectionNotice(
                    hue = status.warn,
                    icon = Icons.Filled.Group,
                    title = stringResource(R.string.enrollment_full_title),
                    text = when {
                        !useQueue -> stringResource(R.string.enrollment_full_no_queue)
                        section.waitlistCount > 0 ->
                            stringResource(R.string.enrollment_full_queue_ahead_format, section.waitlistCount)
                        else -> stringResource(R.string.enrollment_full_queue)
                    },
                )
            }
        }

        if (!readonly) {
            SectionFooterButton(
                isSelected = isSelected,
                blocked = blocked,
                full = seats.isFull,
                useQueue = useQueue,
                hue = hue,
                onTap = onTap,
            )
        }
    }
}

@Composable
private fun seatLabel(full: Boolean, tight: Boolean): String = stringResource(
    when {
        full -> R.string.enrollment_seats_full
        tight -> R.string.enrollment_seats_tight
        else -> R.string.enrollment_seats_open
    },
)

@Composable
private fun ShiftChip(shift: EnrollmentShift, hue: Color, muted: Boolean) {
    Text(
        text = stringResource(
            when (shift) {
                EnrollmentShift.Morning -> R.string.enrollment_shift_morning
                EnrollmentShift.Afternoon -> R.string.enrollment_shift_afternoon
                EnrollmentShift.Night -> R.string.enrollment_shift_night
                EnrollmentShift.Undefined -> R.string.enrollment_shift_undefined
            },
        ),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = if (muted) MaterialTheme.colorScheme.outline else hue,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (muted) MaterialTheme.colorScheme.surfaceContainer else hue.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun SeatMeter(filled: Int, total: Int, fraction: Float, color: Color, label: String) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = filled.toString(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.enrollment_seats_count_format, total),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Box(
            modifier = Modifier
                .width(76.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(76.dp * fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.54.sp,
            ),
            color = color,
        )
    }
}

@Composable
private fun SectionNotice(hue: Color, icon: ImageVector, title: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(hue.copy(alpha = 0.12f))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(hue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(12.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SectionFooterButton(
    isSelected: Boolean,
    blocked: Boolean,
    full: Boolean,
    useQueue: Boolean,
    hue: Color,
    onTap: () -> Unit,
) {
    val status = MaterialTheme.melon.status
    val inert = blocked || (full && !useQueue && !isSelected)
    val label: String
    val icon: ImageVector
    val background: Color
    val foreground: Color
    when {
        isSelected -> {
            label = stringResource(R.string.enrollment_section_selected)
            icon = Icons.Filled.Check
            background = hue
            foreground = MaterialTheme.melon.fixed.onHero
        }
        blocked -> {
            label = stringResource(R.string.enrollment_section_blocked)
            icon = Icons.Filled.Block
            background = MaterialTheme.colorScheme.surfaceContainer
            foreground = MaterialTheme.colorScheme.outlineVariant
        }
        full && useQueue -> {
            label = stringResource(R.string.enrollment_section_queue)
            icon = Icons.Filled.FormatListNumbered
            background = status.warn
            foreground = MaterialTheme.melon.fixed.onHero
        }
        full -> {
            label = stringResource(R.string.enrollment_section_full)
            icon = Icons.Filled.Block
            background = MaterialTheme.colorScheme.surfaceContainer
            foreground = MaterialTheme.colorScheme.outlineVariant
        }
        else -> {
            label = stringResource(R.string.enrollment_section_select)
            icon = Icons.Filled.Add
            background = MaterialTheme.colorScheme.surfaceContainer
            foreground = MaterialTheme.colorScheme.onBackground
        }
    }
    val line = MaterialTheme.melon.surface.line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .drawBehind {
                drawLine(
                    color = line,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .background(background)
            .then(
                if (inert) Modifier
                else Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onTap),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.14).sp,
            ),
            color = foreground,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

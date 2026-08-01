package dev.forcetower.unes.ui.feature.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.PersonalCategory
import dev.forcetower.unes.ui.feature.calendar.PersonalDiscipline
import dev.forcetower.unes.ui.feature.calendar.PersonalDisciplineOption
import dev.forcetower.unes.ui.feature.calendar.PersonalEntry
import dev.forcetower.unes.ui.feature.calendar.PersonalReminder
import dev.forcetower.unes.ui.feature.calendar.color
import dev.forcetower.unes.ui.feature.overview.ColorFor
import dev.forcetower.unes.ui.feature.calendar.icon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

// "Novo evento" / "Editar evento" composer: title, kind, dates, an optional
// class tag, a reminder and notes. Mirrors iOS `CalendarPersonalEventSheet`
// and the dc `CalUserFormSheet`.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalPersonalEventSheet(
    editing: PersonalEntry?,
    seedDay: LocalDate,
    disciplines: List<PersonalDisciplineOption>,
    onDismiss: () -> Unit,
    onSave: (PersonalEntry, isNew: Boolean) -> Unit,
    onDelete: (PersonalEntry) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isNew = editing == null

    var title by remember { mutableStateOf(editing?.title.orEmpty()) }
    var start by remember { mutableStateOf(editing?.start ?: seedDay) }
    var end by remember { mutableStateOf(editing?.end) }
    var category by remember { mutableStateOf(editing?.category ?: PersonalCategory.Task) }
    var discipline by remember { mutableStateOf(editing?.discipline) }
    var reminder by remember { mutableStateOf(editing?.reminder ?: PersonalReminder.DayBefore) }
    var notes by remember { mutableStateOf(editing?.notes.orEmpty()) }
    var picking by remember { mutableStateOf<DateTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val accent = category.category.color()
    val canSave = title.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
        ) {
            HeaderBar(
                isNew = isNew,
                canSave = canSave,
                onCancel = onDismiss,
                onSave = {
                    onSave(
                        PersonalEntry(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            start = start,
                            // A range that runs backwards collapses to a single day.
                            end = end?.takeIf { it.isAfter(start) },
                            category = category,
                            discipline = discipline,
                            reminder = reminder,
                            notes = notes.trim(),
                            createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                        ),
                        isNew,
                    )
                    onDismiss()
                },
            )

            TitleCard(title = title, accent = accent, category = category, onChange = { title = it })

            CategoryPicker(
                selected = category,
                onSelect = { category = it },
                modifier = Modifier.padding(top = 10.dp),
            )

            FormGroup(label = stringResource(R.string.calendar_personal_section_when)) {
                FormRow(
                    icon = Icons.Filled.CalendarMonth,
                    tint = MaterialTheme.melon.status.bad,
                    label = stringResource(R.string.calendar_personal_row_date),
                ) {
                    ValuePill(text = CalendarFormat.dateShort(start), onClick = { picking = DateTarget.Start })
                }
                FormRow(
                    icon = Icons.Filled.Schedule,
                    tint = MaterialTheme.melon.status.warn,
                    label = stringResource(R.string.calendar_personal_row_period),
                ) {
                    Switch(
                        checked = end != null,
                        onCheckedChange = { on -> end = if (on) start.plusDays(2) else null },
                    )
                }
                end?.let { current ->
                    FormRow(
                        icon = Icons.Filled.Schedule,
                        tint = MaterialTheme.melon.palette.magenta,
                        label = stringResource(R.string.calendar_personal_row_ends),
                    ) {
                        ValuePill(text = CalendarFormat.dateShort(current), onClick = { picking = DateTarget.End })
                    }
                }
            }

            FormGroup(label = stringResource(R.string.calendar_personal_section_discipline)) {
                DisciplinePicker(
                    options = disciplines,
                    selected = discipline,
                    onSelect = { discipline = it },
                )
            }

            FormGroup(label = stringResource(R.string.calendar_personal_section_reminder)) {
                ReminderPicker(selected = reminder, onSelect = { reminder = it })
            }

            FormGroup(label = stringResource(R.string.calendar_personal_section_notes)) {
                NotesField(notes = notes, onChange = { notes = it })
            }

            if (editing != null) {
                DeleteButton(onClick = { confirmDelete = true })
            }

            Text(
                text = stringResource(R.string.calendar_personal_privacy_note),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, start = 20.dp, end = 20.dp),
            )
        }
    }

    picking?.let { target ->
        val initial = if (target == DateTarget.Start) start else (end ?: start.plusDays(2))
        DatePickerSheet(
            initial = initial,
            // The end date can never precede the start.
            minimum = if (target == DateTarget.End) start.plusDays(1) else null,
            onDismiss = { picking = null },
            onPick = { picked ->
                if (target == DateTarget.Start) {
                    start = picked
                    end?.let { if (!it.isAfter(picked)) end = picked.plusDays(2) }
                } else {
                    end = picked
                }
                picking = null
            },
        )
    }

    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.calendar_personal_delete_confirm_title)) },
            text = { Text(editing.title) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete(editing)
                        onDismiss()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.calendar_personal_delete_action),
                        color = MaterialTheme.melon.status.bad,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.calendar_personal_cancel_action))
                }
            },
        )
    }
}

private enum class DateTarget { Start, End }

@Composable
private fun HeaderBar(
    isNew: Boolean,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel) {
            Text(
                text = stringResource(R.string.calendar_personal_cancel_action),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                if (isNew) R.string.calendar_personal_new_title else R.string.calendar_personal_edit_title,
            ),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSave, enabled = canSave) {
            Text(
                text = stringResource(
                    if (isNew) R.string.calendar_personal_add_action else R.string.calendar_personal_save_action,
                ),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TitleCard(
    title: String,
    accent: Color,
    category: PersonalCategory,
    onChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.melon.surface.card,
        border = BorderStroke(1.dp, MaterialTheme.melon.surface.line),
    ) {
        // The tile hugs the first line as the title wraps, so it doesn't drift
        // to the middle of a tall block. 11dp = the field's 16dp top padding
        // plus half a 24sp line, less half the 34dp tile. An empty field draws
        // no line to hug, so it centres instead.
        val pinned = title.isNotBlank()
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = if (pinned) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = if (pinned) 11.dp else 0.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.melon.fixed.onHero,
                    modifier = Modifier.size(17.dp),
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = onChange,
                placeholder = { Text(stringResource(R.string.calendar_personal_title_placeholder)) },
                singleLine = false,
                maxLines = 4,
                colors = transparentFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CategoryPicker(
    selected: PersonalCategory,
    onSelect: (PersonalCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        PersonalCategory.entries.forEach { option ->
            val accent = option.category.color()
            val isOn = selected == option
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (isOn) accent.copy(alpha = 0.12f) else MaterialTheme.melon.surface.card)
                    .border(
                        width = 1.dp,
                        color = if (isOn) accent else MaterialTheme.melon.surface.line,
                        shape = RoundedCornerShape(15.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = option.category.icon(),
                    contentDescription = null,
                    tint = if (isOn) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = stringResource(option.labelRes),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isOn) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DisciplinePicker(
    options: List<PersonalDisciplineOption>,
    selected: PersonalDiscipline?,
    onSelect: (PersonalDiscipline?) -> Unit,
) {
    if (options.isEmpty()) {
        Text(
            text = stringResource(R.string.calendar_personal_discipline_empty),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
        return
    }
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChipButton(
                label = stringResource(R.string.calendar_personal_discipline_none),
                isActive = selected == null,
                activeColor = MaterialTheme.colorScheme.onBackground,
                activeContent = MaterialTheme.colorScheme.background,
                onClick = { onSelect(null) },
            )
            options.forEach { option ->
                val tint = ColorFor.discipline(option.discipline.code)
                ChipButton(
                    label = option.discipline.code,
                    isActive = selected?.id == option.discipline.id,
                    activeColor = tint,
                    activeContent = MaterialTheme.melon.fixed.onHero,
                    dot = tint,
                    onClick = { onSelect(option.discipline) },
                )
            }
        }
        Text(
            text = stringResource(R.string.calendar_personal_discipline_hint),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun ChipButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    activeContent: Color,
    onClick: () -> Unit,
    dot: Color? = null,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) activeColor else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.melon.surface.line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dot != null && !isActive) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dot),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (isActive) activeContent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReminderPicker(selected: PersonalReminder, onSelect: (PersonalReminder) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        PersonalReminder.entries.forEach { option ->
            val isOn = selected == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isOn) MaterialTheme.melon.surface.card else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(option.shortLabelRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isOn) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NotesField(notes: String, onChange: (String) -> Unit) {
    // Same first-line pinning as the title card; 15dp against the 26dp tile.
    val pinned = notes.isNotBlank()
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = if (pinned) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = if (pinned) 15.dp else 0.dp)
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.melon.palette.teal),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(15.dp),
            )
        }
        OutlinedTextField(
            value = notes,
            onValueChange = onChange,
            placeholder = { Text(stringResource(R.string.calendar_personal_notes_placeholder)) },
            singleLine = false,
            maxLines = 6,
            keyboardOptions = KeyboardOptions.Default,
            colors = transparentFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.melon.surface.card,
        border = BorderStroke(1.dp, MaterialTheme.melon.surface.line),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.melon.status.bad,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.calendar_personal_delete_event),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.melon.status.bad,
            )
        }
    }
}

// Inset-grouped section: uppercase caption over one rounded card.
@Composable
private fun FormGroup(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 14.dp, bottom = 7.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.melon.surface.card,
            border = BorderStroke(1.dp, MaterialTheme.melon.surface.line),
        ) {
            Column { content() }
        }
    }
}

// One labelled row: tinted glyph tile, title, trailing control.
@Composable
private fun FormRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        control()
    }
}

@Composable
private fun ValuePill(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initial: LocalDate,
    minimum: LocalDate?,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onPick(if (minimum != null && picked.isBefore(minimum)) minimum else picked)
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text(stringResource(R.string.calendar_personal_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.calendar_personal_cancel_action))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun transparentFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
)

// The composer seeds "today" when opened from the app bar.
internal fun personalEventSeedDay(): LocalDate = CalendarMath.today

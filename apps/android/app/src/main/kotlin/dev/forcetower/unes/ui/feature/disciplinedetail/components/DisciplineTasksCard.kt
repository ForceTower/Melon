package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.calendar.CalendarFormat
import dev.forcetower.unes.ui.feature.calendar.CalendarMath
import dev.forcetower.unes.ui.feature.calendar.PersonalCategory
import dev.forcetower.unes.ui.feature.calendar.PersonalEntry
import dev.forcetower.unes.ui.feature.calendar.PersonalReminder
import dev.forcetower.unes.ui.feature.calendar.color
import dev.forcetower.unes.ui.feature.disciplinedetail.TaskDue
import dev.forcetower.unes.ui.feature.disciplinedetail.taskDue
import java.time.LocalDate

// The discipline's checklist: the student's own entries tagged with it,
// pending ones first. Tapping a row opens the composer; the ring toggles it.
@Composable
internal fun DisciplineTasksCard(
    tasks: List<PersonalEntry>,
    canAdd: Boolean,
    onToggle: (PersonalEntry) -> Unit,
    onOpen: (PersonalEntry) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val today = CalendarMath.today
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.cardLine, shape),
    ) {
        Summary(tasks = tasks, canAdd = canAdd, onAdd = onAdd)
        if (tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.discipline_tasks_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            )
        }
        tasks.forEach { task ->
            key(task.id) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.melon.surface.line),
                )
                TaskRow(
                    task = task,
                    due = taskDue(task, today),
                    onToggle = { onToggle(task) },
                    onOpen = { onOpen(task) },
                )
            }
        }
    }
}

@Composable
private fun Summary(
    tasks: List<PersonalEntry>,
    canAdd: Boolean,
    onAdd: () -> Unit,
) {
    val pending = tasks.count { !it.isCompleted }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when {
                pending > 0 -> pluralStringResource(R.plurals.discipline_tasks_pending, pending, pending)
                tasks.isNotEmpty() -> stringResource(R.string.discipline_tasks_all_done)
                else -> ""
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (canAdd) {
            Surface(
                onClick = onAdd,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.discipline_tasks_add),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: PersonalEntry,
    due: TaskDue,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val accent = task.category.category.color()
    val toggleLabel = stringResource(
        if (task.isCompleted) R.string.personal_task_mark_pending else R.string.personal_task_mark_done,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 8.dp, end = 18.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .toggleable(value = task.isCompleted, role = Role.Checkbox, onValueChange = { onToggle() })
                .semantics { contentDescription = toggleLabel },
            contentAlignment = Alignment.Center,
        ) {
            CheckRing(checked = task.isCompleted, accent = accent)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                ),
                color = if (task.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = dueLabel(due),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = dueColor(due),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun CheckRing(
    checked: Boolean,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(accent)
                } else {
                    Modifier.border(2.dp, accent, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun dueLabel(due: TaskDue): String =
    when (due) {
        TaskDue.Done -> stringResource(R.string.personal_task_done)
        TaskDue.Today -> stringResource(R.string.discipline_tasks_due_today)
        TaskDue.Tomorrow -> stringResource(R.string.discipline_tasks_due_tomorrow)
        is TaskDue.InDays -> pluralStringResource(R.plurals.discipline_tasks_due_in, due.days, due.days)
        is TaskDue.On -> stringResource(R.string.discipline_tasks_due_on, CalendarFormat.dateShort(due.date))
        is TaskDue.Overdue -> pluralStringResource(R.plurals.discipline_tasks_overdue, due.days, due.days)
    }

@Composable
private fun dueColor(due: TaskDue): Color =
    when (due) {
        is TaskDue.Overdue -> MaterialTheme.melon.status.bad
        TaskDue.Today, TaskDue.Tomorrow -> MaterialTheme.melon.status.warn
        TaskDue.Done -> MaterialTheme.colorScheme.outlineVariant
        is TaskDue.InDays, is TaskDue.On -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Preview
@Composable
private fun DisciplineTasksCardPreview() {
    val today = LocalDate.now()
    fun task(
        id: String,
        title: String,
        inDays: Long,
        completedAt: Long? = null,
        category: PersonalCategory = PersonalCategory.Task,
    ) = PersonalEntry(
        id = id,
        title = title,
        start = today.plusDays(inDays),
        end = null,
        category = category,
        discipline = null,
        reminder = PersonalReminder.DayBefore,
        notes = "",
        createdAt = 0L,
        completedAt = completedAt,
    )
    MelonTheme {
        DisciplineTasksCard(
            tasks = listOf(
                task("1", "Lista 3 de exercícios", inDays = -1),
                task("2", "Relatório do laboratório", inDays = 2),
                task("3", "Revisar para a P2", inDays = 12, category = PersonalCategory.Study),
                task("4", "Seminário em grupo", inDays = -4, completedAt = 1L),
            ),
            canAdd = true,
            onToggle = {},
            onOpen = {},
            onAdd = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

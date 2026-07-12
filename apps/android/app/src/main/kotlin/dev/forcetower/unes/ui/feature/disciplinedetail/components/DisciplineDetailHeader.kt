package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.hasMultipleGroups

// Headline block under the top app bar — code chip + department eyebrow,
// discipline name, one avatar row per teacher, and the M3 segmented group
// filter on multi-group disciplines ("Tudo / Teórica / Prática").
@Composable
internal fun DisciplineDetailHeader(
    discipline: Discipline,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CodeChip(text = discipline.fullCode, accent = discipline.color)
            if (discipline.dept.isNotEmpty()) {
                // Upstream sometimes ships the department already prefixed
                // ("Departamento de Ciências Exatas") — only prepend when it
                // isn't, or the line reads "Departamento de Departamento de…".
                val dept = discipline.dept
                Text(
                    text = if (dept.startsWith("departamento", ignoreCase = true)) {
                        dept
                    } else {
                        stringResource(R.string.discipline_detail_department_format, dept)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = discipline.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 30.sp,
                lineHeight = 33.sp,
                letterSpacing = (-0.6).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 12.dp),
        )

        TeacherRows(
            discipline = discipline,
            modifier = Modifier.padding(top = 14.dp),
        )

        if (discipline.hasMultipleGroups) {
            GroupFilter(
                discipline = discipline,
                selected = selectedGroup,
                onSelect = onSelectGroup,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun CodeChip(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 11.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            ),
            color = accent,
        )
    }
}

// One row per teacher — initials avatar tinted with the discipline color plus
// name and the "Professor · T01 · Teórica" meta line. Falls back to a single
// seed-professor row while the detail payload hydrates.
@Composable
private fun TeacherRows(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val role = stringResource(R.string.discipline_detail_teacher_role)
    val multi = discipline.hasMultipleGroups
    data class TeacherLine(val name: String, val meta: String)
    val lines = if (discipline.groups.isNotEmpty()) {
        discipline.groups
            .filter { it.prof.isNotEmpty() || it.code.isNotEmpty() }
            .map { g ->
                val meta = buildList {
                    if (!multi) add(role)
                    if (g.code.isNotEmpty()) add(g.code)
                    if (g.kind.isNotEmpty()) add(g.kind)
                }.joinToString(separator = " · ")
                TeacherLine(name = g.prof.ifEmpty { role }, meta = meta)
            }
    } else if (discipline.prof.isNotEmpty()) {
        listOf(TeacherLine(name = discipline.prof, meta = role))
    } else {
        emptyList()
    }
    if (lines.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        lines.forEach { line ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(name = line.name, accent = discipline.color)
                Column {
                    Text(
                        text = line.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (line.meta.isNotEmpty()) {
                        Text(
                            text = line.meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialsAvatar(name: String, accent: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(name),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = accent,
        )
    }
}

// First letter of the first and last words — "Gabriela Ribeiro Peixoto" → "GP".
private fun initials(name: String): String {
    val words = name.split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return "·"
    val first = words.first().first().uppercaseChar()
    if (words.size == 1) return first.toString()
    return "$first${words.last().first().uppercaseChar()}"
}

@Composable
private fun GroupFilter(
    discipline: Discipline,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val border = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f)
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = accent.copy(alpha = 0.16f),
        activeContentColor = accent,
        activeBorderColor = border,
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveBorderColor = border,
    )

    data class Option(val code: String?, val label: String)
    val options = listOf(Option(null, stringResource(R.string.discipline_detail_group_all))) +
        discipline.groups.map { Option(it.code, it.kind.ifEmpty { it.code }) }

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.height(40.dp),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option.code,
                onClick = { onSelect(option.code) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = colors,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

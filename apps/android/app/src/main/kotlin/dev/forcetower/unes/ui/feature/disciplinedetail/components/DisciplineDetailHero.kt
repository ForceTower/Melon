package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplineGroup
import dev.forcetower.unes.ui.feature.disciplines.hasMultipleGroups

// Top section of the detail screen — back chevron, code chip, department,
// title, professor row (stacks for multi-group), and a segmented control to
// switch between "Tudo / Teórica / Prática". Mirrors iOS
// `DisciplineDetailHero`.
@Composable
internal fun DisciplineDetailHero(
    discipline: Discipline,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        BackChevronButton(onBack = onBack)

        Row(
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CodeChip(text = discipline.fullCode, accent = discipline.color)
            if (discipline.dept.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.discipline_detail_department_format, discipline.dept),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = discipline.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 30.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.6).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )

        ProfessorRow(
            discipline = discipline,
            selectedGroup = selectedGroup,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (discipline.hasMultipleGroups) {
            GroupSegmented(
                groups = discipline.groups,
                selected = selectedGroup,
                onChange = onSelectGroup,
                accent = discipline.color,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun BackChevronButton(onBack: () -> Unit) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val backLabel = stringResource(R.string.discipline_detail_back)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(card)
            .border(1.dp, cardLine, CircleShape)
            .clickable(onClick = onBack)
            .semantics {
                role = Role.Button
                contentDescription = backLabel
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val strokePx = 1.5.dp.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val arm = size.minDimension * 0.32f
            val path = Path().apply {
                moveTo(cx + arm * 0.4f, cy - arm)
                lineTo(cx - arm * 0.4f, cy)
                lineTo(cx + arm * 0.4f, cy + arm)
            }
            drawPath(
                path = path,
                color = ink,
                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun CodeChip(text: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(accent.copy(alpha = 0.13f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = accent,
        )
    }
}

// Lists the professor for the active group. On a multi-group discipline with
// no group selected ("Tudo"), shows one row per group with a turma badge.
@Composable
private fun ProfessorRow(
    discipline: Discipline,
    selectedGroup: String?,
    modifier: Modifier = Modifier,
) {
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink4 = MaterialTheme.colorScheme.outlineVariant

    if (!discipline.hasMultipleGroups) {
        if (discipline.prof.isNotEmpty()) {
            ProfessorLine(prof = discipline.prof, turma = null, ink2 = ink2, modifier = modifier)
        }
        return
    }

    val resolved = selectedGroup?.let { code -> discipline.groups.firstOrNull { it.code == code } }
    if (resolved != null) {
        ProfessorLine(prof = resolved.prof, turma = resolved.code, ink2 = ink2, modifier = modifier)
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        discipline.groups.forEach { g ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfessorIcon()
                Text(
                    text = g.prof,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = ink2,
                )
                TurmaBadge(text = g.code)
                Text(
                    text = "· ${g.kind}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = ink4,
                )
            }
        }
    }
}

@Composable
private fun ProfessorLine(
    prof: String,
    turma: String?,
    ink2: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfessorIcon()
        Text(
            text = prof,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = ink2,
        )
        if (turma != null) TurmaBadge(text = turma)
    }
}

@Composable
private fun ProfessorIcon() {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Canvas(modifier = Modifier.size(12.dp)) {
        val strokePx = 1.3.dp.toPx()
        val cx = size.width / 2f
        // Head
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.18f,
            center = Offset(cx, size.height * 0.36f),
            style = Stroke(width = strokePx),
        )
        // Shoulders (half-circle arc approximated with a path)
        val arcPath = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.92f)
            cubicTo(
                size.width * 0.20f, size.height * 0.62f,
                size.width * 0.80f, size.height * 0.62f,
                size.width * 0.82f, size.height * 0.92f,
            )
        }
        drawPath(
            path = arcPath,
            color = tint,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun TurmaBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.72.sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

// "Tudo / Teórica / Prática" — each option shows its turma code as a caption.
@Composable
private fun GroupSegmented(
    groups: List<DisciplineGroup>,
    selected: String?,
    onChange: (String?) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val shape = RoundedCornerShape(14.dp)

    data class Option(val code: String?, val kind: String, val turma: String?)
    val options = listOf(Option(null, stringResource(R.string.discipline_detail_group_all), null)) +
        groups.map { Option(it.code, it.kind, it.code) }

    Row(
        modifier = modifier
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { opt ->
            val active = selected == opt.code
            val pillShape = RoundedCornerShape(11.dp)
            Column(
                modifier = Modifier
                    .clip(pillShape)
                    .background(if (active) accent else Color.Transparent)
                    .clickable { onChange(opt.code) }
                    .padding(horizontal = 12.dp)
                    .padding(top = 7.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = opt.kind,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = if (active) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = opt.turma ?: "·",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = if (active) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                )
            }
        }
    }
}

package dev.forcetower.unes.ui.feature.disciplinedetail.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplinedetail.DisciplineFinalsMath
import dev.forcetower.unes.ui.feature.disciplines.AbsenceRisk
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.absenceRisk
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import dev.forcetower.unes.ui.feature.disciplines.partialAverage
import java.util.Locale

private val CardShape = RoundedCornerShape(22.dp)

// ── Carga horária ────────────────────────────────────────────────────────────

@Composable
internal fun DisciplineCargaCard(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.cardLine, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = Icons.Filled.Schedule, tint = accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.discipline_detail_stat_hours),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.discipline_detail_stat_hours_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = stringResource(R.string.disciplines_card_hours_format, discipline.hours),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.9).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ── Prova Final callout ──────────────────────────────────────────────────────

// Shown while the student is sitting the Prova Final: restates the partial
// mean and computes the minimum exam grade (floor the mean, ceil the target —
// the university truncates grades).
@Composable
internal fun DisciplineFinalsCallout(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val warn = MaterialTheme.melon.status.warn
    val card = MaterialTheme.melon.surface.card
    val average = discipline.partialAverage ?: 0.0
    val needed = DisciplineFinalsMath.neededFinalGrade(average)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(warn.copy(alpha = 0.12f).compositeOver(card))
            .border(1.dp, warn.copy(alpha = 0.30f), CardShape),
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    tint = warn,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.discipline_detail_final_section).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp,
                    ),
                    color = warn,
                )
            }
            Text(
                text = stringResource(R.string.discipline_detail_callout_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 10.dp),
            )
            val directPass = formatGrade(DisciplineFinalsMath.DIRECT_PASS_THRESHOLD)
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.discipline_detail_callout_lead))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(formatGrade(average))
                    }
                    append(stringResource(R.string.discipline_detail_callout_tail, directPass))
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(card)
                .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Filled.Adjust, tint = warn, size = 48.dp)
            Text(
                text = formatGrade(needed),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.2).sp,
                ),
                color = warn,
            )
            Text(
                text = stringResource(R.string.discipline_detail_callout_min_caption),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Frequência ───────────────────────────────────────────────────────────────

@Composable
internal fun DisciplineFrequenciaCard(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val ok = MaterialTheme.melon.status.ok
    val warn = MaterialTheme.melon.status.warn
    val bad = MaterialTheme.melon.status.bad
    val risk = discipline.absenceRisk
    val tone = when (risk) {
        AbsenceRisk.Risk -> bad
        AbsenceRisk.Warn -> warn
        AbsenceRisk.Ok -> ok
    }
    val used = discipline.absences
    val allowed = discipline.allowedAbsences
    val remaining = (allowed - used).coerceAtLeast(0)
    val ratio = if (allowed > 0) (used.toFloat() / allowed).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.cardLine, CardShape)
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = used.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 44.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.3).sp,
                        ),
                        color = tone,
                    )
                    Text(
                        text = stringResource(R.string.discipline_detail_freq_of_format, allowed),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.discipline_detail_freq_registered),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = remaining.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.discipline_detail_freq_remaining).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(10.dp),
            color = tone,
            trackColor = MaterialTheme.melon.surface.line,
            strokeCap = StrokeCap.Round,
        )

        AbsenceTicks(
            used = used,
            allowed = allowed,
            tone = tone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        )

        val bannerText = when (risk) {
            AbsenceRisk.Ok -> pluralStringResource(R.plurals.discipline_detail_freq_ok, remaining, remaining)
            AbsenceRisk.Warn -> stringResource(R.string.discipline_detail_freq_warn_mid)
            AbsenceRisk.Risk -> stringResource(R.string.discipline_detail_freq_warn_high)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tone.copy(alpha = 0.12f).compositeOver(MaterialTheme.melon.surface.card))
                .border(1.dp, tone.copy(alpha = 0.26f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (risk == AbsenceRisk.Ok) Icons.Filled.Verified else Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = bannerText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// One tick per allowed absence, capped at 15 cells (long workloads would
// otherwise shrink the squares into noise) — filled ticks scale with usage.
@Composable
private fun AbsenceTicks(
    used: Int,
    allowed: Int,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    if (allowed <= 0) return
    val cells = minOf(allowed, 15)
    val filled = if (allowed <= cells) {
        used.coerceIn(0, cells)
    } else {
        ((used.toFloat() / allowed) * cells).toInt().coerceIn(0, cells)
    }
    val line = MaterialTheme.melon.surface.line
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(cells) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index < filled) tone else line),
            )
        }
    }
}

// ── Ementa ───────────────────────────────────────────────────────────────────

@Composable
internal fun DisciplineEmentaCard(
    discipline: Discipline,
    modifier: Modifier = Modifier,
) {
    val ementa = discipline.ementa?.takeIf { it.isNotEmpty() } ?: return
    var expanded by rememberSaveable(ementa) { mutableStateOf(false) }
    var overflows by rememberSaveable(ementa) { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(CardShape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.cardLine, CardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(discipline.color),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(18.dp)
                .animateContentSize(),
        ) {
            Text(
                text = stringResource(R.string.discipline_detail_ementa_overline).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = ementa,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (it.hasVisualOverflow) overflows = true },
                modifier = Modifier.padding(top = 10.dp),
            )
            if (overflows) {
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (expanded) {
                                R.string.discipline_detail_ementa_show_less
                            } else {
                                R.string.discipline_detail_ementa_show_more
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ── Colaborativo banner ──────────────────────────────────────────────────────

// Entry point to the collaborative Materiais shelf. With materials it reads
// as a link ("8 materiais da turma"); an empty shelf pitches the first upload.
@Composable
internal fun DisciplineCollabBanner(
    discipline: Discipline,
    count: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subject = discipline.color
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(subject.copy(alpha = 0.10f).compositeOver(MaterialTheme.melon.surface.card))
            .border(1.dp, subject.copy(alpha = 0.26f), shape)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = Icons.Filled.Groups, tint = subject)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.discipline_detail_collab_overline).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                ),
                color = subject,
            )
            Text(
                text = if (count > 0) {
                    pluralStringResource(R.plurals.discipline_detail_collab_count, count, count)
                } else {
                    stringResource(R.string.discipline_detail_collab_empty_title)
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (count == 0) {
                Text(
                    text = stringResource(R.string.discipline_detail_collab_empty_sub),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Icon(
            imageVector = if (count > 0) Icons.Filled.ChevronRight else Icons.Filled.Add,
            contentDescription = null,
            tint = if (count > 0) MaterialTheme.colorScheme.outlineVariant else subject,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ── Shared ───────────────────────────────────────────────────────────────────

@Composable
private fun IconTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

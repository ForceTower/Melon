package dev.forcetower.unes.ui.feature.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.library.domain.model.LibraryAvailability
import dev.forcetower.melon.feature.library.domain.model.LibraryReading
import dev.forcetower.melon.feature.library.domain.model.LibraryWork
import dev.forcetower.melon.feature.library.domain.model.LibraryWorkType
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.SkeletonBar
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.library.formatLibraryAgo
import dev.forcetower.unes.ui.feature.library.hue
import dev.forcetower.unes.ui.feature.library.icon
import dev.forcetower.unes.ui.feature.library.labelRes
import kotlin.time.Instant

// Shared chrome for the Biblioteca screens: the spine mark that stands in for
// covers (Pergamum provides none), availability rendering, the freshness
// stamp, and the small plates/notes the three screens share.

@Composable
internal fun LibraryBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onBack, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.library_back),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// Scope/restrict chip in the design's accent treatment: selected = soft
// accent-tinted container with accent label + check, unselected = outlined.
// M3 FilterChip defaults would pull the magenta secondaryContainer instead.
@Composable
internal fun LibraryFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    showCheck: Boolean = true,
) {
    val accent = MaterialTheme.colorScheme.primary
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected && showCheck) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = accent.copy(alpha = 0.18f),
            selectedLabelColor = accent,
            selectedLeadingIconColor = accent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.melon.surface.line,
            selectedBorderColor = Color.Transparent,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun LibraryCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape),
    ) {
        content()
    }
}

@Composable
internal fun LibrarySectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

// Tinted info plate — the catalogue disclaimers.
@Composable
internal fun LibraryInfoNote(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// The spine mark standing in for a cover: type stripe + glyph and the
// classification split into its big first token ("515") and the cutter tail.
// The catalogue's occasional T/C/P prefixes are shelf-area markers, not part
// of the classification the student walks to.
@Composable
internal fun LibraryWorkMark(
    work: LibraryWork,
    modifier: Modifier = Modifier,
    width: Int = 56,
    height: Int = 80,
) {
    val hue = work.type.hue()
    val shape = RoundedCornerShape(if (width > 50) 12.dp else 10.dp)
    val classification = work.callNumber
        .replace(Regex("""^[A-Z]{1,2}\s+"""), "")
        .split(" ")
    Box(
        modifier = modifier
            .width(width.dp)
            .height(height.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.melon.surface.line, shape),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(height.dp)
                .background(hue.copy(alpha = if (work.type == LibraryWorkType.Book) 0.3f else 0.95f)),
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp, end = 6.dp, top = 7.dp, bottom = 6.dp)
                .fillMaxWidth(),
        ) {
            Icon(
                imageVector = work.type.icon(),
                contentDescription = stringResource(work.type.labelRes()),
                tint = hue,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.End),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = classification.firstOrNull().orEmpty(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = if (width > 50) 14.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = classification.drop(1).joinToString(" "),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Uppercase type chip — "LIVRO", "CORDEL" — in the type hue.
@Composable
internal fun LibraryTypeTag(work: LibraryWork, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(work.type.labelRes()).uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        ),
        color = work.type.hue(),
        maxLines = 1,
        modifier = modifier,
    )
}

internal enum class LibraryVerdictTone { Ok, Bad, Other, Muted }

@Composable
internal fun LibraryVerdictTone.color(): Color = when (this) {
    LibraryVerdictTone.Ok -> MaterialTheme.melon.status.ok
    LibraryVerdictTone.Bad -> MaterialTheme.melon.status.bad
    LibraryVerdictTone.Other -> MaterialTheme.melon.palette.indigo
    LibraryVerdictTone.Muted -> MaterialTheme.colorScheme.outline
}

internal fun LibraryAvailability.verdictTone(): LibraryVerdictTone = when (verdict) {
    LibraryAvailability.Verdict.Available -> LibraryVerdictTone.Ok
    LibraryAvailability.Verdict.AllOnLoan -> LibraryVerdictTone.Bad
    LibraryAvailability.Verdict.LocalUseOnly -> LibraryVerdictTone.Other
}

// Verdict headline — "51 de 100 disponíveis" / "Nenhum disponível" /
// "Só consulta local". Missing copies are already outside the denominator.
@Composable
internal fun LibraryAvailability.verdictHead(): String = when (verdict) {
    LibraryAvailability.Verdict.Available ->
        pluralStringResource(R.plurals.library_availability_some, available, available, total)
    LibraryAvailability.Verdict.AllOnLoan ->
        stringResource(R.string.library_availability_none)
    LibraryAvailability.Verdict.LocalUseOnly ->
        stringResource(R.string.library_availability_local)
}

// The one-line availability slot on a result row: shimmer while consulting,
// an honest "couldn't read it" when Pergamum is down, or the verdict dot +
// counts + where.
@Composable
internal fun LibraryRowAvailability(
    work: LibraryWork,
    reading: LibraryReading?,
    effectiveAvailability: LibraryAvailability,
    now: Instant,
) {
    when (reading) {
        null -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(18.dp),
        ) {
            SkeletonBar(width = 9.dp, height = 9.dp)
            SkeletonBar(width = 112.dp, height = 9.dp)
        }
        LibraryReading.Unavailable -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.library_availability_down),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        is LibraryReading.Fresh, is LibraryReading.Stale -> {
            val stale = reading is LibraryReading.Stale
            val tone = if (stale) {
                MaterialTheme.colorScheme.outline
            } else {
                effectiveAvailability.verdictTone().color()
            }
            val tail = when {
                reading is LibraryReading.Stale -> stringResource(
                    R.string.library_availability_stale_tail,
                    formatLibraryAgo(reading.checkedAt, now),
                )
                effectiveAvailability.branches.size == 1 ->
                    stringResource(
                        R.string.library_availability_lib_tail,
                        effectiveAvailability.branches.first().branch.sigla,
                    )
                else -> pluralStringResource(
                    R.plurals.library_availability_libs_tail,
                    effectiveAvailability.branches.size,
                    effectiveAvailability.branches.size,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(tone),
                )
                Text(
                    text = effectiveAvailability.verdictHead(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = tone,
                    maxLines = 1,
                )
                Text(
                    text = tail,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// "Verificado há 2 min" / "Última leitura conhecida" / "Pergamum não
// respondeu" + the Atualizar action. `reading` is the screen's aggregate.
@Composable
internal fun LibraryFreshnessRow(
    reading: LibraryReading?,
    checking: Boolean,
    now: Instant,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val warn = MaterialTheme.melon.status.warn
    val bad = MaterialTheme.melon.status.bad
    val muted = MaterialTheme.colorScheme.outline
    val (icon, label, tone) = when (reading) {
        null -> Triple(null, stringResource(R.string.library_fresh_checking), muted)
        LibraryReading.Unavailable ->
            Triple(Icons.Filled.CloudOff, stringResource(R.string.library_fresh_down), bad)
        is LibraryReading.Stale -> Triple(
            Icons.Filled.HourglassTop,
            stringResource(R.string.library_fresh_stale, formatLibraryAgo(reading.checkedAt, now)),
            warn,
        )
        is LibraryReading.Fresh -> Triple(
            Icons.Filled.Schedule,
            stringResource(R.string.library_fresh_checked, formatLibraryAgo(reading.checkedAt, now)),
            muted,
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        if (checking || reading == null) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = muted,
                modifier = Modifier.size(14.dp),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
            color = tone,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (reading != null && !checking) {
            TextButton(onClick = onRefresh) {
                Text(
                    text = stringResource(R.string.library_fresh_refresh),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

// Tappable suggestion plate for the "busca ampla" / "nada encontrado" states.
@Composable
internal fun LibrarySuggestionRow(
    icon: ImageVector,
    label: String,
    hint: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

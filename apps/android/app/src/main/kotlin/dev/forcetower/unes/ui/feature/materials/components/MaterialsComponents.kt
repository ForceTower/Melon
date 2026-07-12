package dev.forcetower.unes.ui.feature.materials.components

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialFileKind
import dev.forcetower.melon.feature.materials.domain.model.MaterialStatus
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.materials.MaterialsFormat
import dev.forcetower.unes.ui.feature.materials.hue
import dev.forcetower.unes.ui.feature.materials.icon
import dev.forcetower.unes.ui.feature.materials.labelRes

// Shared building blocks for the Materiais screens (dc `MateriaisScreen`).

@Composable
internal fun MaterialsBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onBack, modifier = modifier) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.materials_back),
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// "MEUS ENVIOS" / "ACERVO" / "SUAS DISCIPLINAS" eyebrow.
@Composable
internal fun MaterialsSectionLabel(text: String, modifier: Modifier = Modifier) {
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

// White-on-tint rounded square with the type glyph — the leading badge on
// every material row.
@Composable
internal fun MaterialTypeBadge(material: Material, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(material.type.hue()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = material.type.icon(),
            contentDescription = null,
            tint = MaterialTheme.melon.fixed.onHero,
            modifier = Modifier.size(20.dp),
        )
    }
}

// One row of the public acervo card: type eyebrow (+ saved bookmark), title,
// "2025.2 · 4 págs · Camila" meta, and the useful/file-kind trailing stack.
@Composable
internal fun MaterialRow(
    material: Material,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 15.dp, vertical = 13.dp),
        ) {
            MaterialTypeBadge(material)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = stringResource(material.type.labelRes()).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = material.type.hue(),
                    )
                    if (material.isSaved) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = stringResource(R.string.materials_saved_marker),
                            tint = MaterialTheme.melon.status.ok,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = material.rowMeta(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = material.usefulCount.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = material.fileKind.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
        }
    }
}

// One row of the "Meus envios" card — the student's own pending/rejected
// submissions, with the moderation status pill.
@Composable
internal fun MineMaterialRow(
    material: Material,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 15.dp, vertical = 13.dp),
        ) {
            MaterialTypeBadge(material)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                MaterialStatusPill(status = material.status)
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterVertically),
            )
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
        }
    }
}

// "Em análise" (amber schedule) / "Não aprovado" (coral warning) chip.
@Composable
internal fun MaterialStatusPill(status: MaterialStatus, modifier: Modifier = Modifier) {
    val pending = status == MaterialStatus.Pending
    val tone = if (pending) MaterialTheme.melon.status.warn else MaterialTheme.melon.status.bad
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tone.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Icon(
            imageVector = if (pending) Icons.Filled.Schedule else Icons.Filled.Warning,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(
                if (pending) R.string.materials_status_pending else R.string.materials_status_rejected,
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = tone,
        )
    }
}

// Rounded-square tile carrying the discipline short code in its stable tint.
@Composable
internal fun DisciplineCodeTile(
    code: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Int = 44,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
            ),
            color = tint,
            maxLines = 1,
        )
    }
}

// Card plate used by every grouped list on these screens.
@Composable
internal fun MaterialsCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
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

// "2025.2 · 4 págs · Camila" — semester, page count, professor first name.
@Composable
private fun Material.rowMeta(): String {
    val pages = pluralStringResource(R.plurals.materials_pages_short, pages, pages)
    val prof = teacherName?.split(" ")?.firstOrNull()
    return listOfNotNull(semester, pages, prof).joinToString(" · ")
}

internal fun MaterialFileKind.icon() = when (this) {
    MaterialFileKind.Pdf -> Icons.Filled.Description
    MaterialFileKind.Photo -> Icons.Filled.PhotoCamera
}

@Composable
internal fun MaterialFileKind.label(): String = stringResource(
    when (this) {
        MaterialFileKind.Pdf -> R.string.materials_file_kind_pdf
        MaterialFileKind.Photo -> R.string.materials_file_kind_photo
    },
)

// Compact download tally with locale-aware thousands ("1,2 mil").
internal fun formatDownloads(count: Int): String = MaterialsFormat.compactCount(count)

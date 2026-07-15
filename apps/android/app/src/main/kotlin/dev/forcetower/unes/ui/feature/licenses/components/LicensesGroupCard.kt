package dev.forcetower.unes.ui.feature.licenses.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicenseFamily
import dev.forcetower.unes.ui.feature.licenses.LicensePackage
import kotlinx.coroutines.delay

// One license-family group: an M3 list header (tinted glyph tile + family name +
// blurb + "matched de total" count pill) above a card of expandable package
// rows. Mirrors the dc `LicensesScreen` groups. Tapping a row reveals the
// copyright notice, the repository link, and copy/license-text chips.
@Composable
internal fun LicensesGroupCard(
    family: LicenseFamily,
    items: List<LicensePackage>,
    total: Int,
    expandedKey: String?,
    onToggleExpanded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.melon.surface.line

    Column(modifier = modifier.fillMaxWidth()) {
        GroupHeader(family = family, matched = items.size, total = total)
        Spacer(modifier = Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.melon.surface.card)
                .border(1.dp, line, RoundedCornerShape(24.dp)),
        ) {
            items.forEachIndexed { index, pkg ->
                val key = "${family.name}/${pkg.coordinates}"
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(line),
                    )
                }
                LicenseRow(
                    pkg = pkg,
                    family = family,
                    expanded = expandedKey == key,
                    onToggle = { onToggleExpanded(key) },
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(family: LicenseFamily, matched: Int, total: Int) {
    val tone = family.toneBackground()
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val surface2 = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tone.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = family.displayName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.19).sp,
                ),
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = family.blurb,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.licenses_group_count_format, matched, total),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            color = ink3,
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .background(surface2)
                .padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun LicenseRow(
    pkg: LicensePackage,
    family: LicenseFamily,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MelonMotion.ease(),
        label = "row-chevron",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(family.toneBackground()),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = pkg.artifact,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (pkg.version.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pkg.version,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                            ),
                            color = ink4,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = pkg.groupId.ifBlank { stringResource(R.string.licenses_row_unknown_group) },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = ink3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = ink4,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(260, easing = MelonMotion.EmphasizedEasing)) +
                fadeIn(tween(260, easing = MelonMotion.EmphasizedEasing)),
            exit = shrinkVertically(tween(200, easing = MelonMotion.EmphasizedEasing)) +
                fadeOut(tween(200, easing = MelonMotion.EmphasizedEasing)),
        ) {
            ExpandedDetail(pkg = pkg, family = family)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandedDetail(pkg: LicensePackage, family: LicenseFamily) {
    val context = LocalContext.current
    val tone = family.toneBackground()
    val card = MaterialTheme.melon.surface.card
    val ink2 = MaterialTheme.colorScheme.onSurface
    val onTone = MaterialTheme.melon.fixed.onHero

    val author = pkg.groupId.ifBlank { pkg.artifact }
    val coord = if (pkg.version.isNotBlank()) "${pkg.coordinates}:${pkg.version}" else pkg.coordinates

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 16.dp, bottom = 18.dp),
    ) {
        // Copyright notice — tone washed into the card so each license family
        // reads with its own tint without leaving the neutral surface.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(card)
                .background(tone.copy(alpha = 0.09f))
                .border(1.dp, tone.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .padding(horizontal = 15.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.licenses_notice_format, author, family.displayName),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.sp),
                color = ink2,
            )
        }

        if (!pkg.scmUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            RepoLink(host = displayHost(pkg.scmUrl), tone = tone, onTone = onTone) {
                openUrl(context, pkg.scmUrl)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CopyChip(value = coord, context = context)
            if (!pkg.licenseUrl.isNullOrBlank()) {
                val label = pkg.licenseId ?: pkg.licenseName ?: stringResource(R.string.licenses_action_text_fallback)
                MetaChip(
                    icon = Icons.Filled.Description,
                    label = stringResource(R.string.licenses_action_text_format, label),
                    onClick = { openUrl(context, pkg.licenseUrl) },
                )
            }
        }
    }
}

@Composable
private fun RepoLink(host: String, tone: Color, onTone: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(tone)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
            .widthIn(max = 300.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.NorthEast,
            contentDescription = null,
            tint = onTone,
            modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = host,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = onTone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CopyChip(value: String, context: Context) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }
    MetaChip(
        icon = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
        label = if (copied) stringResource(R.string.licenses_action_copied) else value,
        onClick = {
            if (value.isNotBlank() && copyToClipboard(context, value)) copied = true
        },
    )
}

@Composable
private fun MetaChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    val surface2 = MaterialTheme.colorScheme.surfaceVariant
    val line = MaterialTheme.melon.surface.line
    val ink2 = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(surface2)
            .border(1.dp, line, RoundedCornerShape(100))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
            .widthIn(max = 300.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ink2, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

// Uses the platform `ClipboardManager` directly — `LocalClipboardManager` is
// deprecated in newer Compose in favor of a suspend-based `Clipboard`, which
// would force the click handler to launch a coroutine for what is a sync
// operation. Returns false if the system service is unavailable so the
// "copied" indicator only fires when the copy actually landed.
private fun copyToClipboard(context: Context, text: String): Boolean {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return false
    manager.setPrimaryClip(ClipData.newPlainText("license-coordinates", text))
    return true
}

// "https://github.com/owner/repo" → "github.com/owner/repo" — trims the scheme
// and a trailing slash so the link pill stays compact.
private fun displayHost(url: String): String {
    val withoutScheme = url.substringAfter("://", url)
    return withoutScheme.trimEnd('/')
}

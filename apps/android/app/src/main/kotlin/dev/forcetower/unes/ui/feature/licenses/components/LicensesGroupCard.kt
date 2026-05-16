package dev.forcetower.unes.ui.feature.licenses.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.LicenseFamily
import dev.forcetower.unes.ui.feature.licenses.LicensePackage
import kotlinx.coroutines.delay

// One license-family group: editorial header chip + count, then a stacked
// card of expandable rows. Mirrors `LicensesGroupCard` (iOS) + `LicGroupHeader`
// + `LicRow` (JSX). Tapping a row expands inline to reveal the copyright
// blurb and three actions (open homepage, copy coordinates, open license URL).
@Composable
internal fun LicensesGroupCard(
    family: LicenseFamily,
    items: List<LicensePackage>,
    expandedKey: String?,
    onToggleExpanded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GroupHeader(family = family, count = items.size)
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.melon.surface.card)
                .border(1.dp, MaterialTheme.melon.surface.cardLine, RoundedCornerShape(22.dp)),
        ) {
            items.forEachIndexed { index, pkg ->
                val key = "${family.name}/${pkg.coordinates}"
                LicenseRow(
                    pkg = pkg,
                    family = family,
                    expanded = expandedKey == key,
                    onToggle = { onToggleExpanded(key) },
                )
                if (index < items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp)
                            .height(1.dp)
                            .background(MaterialTheme.melon.surface.line),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(family: LicenseFamily, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LicenseFamilyChip(family = family, compact = false)
        Text(
            text = family.blurb.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                letterSpacing = 0.95.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 16.sp,
                lineHeight = 16.sp,
                letterSpacing = (-0.16).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
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
        animationSpec = tween(durationMillis = 220),
        label = "row-chevron",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Vertical color bar — same family tone as the chip, slightly
            // dimmed so it reads as a visual anchor rather than a CTA.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(family.toneBackground().copy(alpha = 0.85f)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nameAndVersion(pkg = pkg, ink = ink, ink4 = ink4),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.06.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = pkg.groupId.ifBlank { stringResource(R.string.licenses_row_unknown_group) },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        letterSpacing = (-0.06).sp,
                    ),
                    color = ink3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LicenseFamilyChip(family = family, compact = true)
            LicensesIcon(
                glyph = LicensesGlyph.ChevronDown,
                color = ink3,
                modifier = Modifier
                    .size(13.dp)
                    .rotate(rotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(280)),
            exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(220)),
        ) {
            ExpandedDetail(pkg = pkg, family = family)
        }
    }
}

@Composable
private fun nameAndVersion(
    pkg: LicensePackage,
    ink: Color,
    ink4: Color,
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = ink)) { append(pkg.artifact) }
    if (pkg.version.isNotEmpty()) {
        withStyle(SpanStyle(color = ink4, fontSize = 10.sp, letterSpacing = 0.4.sp)) {
            append("  @")
            append(pkg.version)
        }
    }
}

@Composable
private fun ExpandedDetail(pkg: LicensePackage, family: LicenseFamily) {
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val surface2 = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(surface2)
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "◦ ${family.blurb.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 1.08.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = ink4,
                )
                Text(
                    text = blurbAnnotated(pkg = pkg, family = family, ink2 = ink2, ink3 = ink3),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        letterSpacing = (-0.06).sp,
                    ),
                )
            }
        }

        ActionPills(pkg = pkg)
    }
}

@Composable
private fun blurbAnnotated(
    pkg: LicensePackage,
    family: LicenseFamily,
    ink2: Color,
    ink3: Color,
): AnnotatedString {
    val lead = stringResource(R.string.licenses_row_blurb_lead)
    val under = stringResource(R.string.licenses_row_blurb_under)
    val tail = stringResource(R.string.licenses_row_blurb_tail)
    val coords = pkg.coordinates.ifBlank { pkg.artifact }
    val licenseId = family.displayName
    return buildAnnotatedString {
        withStyle(SpanStyle(color = ink3)) { append("$lead ") }
        withStyle(SpanStyle(color = ink2, fontWeight = FontWeight.Medium)) { append(coords) }
        withStyle(SpanStyle(color = ink3)) { append(" $under ") }
        withStyle(SpanStyle(color = ink2, fontWeight = FontWeight.Medium)) { append(licenseId) }
        withStyle(SpanStyle(color = ink3)) { append(tail) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionPills(pkg: LicensePackage) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!pkg.scmUrl.isNullOrBlank()) {
            ActionPill(
                icon = LicensesGlyph.ExternalLink,
                label = displayHost(pkg.scmUrl),
                primary = true,
                onClick = { openUrl(context, pkg.scmUrl) },
            )
        }
        ActionPill(
            icon = if (copied) LicensesGlyph.Check else LicensesGlyph.Copy,
            label = if (copied) {
                stringResource(R.string.licenses_action_copied)
            } else {
                pkg.coordinates.ifBlank { pkg.artifact }
            },
            primary = false,
            onClick = {
                val toCopy = pkg.coordinates.ifBlank { pkg.artifact }
                if (toCopy.isNotBlank() && copyToClipboard(context, toCopy)) {
                    copied = true
                }
            },
        )
        if (!pkg.licenseUrl.isNullOrBlank()) {
            val fallback = stringResource(R.string.licenses_action_text_fallback)
            val licenseLabel = pkg.licenseId ?: pkg.licenseName ?: fallback
            ActionPill(
                icon = LicensesGlyph.ExternalLink,
                label = stringResource(R.string.licenses_action_text_format, licenseLabel),
                primary = false,
                onClick = { openUrl(context, pkg.licenseUrl) },
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: LicensesGlyph,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface

    val bg = if (primary) ink else card
    val fg = if (primary) surface else ink2
    val border = if (primary) ink else cardLine

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .widthIn(max = 220.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        LicensesIcon(glyph = icon, color = fg, modifier = Modifier.size(11.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
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

// "https://github.com/owner/repo" → "github.com/owner/repo" — trims the
// scheme and a trailing slash so the pill stays compact.
private fun displayHost(url: String): String {
    val withoutScheme = url.substringAfter("://", url)
    return withoutScheme.trimEnd('/')
}

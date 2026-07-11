package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.firebase.FeatureGates
import dev.forcetower.unes.ui.feature.me.MeFixtures
import dev.forcetower.unes.ui.feature.me.Shortcut
import dev.forcetower.unes.ui.feature.me.ShortcutKind
import dev.forcetower.unes.ui.feature.me.hue
import java.util.Locale

// Two-column grid of tonal shortcut cards — dc `EuScreen` "Atalhos". Each
// card follows the `DisciplineCard` tonal recipe (8% plate, 20% border, 20%
// icon container, full-hue icon); pressing scales to 0.97.
@Composable
internal fun ShortcutGrid(
    shortcuts: List<Shortcut>,
    onOpen: (ShortcutKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        shortcuts.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { shortcut ->
                    ShortcutCard(
                        shortcut = shortcut,
                        onOpen = { onOpen(shortcut.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Full-width shortcut row under the grid — the dc design promotes Materiais
// into this wide card.
@Composable
internal fun WideShortcutCard(
    shortcut: Shortcut,
    onOpen: (ShortcutKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hue = shortcut.tone.hue()
    val label = stringResource(shortcut.labelRes)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = MelonMotion.pop(),
        label = "wide-shortcut-scale",
    )
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(tonalPlate(hue))
            .border(1.dp, hue.copy(alpha = 0.20f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = { onOpen(shortcut.id) },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconContainer(shortcut = shortcut, hue = hue, size = 46.dp, iconSize = 25.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(shortcut.hintRes),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ShortcutCard(
    shortcut: Shortcut,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hue = shortcut.tone.hue()
    val label = stringResource(shortcut.labelRes)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = MelonMotion.pop(),
        label = "shortcut-scale",
    )
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(tonalPlate(hue))
            .border(1.dp, hue.copy(alpha = 0.20f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = onOpen,
            )
            .padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 17.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconContainer(shortcut = shortcut, hue = hue, size = 44.dp, iconSize = 24.dp)
            if (shortcut.beta) {
                BetaBadge()
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                lineHeight = 17.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(shortcut.hintRes),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun IconContainer(shortcut: Shortcut, hue: Color, size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(hue.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = shortcut.icon,
            contentDescription = null,
            tint = hue,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun BetaBadge() {
    val accent = MaterialTheme.colorScheme.primary
    Text(
        text = stringResource(R.string.me_shortcut_beta).uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.9.sp,
        ),
        color = accent,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

// dc `color-mix(in srgb, hue 8%, card)` — the tonal plate every shortcut sits on.
@Composable
private fun tonalPlate(hue: Color): Color =
    hue.copy(alpha = 0.08f).compositeOver(MaterialTheme.melon.surface.card)

@Preview
@Composable
private fun ShortcutGridPreview() {
    MelonTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            ShortcutGrid(
                shortcuts = MeFixtures.gridShortcuts(
                    FeatureGates(
                        enrollment = true,
                        enrollmentCertificate = true,
                        academicHistory = true,
                        paradoxo = true,
                        materials = true,
                    ),
                ),
                onOpen = {},
            )
            Spacer(Modifier.height(12.dp))
            WideShortcutCard(shortcut = MeFixtures.materials, onOpen = {})
        }
    }
}

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.remote.FeatureGates
import dev.forcetower.unes.ui.feature.me.MeFixtures
import dev.forcetower.unes.ui.feature.me.Shortcut
import dev.forcetower.unes.ui.feature.me.ShortcutKind
import dev.forcetower.unes.ui.feature.me.hue

// Three-column grid of tonal shortcut cards — dc `EuScreen` "Atalhos". Each
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
        shortcuts.chunked(3).forEach { rowItems ->
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
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
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
    val shape = RoundedCornerShape(18.dp)

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
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconContainer(shortcut = shortcut, hue = hue, size = 38.dp, iconSize = 21.dp)
            if (shortcut.beta) {
                BetaBadge()
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 13.sp,
                lineHeight = 16.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        shortcut.hintRes?.let { hintRes ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(hintRes),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                ),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun IconContainer(shortcut: Shortcut, hue: Color, size: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
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
        text = stringResource(R.string.me_shortcut_beta).uppercase(LocalConfiguration.current.locales[0]),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
        ),
        color = accent,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
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
                        library = true,
                        materials = true,
                    ),
                ),
                onOpen = {},
            )
        }
    }
}

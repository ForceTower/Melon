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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.Shortcut
import dev.forcetower.unes.ui.feature.me.ShortcutKind
import dev.forcetower.unes.ui.feature.me.resolveTone

// 3-column constellation of pinned shortcuts. The hub renders whatever subset
// the user has pinned (managed via the Tweak panel in the design; the live
// app will eventually expose this through Settings). Each tile is a card
// surface with an accented icon badge, a label, and a single-line hint —
// pressing scales the tile to 0.96 to match the JSX prototype.
@Composable
internal fun ShortcutGrid(
    shortcuts: List<Shortcut>,
    onOpen: (ShortcutKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        shortcuts.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { shortcut ->
                    ShortcutTile(
                        shortcut = shortcut,
                        onOpen = { onOpen(shortcut.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Pad incomplete rows so each tile keeps its 3-column width.
                repeat(3 - rowItems.size) {
                    Box(modifier = Modifier.weight(1f).height(94.dp))
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val tone = resolveTone(shortcut.tone)

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = MelonMotion.pop(),
        label = "shortcut-tile-scale",
    )

    val label = stringResource(shortcut.labelRes)
    Column(
        modifier = modifier
            .heightIn(min = 94.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onClick = onOpen,
            )
            .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(9.dp),
                    ambientColor = tone.background.copy(alpha = 0.2f),
                    spotColor = tone.background.copy(alpha = 0.2f),
                )
                .clip(RoundedCornerShape(9.dp))
                .background(tone.background),
            contentAlignment = Alignment.Center,
        ) {
            MeShortcutIconBox(
                icon = shortcut.icon,
                color = tone.foreground,
                modifier = Modifier.size(16.dp),
            )
        }
        Box(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.06).sp,
            ),
            color = ink,
            maxLines = 1,
        )
        Box(modifier = Modifier.height(3.dp))
        Text(
            text = stringResource(shortcut.hintRes),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 11.sp,
                letterSpacing = 0.36.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink4,
            maxLines = 1,
        )
    }
}

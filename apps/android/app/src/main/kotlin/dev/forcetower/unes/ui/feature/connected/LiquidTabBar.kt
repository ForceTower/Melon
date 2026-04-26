package dev.forcetower.unes.ui.feature.connected

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon

// "Liquid" pill tab bar — translucent fill, soft inner shine, single active
// pill that swaps the tab's slot fill for `onBackground`. Mirrors the JSX
// prototype `TabBar` component in `screens-home.jsx`. iOS uses native
// `TabView` for the same role; Android takes the custom route so the visual
// (rounded outer pill, animated label expansion) matches the prototype.

@Composable
internal fun LiquidTabBar(
    items: List<ConnectedTab>,
    active: ConnectedTab,
    onChange: (ConnectedTab) -> Unit,
    modifier: Modifier = Modifier,
    badges: Map<ConnectedTab, Int> = emptyMap(),
) {
    val chromeBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val onInk = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(chromeBg)
            .border(1.dp, MaterialTheme.melon.surface.cardLine, RoundedCornerShape(32.dp))
            .padding(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items.forEach { tab ->
                TabSlot(
                    tab = tab,
                    active = tab == active,
                    badge = badges[tab] ?: 0,
                    ink = ink,
                    ink3 = ink3,
                    onInk = onInk,
                    accent = accent,
                    onClick = { onChange(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabSlot(
    tab: ConnectedTab,
    active: Boolean,
    badge: Int,
    ink: Color,
    ink3: Color,
    onInk: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val label = stringResource(tab.labelRes)

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (active) ink else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .semantics {
                role = Role.Tab
                selected = active
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(22.dp)) {
                TabIcon(tab = tab, color = if (active) onInk else ink3, active = active)
                if (badge > 0 && !active) {
                    BadgeBubble(
                        count = badge,
                        accent = accent,
                        onAccent = onInk,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 0.dp, end = 0.dp),
                    )
                }
            }
            AnimatedVisibility(
                visible = active,
                enter = fadeIn(tween(220)) + expandHorizontally(tween(220)),
                exit = fadeOut(tween(160)) + shrinkHorizontally(tween(160)),
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.36.sp,
                    ),
                    color = onInk,
                )
            }
        }
    }
}

@Composable
private fun BadgeBubble(
    count: Int,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(accent)
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = onAccent,
        )
    }
}

@Composable
private fun TabIcon(tab: ConnectedTab, color: Color, active: Boolean) {
    val width = if (active) 1.8.dp else 1.5.dp
    when (tab) {
        ConnectedTab.Overview -> HomeGlyph(color, width)
        ConnectedTab.Schedule -> GridGlyph(color, width)
        ConnectedTab.Classes -> StackGlyph(color, width)
        ConnectedTab.Messages -> ChatGlyph(color, width)
        ConnectedTab.Me -> UserGlyph(color, width)
    }
}

@Composable
private fun HomeGlyph(color: Color, width: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path().apply {
            moveTo(w * (3f / 22f), h * (10f / 22f))
            lineTo(w * (11f / 22f), h * (4f / 22f))
            lineTo(w * (19f / 22f), h * (10f / 22f))
            lineTo(w * (19f / 22f), h * (18f / 22f))
            lineTo(w * (15.5f / 22f), h * (19.5f / 22f))
            lineTo(w * (15.5f / 22f), h * (13f / 22f))
            lineTo(w * (8.5f / 22f), h * (13f / 22f))
            lineTo(w * (8.5f / 22f), h * (19.5f / 22f))
            lineTo(w * (4.5f / 22f), h * (18f / 22f))
            close()
        }
        drawPath(p, color = color, style = s)
    }
}

@Composable
private fun GridGlyph(color: Color, width: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val sw = width.toPx()
        val s = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        listOf(
            Offset(3f, 3f), Offset(12f, 3f), Offset(3f, 12f), Offset(12f, 12f),
        ).forEach { o ->
            val rect = Rect(
                offset = Offset(w * (o.x / 22f), h * (o.y / 22f)),
                size = Size(w * (7f / 22f), h * (7f / 22f)),
            )
            val rounded = RoundRect(rect, androidx.compose.ui.geometry.CornerRadius(w * (1.5f / 22f)))
            drawPath(Path().apply { addRoundRect(rounded) }, color = color, style = s)
        }
    }
}

@Composable
private fun StackGlyph(color: Color, width: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val top = Path().apply {
            moveTo(w * (11f / 22f), h * (3f / 22f))
            lineTo(w * (3f / 22f), h * (7f / 22f))
            lineTo(w * (11f / 22f), h * (11f / 22f))
            lineTo(w * (19f / 22f), h * (7f / 22f))
            close()
        }
        drawPath(top, color = color, style = s)
        listOf(11f, 15f).forEach { y ->
            val mid = Path().apply {
                moveTo(w * (3f / 22f), h * (y / 22f))
                lineTo(w * (11f / 22f), h * ((y + 4f) / 22f))
                lineTo(w * (19f / 22f), h * (y / 22f))
            }
            drawPath(mid, color = color, style = s)
        }
    }
}

@Composable
private fun ChatGlyph(color: Color, width: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val rect = Path().apply {
            val r = w * (2f / 22f)
            val rounded = RoundRect(
                rect = Rect(Offset(w * (4f / 22f), h * (3.5f / 22f)), Size(w * (14f / 22f), h * (11f / 22f))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
            )
            addRoundRect(rounded)
            // Tail
            moveTo(w * (5f / 22f), h * (18f / 22f))
            lineTo(w * (5f / 22f), h * (14.5f / 22f))
            moveTo(w * (5f / 22f), h * (18f / 22f))
            lineTo(w * (9f / 22f), h * (14.5f / 22f))
        }
        drawPath(rect, color = color, style = s)
    }
}

@Composable
private fun UserGlyph(color: Color, width: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val s = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(
            color = color,
            radius = w * (3.5f / 22f),
            center = Offset(w * (11f / 22f), h * (8f / 22f)),
            style = s,
        )
        val shoulders = Path().apply {
            moveTo(w * (4.5f / 22f), h * (18f / 22f))
            quadraticTo(w * 0.5f, h * (12f / 22f), w * (17.5f / 22f), h * (18f / 22f))
        }
        drawPath(shoulders, color = color, style = s)
    }
}

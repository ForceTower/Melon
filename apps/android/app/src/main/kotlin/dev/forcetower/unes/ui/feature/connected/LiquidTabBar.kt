package dev.forcetower.unes.ui.feature.connected

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon

// "Liquid" pill tab bar — translucent fill over a backdrop-blurred capture of
// the screen, with each slot stacking icon-on-top / label-below. Mirrors the
// JSX prototype `TabBar` in `screens-home.jsx`. iOS uses native `TabView` for
// the same role; Android takes the custom route to match the prototype's
// glass + animated label expansion.
//
// `backdrop` is the offscreen layer captured by `ConnectedScreen` with a
// `BlurEffect` already attached. We replay it here translated by the bar's
// negative root position, so the slice that lines up with our bounds shows
// behind the chrome — Compose's analog of CSS `backdrop-filter: blur(...)`.

@Composable
internal fun LiquidTabBar(
    items: List<ConnectedTab>,
    active: ConnectedTab,
    onChange: (ConnectedTab) -> Unit,
    modifier: Modifier = Modifier,
    badges: Map<ConnectedTab, Int> = emptyMap(),
    backdrop: GraphicsLayer? = null,
) {
    val chromeBg = MaterialTheme.colorScheme.surface.copy(alpha = if (backdrop != null) 0.55f else 0.78f)
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val onInk = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary
    val cardLine = MaterialTheme.melon.surface.cardLine

    var positionInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .onGloballyPositioned { positionInRoot = it.positionInRoot() }
            .clip(RoundedCornerShape(32.dp))
            .drawBehind {
                // Replay the captured (and pre-blurred) backdrop layer
                // translated so the slice under our pill aligns. The clip
                // applied above the drawBehind crops it to the rounded shape.
                if (backdrop != null) {
                    translate(left = -positionInRoot.x, top = -positionInRoot.y) {
                        drawLayer(backdrop)
                    }
                }
            }
            .background(chromeBg)
            .border(1.dp, cardLine, RoundedCornerShape(32.dp))
            .padding(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
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

    // All three transforms drive `Modifier.graphicsLayer` rather than layout
    // properties — that way the column never reflows on selection. The label
    // is always laid out (its slot reserved); only its alpha animates. The
    // icon scales + slides up via the same graphicsLayer pass, so motion
    // reads as one smooth gesture rather than the layout snap that an
    // `animateDpAsState`/`if (active)` toggle produced.
    val iconScale by animateFloatAsState(
        targetValue = if (active) 0.82f else 1f,
        animationSpec = MelonMotion.spring(),
        label = "tab-icon-scale",
    )
    val iconOffsetY by animateDpAsState(
        // Inactive: icon translates down ~9dp so it sits at the slot's
        // optical center (compensating for the always-reserved label space
        // below). Active: icon settles at its natural top position so the
        // label slots in below.
        targetValue = if (active) 0.dp else 9.dp,
        animationSpec = MelonMotion.spring(),
        label = "tab-icon-offset",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = MelonMotion.EmphasizedEasing),
        label = "tab-label-alpha",
    )
    val iconColor by animateColorAsState(
        targetValue = if (active) onInk else ink3,
        animationSpec = tween(durationMillis = 220, easing = MelonMotion.EmphasizedEasing),
        label = "tab-icon-color",
    )

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
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        translationY = iconOffsetY.toPx()
                    },
                contentAlignment = Alignment.Center,
            ) {
                TabIcon(tab = tab, color = iconColor, size = 22.dp, active = active)
                if (badge > 0 && !active) {
                    BadgeBubble(
                        count = badge,
                        accent = accent,
                        onAccent = onInk,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
            // Label space is always reserved; alpha animates so the column
            // height stays constant and the icon's translation is the only
            // vertical motion.
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.36.sp,
                ),
                color = onInk,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .graphicsLayer { alpha = labelAlpha },
            )
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
private fun TabIcon(tab: ConnectedTab, color: Color, size: Dp, active: Boolean) {
    val width = if (active) 1.8.dp else 1.5.dp
    when (tab) {
        ConnectedTab.Overview -> HomeGlyph(color, width, size)
        ConnectedTab.Schedule -> GridGlyph(color, width, size)
        ConnectedTab.Classes -> StackGlyph(color, width, size)
        ConnectedTab.Messages -> ChatGlyph(color, width, size)
        ConnectedTab.Me -> UserGlyph(color, width, size)
    }
}

@Composable
private fun HomeGlyph(color: Color, width: Dp, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
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
private fun GridGlyph(color: Color, width: Dp, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
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
private fun StackGlyph(color: Color, width: Dp, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
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
private fun ChatGlyph(color: Color, width: Dp, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = Stroke(width = width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val rect = Path().apply {
            val r = w * (2f / 22f)
            val rounded = RoundRect(
                rect = Rect(Offset(w * (4f / 22f), h * (3.5f / 22f)), Size(w * (14f / 22f), h * (11f / 22f))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
            )
            addRoundRect(rounded)
            moveTo(w * (5f / 22f), h * (18f / 22f))
            lineTo(w * (5f / 22f), h * (14.5f / 22f))
            moveTo(w * (5f / 22f), h * (18f / 22f))
            lineTo(w * (9f / 22f), h * (14.5f / 22f))
        }
        drawPath(rect, color = color, style = s)
    }
}

@Composable
private fun UserGlyph(color: Color, width: Dp, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
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

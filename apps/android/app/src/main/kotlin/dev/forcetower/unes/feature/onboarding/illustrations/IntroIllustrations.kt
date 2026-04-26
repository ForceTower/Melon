package dev.forcetower.unes.feature.onboarding.illustrations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import kotlinx.coroutines.delay

private val Plum = Color(0xFF2D1B4E)
private val Magenta = Color(0xFFB23A7A)
private val Coral = Color(0xFFE85D4E)
private val Amber = Color(0xFFF4A23C)

private val IllustrationSize = 260.dp

// ──────────────── Schedule (timetable) ────────────────

private data class ClassBlock(
    val col: Int,
    val row: Int,
    val height: Int,
    val color: Color,
    val label: String,
    val room: Int,
)

private val SCHEDULE_BLOCKS = listOf(
    ClassBlock(0, 0, 2, Coral, "ALGI", 124),
    ClassBlock(1, 1, 1, Amber, "CALC", 109),
    ClassBlock(2, 0, 1, Magenta, "LPOO", 132),
    ClassBlock(2, 2, 2, Plum, "FIS2", 117),
    ClassBlock(0, 3, 1, Amber, "PROJ", 102),
)

@Composable
fun ScheduleIllustration() {
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outline
    val gridLine = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)

    val transition = rememberInfiniteTransition(label = "now-line")
    val dashOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dash",
    )
    val pulse by transition.animateFloat(
        initialValue = 3f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(modifier = Modifier.size(IllustrationSize)) {
        Canvas(Modifier.fillMaxSize()) {
            val left = 20.dp.toPx()
            val top = 30.dp.toPx()
            val cellW = 73.dp.toPx()
            val rowH = 40.dp.toPx()

            // grid lines
            for (i in 0..5) {
                drawLine(
                    color = gridLine,
                    start = Offset(left, top + i * rowH),
                    end = Offset(left + 3 * cellW, top + i * rowH),
                    strokeWidth = 1f,
                )
            }
            for (i in 0..3) {
                drawLine(
                    color = gridLine,
                    start = Offset(left + i * cellW, top),
                    end = Offset(left + i * cellW, top + 5 * rowH),
                    strokeWidth = 1f,
                )
            }

            // current-time line — dashed, slowly drifting
            drawLine(
                color = Coral,
                start = Offset(left, top + 2 * rowH),
                end = Offset(left + 3 * cellW, top + 2 * rowH),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2f), phase = dashOffset),
            )
            // pulse dot
            drawCircle(
                color = Coral,
                radius = pulse.dp.toPx(),
                center = Offset(left, top + 2 * rowH),
            )
        }

        // class blocks
        SCHEDULE_BLOCKS.forEachIndexed { i, block ->
            FadeUpDelayed(delayMs = 100 + i * 120) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = (21 + block.col * 73).dp,
                            y = (31 + block.row * 40).dp,
                        )
                        .size(width = 71.dp, height = (block.height * 40 - 2).dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(block.color)
                        .padding(8.dp),
                ) {
                    Column {
                        Text(
                            text = block.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = Color(0xFFFBF7F2),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "sala ${block.room}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = Color(0xFFFBF7F2).copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

// ──────────────── Grades (rising bars) ────────────────

private data class Bar(val targetH: Int, val color: Color, val label: String)

private val GRADE_BARS = listOf(
    Bar(55, Amber, "7.5"),
    Bar(72, Coral, "8.8"),
    Bar(88, Magenta, "9.4"),
    Bar(65, Amber, "8.1"),
    Bar(80, Coral, "9.0"),
)

@Composable
fun GradesIllustration() {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .size(IllustrationSize)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FadeUpDelayed(delayMs = 100) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "8",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 92.sp,
                        lineHeight = 92.sp,
                        letterSpacing = (-3.7).sp,
                    ),
                    color = ink,
                )
                Text(
                    text = ",",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 92.sp,
                        lineHeight = 92.sp,
                    ),
                    color = accent,
                )
                Text(
                    text = "5",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 92.sp,
                        lineHeight = 92.sp,
                        letterSpacing = (-3.7).sp,
                    ),
                    color = ink,
                )
                Text(
                    text = "/10",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 28.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                    color = ink3,
                    modifier = Modifier.padding(start = 2.dp, bottom = 12.dp),
                )
            }
        }

        FadeInDelayed(delayMs = 300) {
            Text(
                text = "COEFICIENTE · 2026.1",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                color = ink3,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Row(
            modifier = Modifier
                .padding(top = 28.dp)
                .height(108.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GRADE_BARS.forEachIndexed { i, bar ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FadeInDelayed(delayMs = 600 + i * 100) {
                        Text(
                            text = bar.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                            color = ink3,
                        )
                    }
                    GrowingBar(targetHeight = bar.targetH.dp, color = bar.color, delayMs = 300 + i * 80)
                }
            }
        }
    }
}

@Composable
private fun GrowingBar(targetHeight: androidx.compose.ui.unit.Dp, color: Color, delayMs: Int) {
    var triggered by remember { mutableStateOf(false) }
    val height by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (triggered) targetHeight else 0.dp,
        animationSpec = tween(700, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)),
        label = "bar-grow",
    )
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        triggered = true
    }
    Box(
        Modifier
            .width(28.dp)
            .height(height)
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
            .background(color),
    )
}

// ──────────────── Messages (notification cards) ────────────────

private data class Msg(val from: String, val preview: String, val color: Color, val time: String, val unread: Boolean)

private val MESSAGES = listOf(
    Msg("Prof. Adriana", "Gabarito da P1 liberado", Magenta, "ag.", true),
    Msg("Coordenação CC", "Matrícula em optativas", Coral, "09:14", false),
    Msg("DCE UEFS", "Assembleia geral quinta…", Amber, "ont.", false),
)

@Composable
fun MessagesIllustration() {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.melon.surface.line
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .size(IllustrationSize)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MESSAGES.forEachIndexed { i, m ->
            val tilt = if (i % 2 == 0) -1.5f else 1.5f
            FadeUpDelayed(delayMs = 100 + i * 150) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .rotate(tilt)
                        .clip(RoundedCornerShape(18.dp))
                        .background(surface)
                        .border(1.dp, line, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(m.color),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = m.from.first().toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                            color = Color(0xFFFBF7F2),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = m.from,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = ink,
                            )
                            Text(
                                text = m.time,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = ink3,
                            )
                        }
                        Text(
                            text = m.preview,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = ink3,
                            maxLines = 1,
                        )
                    }
                    if (m.unread) {
                        PulsingDot(color = accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse-dot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Box(
        Modifier
            .size(8.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}

// ──────────────── Notifications (push stack) ────────────────

private data class Push(val app: String, val title: String, val body: String, val chip: Color)

private val PUSHES = listOf(
    Push("Nota publicada", "CÁLCULO II · P2", "8,7 lançado por Prof. Ribamar", Amber),
    Push("Novo recado", "Prof. Adriana Souza", "Gabarito da P1 está disponível no mural.", Magenta),
    Push("Mudança de horário", "ALGI II · quinta", "Remanejada: sala 204 → sala 312", Coral),
    Push("Material novo", "FÍSICA II", "Lista de exercícios · cap. 7", Plum),
)

@Composable
fun NotificationsIllustration() {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val line = MaterialTheme.melon.surface.line

    Column(
        modifier = Modifier.size(IllustrationSize).padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FadeInDelayed(delayMs = 50) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "9:14",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = ink,
                )
                Text(
                    text = "QUI · 23 ABR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 1.6.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = ink3,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            PUSHES.forEachIndexed { i, p ->
                FadeUpDelayed(delayMs = 150 + i * 110) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(surface)
                            .border(1.dp, line, RoundedCornerShape(14.dp))
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(p.chip),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = p.app.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        letterSpacing = 1.2.sp,
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = ink3,
                                )
                                Text(
                                    text = "agora",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = ink3,
                                )
                            }
                            Text(
                                text = p.title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = ink,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                            Text(
                                text = p.body,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = ink3,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────── helpers ────────────────

@Composable
private fun FadeUpDelayed(delayMs: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) +
            slideInVertically(animationSpec = tween(500), initialOffsetY = { it / 4 }),
    ) { content() }
}

@Composable
private fun FadeInDelayed(delayMs: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(400))) { content() }
}

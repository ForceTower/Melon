package dev.forcetower.unes.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Folio — the paper-corner mascot from the iOS `FolioSprite` (Concept 02 in
// `mascot-concepts.jsx`), translated into a Compose Canvas drawer. Five poses
// cover the Chrome-dino style runner: Idle, RunA/RunB, Jump, Duck. The drawing
// is authored on a 200×200 viewBox; callers pass a destination rect and the
// drawer scales into it.
enum class FolioPose { Idle, RunA, RunB, Jump, Duck }

// Fixed paper-themed palette, kept in sync with iOS `FolioSprite`. Not adaptive
// — the runner is its own paper-cutout world that should read identically in
// light and dark, so we expose it as a dedicated palette rather than routing
// through `MaterialTheme.melon.brand` (whose hexes don't match exactly).
object FolioPalette {
    val cream = Color(0xFFFBF7F2)
    val creamDark = Color(0xFFE9E0D2)
    val ink = Color(0xFF1A1420)
    val coral = Color(0xFFE85D4E)
    val plum = Color(0xFF2D1B4E)
    val amber = Color(0xFFF4A23C)
    val peach = Color(0xFFFBD9A8)
}

private data class FolioConfig(
    val bodyY: Float,
    val bodyR: Float,
    val rippleA: Float,
    val squash: Float,
    val earR: Float,
)

private fun configFor(pose: FolioPose): FolioConfig = when (pose) {
    FolioPose.Idle -> FolioConfig(0f, 0f, 0f, 1.0f, 0f)
    FolioPose.RunA -> FolioConfig(-2f, -2f, 6f, 1.02f, -3f)
    FolioPose.RunB -> FolioConfig(-2f, 2f, -6f, 1.02f, 3f)
    FolioPose.Jump -> FolioConfig(-22f, -6f, 0f, 1.06f, -8f)
    FolioPose.Duck -> FolioConfig(12f, 0f, 0f, 0.6f, 0f)
}

// Canvas drawer used both by the runner's main scene canvas and by the static
// `FolioSpriteView`. `frame` is in the parent canvas's coordinate space.
fun DrawScope.drawFolio(
    pose: FolioPose,
    frame: Rect,
    blink: Boolean = false,
) {
    val cfg = configFor(pose)
    val isJump = pose == FolioPose.Jump
    val isDuck = pose == FolioPose.Duck
    // Eyes are drawn closed for duck (matches the squash) and whenever the
    // caller asks for a blink — duck is used by the game, blink by the idle
    // empty-state mascot.
    val eyesClosed = isDuck || blink

    withTransform({
        translate(frame.left, frame.top)
        scale(frame.width / 200f, frame.height / 200f, pivot = Offset.Zero)
    }) {
        // Shadow on the ground line — shrinks while airborne.
        val shadowCY = if (isJump) 178f else 174f
        val shadowRX = if (isJump) 22f else 38f
        drawOval(
            color = Color.Black.copy(alpha = 0.14f),
            topLeft = Offset(100f - shadowRX, shadowCY - 4f),
            size = Size(shadowRX * 2f, 8f),
        )

        // Body transform — translate, rotate around (100, 100+bodyY), squash.
        // Mirrors the SVG's nested <g transform> so the dog-ear and rule lines
        // move with the page.
        withTransform({
            translate(100f, 100f + cfg.bodyY)
            rotate(cfg.bodyR, pivot = Offset.Zero)
            scale(1f, cfg.squash, pivot = Offset.Zero)
            translate(-100f, -100f)
        }) {
            val bottomBase = if (isDuck) 130f else 145f
            val bottomMid = if (isDuck) 134f else 149f
            val bottomCurve = if (isDuck) 138f else 153f
            val paper = Path().apply {
                moveTo(50f, 50f)
                lineTo(138f, 50f)
                lineTo(152f, 64f)
                lineTo(152f, bottomBase)
                quadraticTo(130f, bottomCurve, 100f, bottomMid)
                quadraticTo(70f, bottomCurve, 50f, bottomMid)
                close()
            }
            drawPath(paper, color = FolioPalette.cream)
            drawPath(
                paper,
                color = FolioPalette.ink,
                style = Stroke(width = 2.5f, join = StrokeJoin.Round),
            )

            // Folded corner (dog ear) — pivots around its own hinge so the run
            // cycle wags it independently of the body squash.
            withTransform({
                translate(145f, 57f)
                rotate(cfg.earR, pivot = Offset.Zero)
                translate(-145f, -57f)
            }) {
                val earPath = Path().apply {
                    moveTo(138f, 50f)
                    lineTo(152f, 64f)
                    lineTo(138f, 64f)
                    close()
                }
                drawPath(earPath, color = FolioPalette.creamDark)
                drawPath(
                    earPath,
                    color = FolioPalette.ink,
                    style = Stroke(width = 2f),
                )
            }

            // Coral margin rule.
            val marginEnd = if (isDuck) 132f else 144f
            val margin = Path().apply {
                moveTo(68f, 58f)
                lineTo(68f, marginEnd)
            }
            drawPath(
                margin,
                color = FolioPalette.coral,
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )

            // Faint horizontal rule lines.
            for (y in listOf(78f, 92f, 106f)) {
                val line = Path().apply {
                    moveTo(78f, y)
                    lineTo(148f, y)
                }
                drawPath(
                    line,
                    color = FolioPalette.ink.copy(alpha = 0.10f),
                    style = Stroke(width = 1f),
                )
            }

            if (!eyesClosed) {
                drawOval(
                    color = FolioPalette.ink,
                    topLeft = Offset(98f - 3.5f, 86f - 3.5f),
                    size = Size(7f, 7f),
                )
                drawOval(
                    color = FolioPalette.ink,
                    topLeft = Offset(120f - 3.5f, 86f - 3.5f),
                    size = Size(7f, 7f),
                )
                drawOval(
                    color = FolioPalette.cream,
                    topLeft = Offset(99f - 1f, 85f - 1f),
                    size = Size(2f, 2f),
                )
                drawOval(
                    color = FolioPalette.cream,
                    topLeft = Offset(121f - 1f, 85f - 1f),
                    size = Size(2f, 2f),
                )
            } else {
                // Sit lids at the open-eye Y in non-duck poses so a blink looks
                // like eyes shutting in place; duck keeps the squashed-head
                // lower position.
                val lidY = if (isDuck) 92f else 86f
                val leftLid = Path().apply {
                    moveTo(94f, lidY)
                    lineTo(102f, lidY)
                }
                drawPath(
                    leftLid,
                    color = FolioPalette.ink,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )
                val rightLid = Path().apply {
                    moveTo(116f, lidY)
                    lineTo(124f, lidY)
                }
                drawPath(
                    rightLid,
                    color = FolioPalette.ink,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )
            }

            val smile = Path().apply {
                moveTo(102f, 100f)
                quadraticTo(109f, 104f, 116f, 100f)
            }
            drawPath(
                smile,
                color = FolioPalette.ink,
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )
        }

        // Rippled bottom edge as legs — drawn on the parent (sprite) transform,
        // not the body's, so the squash doesn't deform the gait.
        if (!isJump && !isDuck) {
            withTransform({
                translate(0f, cfg.bodyY * 0.6f)
            }) {
                val legPath = Path().apply {
                    moveTo(70f, 152f)
                    quadraticTo(78f, 162f + cfg.rippleA, 86f, 152f)
                    quadraticTo(94f, 162f - cfg.rippleA, 102f, 152f)
                    quadraticTo(110f, 162f + cfg.rippleA, 118f, 152f)
                    quadraticTo(126f, 162f - cfg.rippleA, 134f, 152f)
                }
                drawPath(
                    legPath,
                    color = FolioPalette.ink,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

// Static SwiftUI-style wrapper — used outside the running game's Canvas (e.g.
// the runner's start overlay and the schedule empty-state mascot).
@Composable
fun FolioSpriteView(
    pose: FolioPose,
    modifier: Modifier = Modifier,
    size: Dp = 90.dp,
    blink: Boolean = false,
) {
    Canvas(modifier = modifier.size(size)) {
        drawFolio(
            pose = pose,
            frame = Rect(0f, 0f, this.size.width, this.size.height),
            blink = blink,
        )
    }
}

// Idle Folio that closes its eyes once or twice every ~25 seconds. Used as the
// schedule empty-state mascot and as the long-press target for the runner
// easter egg.
@Composable
fun BlinkingFolio(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            // Random gap (avg ~25s) between blink events. Re-rolled each loop
            // so the rhythm doesn't feel mechanical.
            delay(Random.nextLong(18_000L, 32_000L))
            blink = true
            delay(140L)
            blink = false
            // Roughly a third of the time, follow up with a quick double blink
            // — feels natural without becoming twitchy.
            if (Random.nextInt(3) == 0) {
                delay(110L)
                blink = true
                delay(140L)
                blink = false
            }
        }
    }
    FolioSpriteView(pose = FolioPose.Idle, size = size, blink = blink, modifier = modifier)
}

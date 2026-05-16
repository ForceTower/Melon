package dev.forcetower.unes.ui.feature.foliorunner

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.forcetower.unes.designsystem.components.FolioPalette
import dev.forcetower.unes.designsystem.components.FolioPose
import dev.forcetower.unes.designsystem.components.drawFolio
import kotlin.math.min
import kotlin.random.Random

// Engine state for the Chrome-dino style runner. Mirrors the iOS
// `FolioRunnerEngine` — a plain class (not @Stable-derived) whose Compose-
// observed surface is just three values: phase, score, bestScore. Per-frame
// kinematics live in plain Float fields and are mutated from `advance`, which
// the screen calls from a `withFrameNanos` loop. `sceneTick` is bumped each
// advance so the Canvas (whose draw closure has no other observable state to
// read) re-runs every frame.
@Stable
internal class FolioRunnerEngine(initialBest: Int) {
    enum class Phase { Ready, Playing, GameOver }

    var phase by mutableStateOf(Phase.Ready)
        private set
    var score by mutableIntStateOf(0)
        private set
    var bestScore by mutableIntStateOf(initialBest)
        private set
    var sceneTick by mutableIntStateOf(0)
        private set

    // Folio kinematics — y is vertical offset above the ground in dp,
    // velocity is in dp/second. The screen scales the canvas by density before
    // drawing so the engine never has to know about pixel density.
    private var folioY = 0f
    private var folioVelocity = 0f
    private var isDucking = false

    private val obstacles = mutableListOf<Obstacle>()
    private var nextSpawnIn = 1.2

    private var elapsed = 0.0
    private var groundOffset = 0f
    private var cloudOffset = 0f
    private var runFrame = false
    private var runFlipIn = 0.13
    private var restartLockUntilMs: Long? = null

    private data class Obstacle(var x: Float, val kind: Kind) {
        enum class Kind { Books, Plane }
    }

    // MARK: Input

    fun tap() {
        when (phase) {
            Phase.Ready -> {
                phase = Phase.Playing
                jump()
            }
            Phase.Playing -> jump()
            Phase.GameOver -> {
                // Brief grace period after death so the deciding tap doesn't
                // immediately restart and drop you onto the same obstacle.
                val until = restartLockUntilMs
                if (until != null && System.currentTimeMillis() < until) return
                reset()
                phase = Phase.Playing
            }
        }
    }

    fun beginDuck() {
        if (phase != Phase.Playing) return
        isDucking = true
    }

    fun endDuck() {
        isDucking = false
    }

    private fun jump() {
        if (folioY > 0.001f) return
        folioVelocity = JumpVelocity
    }

    private fun reset() {
        score = 0
        elapsed = 0.0
        folioY = 0f
        folioVelocity = 0f
        isDucking = false
        obstacles.clear()
        nextSpawnIn = 1.2
        groundOffset = 0f
        cloudOffset = 0f
        runFrame = false
        runFlipIn = 0.13
    }

    // MARK: Tick

    fun advance(dtSeconds: Double, size: Size) {
        sceneTick++
        if (phase != Phase.Playing) return
        // Cap dt so a stutter doesn't teleport obstacles through Folio.
        val dt = min(dtSeconds, 1.0 / 30.0)
        val dtF = dt.toFloat()
        elapsed += dt
        val speed = currentSpeed()

        folioVelocity -= Gravity * dtF
        folioY = (folioY + folioVelocity * dtF).coerceAtLeast(0f)
        if (folioY == 0f) folioVelocity = 0f

        groundOffset = (groundOffset + speed * dtF) % 24f
        cloudOffset += speed * 0.18f * dtF

        runFlipIn -= dt
        if (runFlipIn <= 0) {
            runFrame = !runFrame
            runFlipIn = 0.12
        }

        for (i in obstacles.indices) {
            obstacles[i].x -= speed * dtF
        }
        obstacles.removeAll { it.x < -60f }

        nextSpawnIn -= dt
        if (nextSpawnIn <= 0) {
            spawnObstacle(size)
            val base = 320.0 / speed.toDouble()
            nextSpawnIn = Random.nextDouble(base, base + 0.8)
        }

        val folioBox = folioCollisionRect(size)
        for (obs in obstacles) {
            if (folioBox.overlaps(obstacleCollisionRect(obs, size))) {
                gameOver()
                return
            }
        }

        score = (elapsed * 12).toInt()
    }

    // Linear ramp from 230dp/s → ~545dp/s over 90s, then flat.
    private fun currentSpeed(): Float = 230f + min(elapsed, 90.0).toFloat() * 3.5f

    private fun spawnObstacle(size: Size) {
        // Books (ground) only for the first stretch; planes (sky) mix in once
        // the speed has ramped enough that ducking matters.
        val kind = when {
            elapsed < 8.0 -> Obstacle.Kind.Books
            Random.nextInt(3) == 0 -> Obstacle.Kind.Plane
            else -> Obstacle.Kind.Books
        }
        obstacles.add(Obstacle(x = size.width + 40f, kind = kind))
    }

    private fun gameOver() {
        phase = Phase.GameOver
        // Snap to the ground so a death mid-jump doesn't leave Folio floating
        // in a squashed pose.
        folioY = 0f
        folioVelocity = 0f
        isDucking = false
        if (score > bestScore) {
            bestScore = score
        }
        restartLockUntilMs = System.currentTimeMillis() + 600L
    }

    // MARK: Geometry

    private fun groundLine(size: Size): Float = size.height * 0.78f

    private fun folioCollisionRect(size: Size): Rect {
        val footY = groundLine(size)
        val cx = 84f
        return if (isDucking && phase == Phase.Playing) {
            // Ducking shrinks the hitbox to roughly the lower half of the
            // sprite so the plane (sky obstacle) clears overhead.
            val w = 56f
            val h = 38f
            Rect(cx - w / 2f, footY - h - 4f, cx + w / 2f, footY - 4f)
        } else {
            val w = 50f
            val h = 64f
            val topY = footY - h - 6f - folioY
            Rect(cx - w / 2f, topY, cx + w / 2f, topY + h)
        }
    }

    private fun obstacleCollisionRect(obs: Obstacle, size: Size): Rect {
        val footY = groundLine(size)
        return when (obs.kind) {
            Obstacle.Kind.Books -> Rect(obs.x - 14f, footY - 42f, obs.x + 14f, footY)
            Obstacle.Kind.Plane -> Rect(obs.x - 16f, footY - 84f, obs.x + 16f, footY - 66f)
        }
    }

    // MARK: Render

    // `tick` isn't used by the renderer — it's read by the caller inside the
    // Canvas draw block so the snapshot system observes `sceneTick` and
    // reschedules a redraw on each advance. Forwarding it through the call
    // keeps the read side-effect-free at the call site.
    fun render(scope: DrawScope, size: Size, tick: Int = 0) {
        check(tick >= 0)
        val footY = groundLine(size)
        drawClouds(scope, size)

        // Ground line.
        scope.drawLine(
            color = FolioPalette.ink.copy(alpha = 0.35f),
            start = Offset(0f, footY),
            end = Offset(size.width, footY),
            strokeWidth = 1f,
        )

        // Hash marks below the ground line — the dino-game scrolling texture.
        // Each tick is 8dp wide on a 24dp cycle.
        var x = -groundOffset
        while (x < size.width) {
            scope.drawLine(
                color = FolioPalette.ink.copy(alpha = 0.25f),
                start = Offset(x, footY + 5f),
                end = Offset(x + 8f, footY + 5f),
                strokeWidth = 1f,
                cap = StrokeCap.Round,
            )
            x += 24f
        }

        // Sparse pebbles, scrolled slightly faster than the hash marks for a
        // parallax cue.
        val pebbleSpan = size.width + 60f
        for (i in 0 until 4) {
            var px = (i * 180f - groundOffset * 3.2f) % pebbleSpan
            if (px < 0f) px += pebbleSpan
            scope.drawOval(
                color = FolioPalette.ink.copy(alpha = 0.4f),
                topLeft = Offset(px, footY + 9f),
                size = Size(4f, 2f),
            )
        }

        for (obs in obstacles) {
            drawObstacle(scope, obs, size)
        }

        val pose = currentPose()
        // Duck pose is drawn slightly larger so the squashed paper still reads
        // at the same visual weight as the upright run.
        val baseSize = if (pose == FolioPose.Duck) FolioSize * 1.05f else FolioSize
        val cx = 84f
        val extraDuckDrop = if (pose == FolioPose.Duck) 6f else 0f
        val yTop = footY - baseSize + 16f - folioY + extraDuckDrop
        scope.drawFolio(
            pose = pose,
            frame = Rect(cx - baseSize / 2f, yTop, cx + baseSize / 2f, yTop + baseSize),
        )
    }

    private fun currentPose(): FolioPose = when (phase) {
        Phase.Ready -> FolioPose.Idle
        Phase.GameOver -> FolioPose.Duck
        Phase.Playing -> when {
            folioY > 0f -> FolioPose.Jump
            isDucking -> FolioPose.Duck
            runFrame -> FolioPose.RunA
            else -> FolioPose.RunB
        }
    }

    private fun drawClouds(scope: DrawScope, size: Size) {
        // Two slow-moving paper-cutout clouds. The shape is a single bumpy
        // curve closed along the bottom — cheap and reads as a friendly cloud
        // at small sizes.
        val span = size.width + 120f
        var ax = (140f - cloudOffset) % span
        if (ax < -60f) ax += span
        var bx = (-180f - cloudOffset * 1.5f) % span
        if (bx < -60f) bx += span

        scope.drawPath(
            cloudPath(Offset(ax, 90f), 1.1f),
            color = FolioPalette.ink.copy(alpha = 0.16f),
        )
        scope.drawPath(
            cloudPath(Offset(bx, 140f), 0.9f),
            color = FolioPalette.ink.copy(alpha = 0.12f),
        )
    }

    private fun cloudPath(origin: Offset, scale: Float): Path = Path().apply {
        val x = origin.x
        val y = origin.y
        moveTo(x + 6f * scale, y + 14f * scale)
        quadraticTo(x + 4f * scale, y + 4f * scale, x + 14f * scale, y + 6f * scale)
        quadraticTo(x + 20f * scale, y - 2f * scale, x + 28f * scale, y + 4f * scale)
        quadraticTo(x + 40f * scale, y - 1f * scale, x + 44f * scale, y + 8f * scale)
        quadraticTo(x + 52f * scale, y + 8f * scale, x + 50f * scale, y + 14f * scale)
        close()
    }

    private fun drawObstacle(scope: DrawScope, obs: Obstacle, size: Size) {
        val footY = groundLine(size)
        val inkStroke = Stroke(width = 1.5f, join = StrokeJoin.Round)
        when (obs.kind) {
            Obstacle.Kind.Books -> {
                // A small stack of textbooks in the brand palette — each
                // slightly narrower than the one below it.
                val books = listOf(
                    Triple(Rect(obs.x - 16f, footY - 14f, obs.x + 16f, footY), 32f, FolioPalette.coral),
                    Triple(Rect(obs.x - 14f, footY - 28f, obs.x + 14f, footY - 14f), 28f, FolioPalette.amber),
                    Triple(Rect(obs.x - 12f, footY - 42f, obs.x + 12f, footY - 28f), 24f, FolioPalette.plum),
                )
                for ((rect, _, color) in books) {
                    scope.drawRoundRect(
                        color = color,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                    )
                    scope.drawRoundRect(
                        color = FolioPalette.ink,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
                        style = inkStroke,
                    )
                    // Thin accent stripe near the spine for a book-cover cue.
                    scope.drawLine(
                        color = FolioPalette.cream.copy(alpha = 0.7f),
                        start = Offset(rect.left + 4f, rect.top + 4f),
                        end = Offset(rect.left + 4f, rect.bottom - 4f),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            Obstacle.Kind.Plane -> {
                // Paper airplane gliding nose-left.
                val baseY = footY - 78f
                val body = Path().apply {
                    moveTo(obs.x - 18f, baseY + 9f)
                    lineTo(obs.x + 16f, baseY)
                    lineTo(obs.x + 4f, baseY + 9f)
                    lineTo(obs.x + 16f, baseY + 18f)
                    close()
                }
                scope.drawPath(body, color = FolioPalette.cream)
                scope.drawPath(body, color = FolioPalette.ink, style = inkStroke)

                val crease = Path().apply {
                    moveTo(obs.x + 16f, baseY)
                    lineTo(obs.x + 4f, baseY + 9f)
                }
                scope.drawPath(
                    crease,
                    color = FolioPalette.ink.copy(alpha = 0.4f),
                    style = Stroke(width = 1f),
                )
            }
        }
    }

    companion object {
        private const val Gravity = 1900f
        private const val JumpVelocity = 760f
        private const val FolioSize = 92f
    }
}

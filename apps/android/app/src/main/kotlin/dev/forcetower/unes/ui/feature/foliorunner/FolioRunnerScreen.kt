package dev.forcetower.unes.ui.feature.foliorunner

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.FolioPalette
import kotlin.math.sqrt

// Easter-egg Chrome-dino style runner starring Folio. Reachable from the
// schedule's `EmptyDay` — long-press the "Nenhuma aula" mascot on a free day to
// open it as a full-screen route. Mirrors `FolioRunnerView` on iOS.
@Composable
internal fun FolioRunnerScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(BestScorePrefs, Context.MODE_PRIVATE)
    }
    val engine = remember { FolioRunnerEngine(initialBest = prefs.getInt(BestScoreKey, 0)) }
    val density = LocalDensity.current.density
    val duckThresholdPx = with(LocalDensity.current) { 28.dp.toPx() }
    val tapMaxMovementPx = with(LocalDensity.current) { 10.dp.toPx() }

    // Persist the new best when it climbs. Re-keying on `engine.bestScore`
    // restarts the effect on every change — the body just writes the value.
    LaunchedEffect(engine.bestScore) {
        prefs.edit().putInt(BestScoreKey, engine.bestScore).apply()
    }

    // Viewport in dp — written by the Canvas's onSizeChanged-equivalent (read
    // inside the draw scope, since DrawScope is the only place that knows the
    // pixel size), read by the frame loop to advance the engine.
    var viewportDp by remember { mutableStateOf(Size.Zero) }

    // Frame loop. `withFrameNanos` yields the next vsync; we feed dt to the
    // engine. Engine state mutations are observable, so HUD and overlays
    // recompose on phase/score, and the Canvas redraws when sceneTick bumps.
    LaunchedEffect(engine) {
        var lastNanos: Long? = null
        while (true) {
            withFrameNanos { now ->
                val last = lastNanos
                lastNanos = now
                if (last != null && viewportDp != Size.Zero) {
                    val dt = (now - last) / 1_000_000_000.0
                    engine.advance(dtSeconds = dt, size = viewportDp)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FolioPalette.cream)
            .pointerInput(engine) {
                // Single gesture handles both inputs: a stationary press +
                // release is the jump tap; pulling down past the threshold
                // puts Folio in a duck for the duration of the touch. Mirrors
                // iOS DragGesture(minimumDistance: 0).
                awaitEachGesture {
                    // requireUnconsumed = true so the close button's consume()
                    // properly filters its taps out of the engine's input.
                    val down = awaitFirstDown(requireUnconsumed = true)
                    var ducked = false
                    var lastPosition = down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.position - down.position
                        if (!ducked && delta.y > duckThresholdPx) {
                            engine.beginDuck()
                            ducked = true
                        }
                        lastPosition = change.position
                        if (change.changedToUp()) break
                    }
                    engine.endDuck()
                    val total = lastPosition - down.position
                    val movement = sqrt(total.x * total.x + total.y * total.y)
                    if (movement < tapMaxMovementPx) {
                        engine.tap()
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nextViewport = Size(size.width / density, size.height / density)
            if (viewportDp != nextViewport) viewportDp = nextViewport
            withTransform({ scale(density, density, pivot = Offset.Zero) }) {
                engine.render(this, nextViewport, tick = engine.sceneTick)
            }
        }

        Hud(score = engine.score, bestScore = engine.bestScore)

        when (engine.phase) {
            FolioRunnerEngine.Phase.Ready -> ReadyOverlay()
            FolioRunnerEngine.Phase.Playing -> Unit
            FolioRunnerEngine.Phase.GameOver -> GameOverOverlay(score = engine.score)
        }

        CloseButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 6.dp),
        )
    }
}

@Composable
private fun BoxScope.Hud(score: Int, bestScore: Int) {
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(end = 22.dp, top = 12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (bestScore > 0) {
            Text(
                text = "HI ${formatScore(bestScore)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = FolioPalette.ink.copy(alpha = 0.4f),
            )
        }
        Text(
            text = formatScore(score),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = FolioPalette.ink.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun BoxScope.ReadyOverlay() {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .padding(bottom = 220.dp, start = 40.dp, end = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.folio_runner_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 38.sp,
                fontStyle = FontStyle.Italic,
            ),
            color = FolioPalette.ink,
        )
        Text(
            text = stringResource(R.string.folio_runner_hint),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                letterSpacing = 0.2.sp,
            ),
            color = FolioPalette.ink.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoxScope.GameOverOverlay(score: Int) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .padding(bottom = 220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.folio_runner_game_over),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 34.sp,
                fontStyle = FontStyle.Italic,
            ),
            color = FolioPalette.ink,
        )
        Text(
            text = formatScore(score),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = FolioPalette.ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.folio_runner_retry_hint),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                letterSpacing = 0.2.sp,
            ),
            color = FolioPalette.ink.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(FolioPalette.ink.copy(alpha = 0.06f))
            .pointerInput(onClick) {
                // Detect tap and consume so the parent's tap-vs-drag gesture
                // doesn't ALSO fire a tap on the engine.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        change.consume()
                        if (change.changedToUp()) {
                            onClick()
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✕",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = FolioPalette.ink.copy(alpha = 0.6f),
        )
    }
}

private fun formatScore(score: Int): String = "%05d".format(score)

private const val BestScorePrefs = "folio_runner"
private const val BestScoreKey = "best_score"

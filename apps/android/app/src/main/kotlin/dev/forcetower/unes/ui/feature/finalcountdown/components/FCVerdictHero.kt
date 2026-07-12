package dev.forcetower.unes.ui.feature.finalcountdown.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.FCVerdict
import dev.forcetower.unes.ui.feature.finalcountdown.FCVerdictFamily
import dev.forcetower.unes.ui.feature.finalcountdown.FCVerdictStyle
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownMath

// Verdict hero — an always-dark mesh card tinted by the outcome family, with
// the drawn average ring on the right and the stat/detail strip at the bottom.
// Mirrors the dc `FinalCountdownScreen` hero block.
@Composable
internal fun FCVerdictHero(
    verdict: FCVerdict,
    style: FCVerdictStyle,
    weighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.melon.verdict
    val palette = when (style.family) {
        FCVerdictFamily.Passed -> tokens.passed
        FCVerdictFamily.Track -> tokens.track
        FCVerdictFamily.Warn -> tokens.warn
        FCVerdictFamily.Ember -> tokens.ember
        FCVerdictFamily.Lost -> tokens.lost
    }
    val onHero = MaterialTheme.melon.fixed.onHero
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, spotColor = tokens.night, ambientColor = tokens.night)
            .clip(shape)
            .background(tokens.night),
    ) {
        Mesh(colors = palette.blobs, modifier = Modifier.matchParentSize())
        // Diagonal legibility scrim over the blobs (dc: 155°, 12% → 64%).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            tokens.veil.copy(alpha = 0.12f),
                            tokens.veil.copy(alpha = 0.64f),
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                ),
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(palette.hue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = tokens.night,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = style.eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.48.sp,
                        ),
                        color = onHero,
                    )
                }
                Text(
                    text = stringResource(
                        if (weighted) R.string.final_countdown_hero_mode_weighted
                        else R.string.final_countdown_hero_mode_simple,
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = onHero.copy(alpha = 0.55f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(onHero.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = style.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 29.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp,
                        ),
                        color = onHero,
                    )
                    Text(
                        text = style.lead,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.14).sp,
                        ),
                        color = onHero.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                AverageRing(avg = verdict.avg, hue = palette.hue, onHero = onHero)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .topHairline(onHero.copy(alpha = 0.14f))
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column {
                    Text(
                        text = style.statLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.42.sp,
                        ),
                        color = onHero.copy(alpha = 0.52f),
                    )
                    Text(
                        text = style.statValue,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 26.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.78).sp,
                        ),
                        color = palette.hue,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Text(
                    text = style.detail,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = onHero.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
            }

            if (style.showScale) {
                FinalScale(
                    need = verdict.need ?: 0.0,
                    onHero = onHero,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

// dc hero ring: a 74% arc opening at the bottom, track at 14% white, fill
// proportional to média/10, the truncated average + "média" in the middle.
@Composable
private fun AverageRing(avg: Double?, hue: Color, onHero: Color) {
    val fraction by animateFloatAsState(
        targetValue = avg?.let { (it / 10.0).toFloat().coerceIn(0f, 1f) } ?: 0f,
        animationSpec = MelonMotion.easeSlow(),
        label = "fcRingFill",
    )
    Box(modifier = Modifier.size(98.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            val inset = 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            // Arc covers 74% of the circle; the 26% gap is centered at the
            // bottom (dc: rotate 90° plus half the gap).
            val gapSweep = 0.26f * 360f
            val start = 90f + gapSweep / 2f
            val fullSweep = 360f - gapSweep
            drawArc(
                color = onHero.copy(alpha = 0.14f),
                startAngle = start,
                sweepAngle = fullSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            if (fraction > 0f) {
                drawArc(
                    color = hue,
                    startAngle = start,
                    sweepAngle = fullSweep * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = FinalCountdownMath.formatGrade(avg),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.2).sp,
                ),
                color = onHero,
            )
            Text(
                text = stringResource(R.string.final_countdown_hero_avg_caption).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.38.sp,
                ),
                color = onHero.copy(alpha = 0.62f),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

// Prova Final difficulty scale (Final verdict only): a green→amber→ember
// gradient rail with a white marker at the needed grade, captioned
// fácil/cruel/brutal.
@Composable
private fun FinalScale(need: Double, onHero: Color, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.melon.verdict
    val markerBias by animateFloatAsState(
        // Fraction 0..1 mapped onto BiasAlignment's -1..1 range.
        targetValue = (need / 10.0).toFloat().coerceIn(0f, 1f) * 2f - 1f,
        animationSpec = MelonMotion.spring(),
        label = "fcScaleMarker",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                tokens.passed.hue.copy(alpha = 0.55f),
                                tokens.warn.hue.copy(alpha = 0.75f),
                                tokens.ember.hue.copy(alpha = 0.9f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = markerBias, verticalBias = 0f))
                    .size(width = 3.dp, height = 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(onHero),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(
                R.string.final_countdown_scale_low,
                R.string.final_countdown_scale_mid,
                R.string.final_countdown_scale_high,
            ).forEach { res ->
                Text(
                    text = stringResource(res).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                    ),
                    color = onHero.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// Hairline above the stat strip — drawn so it hugs the row's width inside the
// padded column without a divider composable claiming layout space.
private fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    drawLine(
        color = color,
        start = Offset.Zero,
        end = Offset(size.width, 0f),
        strokeWidth = 1.dp.toPx(),
    )
}

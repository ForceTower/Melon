package dev.forcetower.unes.ui.feature.finalcountdown.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.FCTone
import dev.forcetower.unes.ui.feature.finalcountdown.FCVerdict
import dev.forcetower.unes.ui.feature.finalcountdown.FCVerdictKind
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownMath
import dev.forcetower.unes.ui.feature.finalcountdown.fcCopyFor
import dev.forcetower.unes.ui.feature.finalcountdown.fcToneBackground
import dev.forcetower.unes.ui.feature.finalcountdown.fcToneForeground

// The dramatic gradient card at the top of the calculator. Status pill, the
// radial dial, the title lines, the big headline row (needed grade + sub +
// message). When the verdict is `Final`, also paints the horizontal
// difficulty meter at the bottom.
@Composable
internal fun FCVerdictHero(
    verdict: FCVerdict,
    weighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val copy = fcCopyFor(verdict)
    val toneBg = fcToneBackground(copy.tone)
    val toneFg = fcToneForeground(copy.tone)
    val heroInk = MaterialTheme.melon.fixed.surfaceLight
    val heroInkSoft = heroInk.copy(alpha = 0.55f)
    val heroInkBody = heroInk.copy(alpha = 0.85f)
    val backgroundBrush = backgroundGradient(verdict.kind)
    val italicLineColor = if (copy.tone == FCTone.Amber) MaterialTheme.melon.brand.peach else toneFg
    val headlineColor = headlineColor(tone = copy.tone, surfaceLight = heroInk, peach = MaterialTheme.melon.brand.peach)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
            .drawBehind {
                // Decorative corner glow — same recipe as iOS: a 160dp disc
                // bleeding past the top-right corner, clipped by the card's
                // rounded-rect mask.
                val glowRadiusPx = 80.dp.toPx()
                val center = Offset(x = size.width - 50.dp.toPx(), y = 50.dp.toPx())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(toneBg.copy(alpha = 0.33f), Color.Transparent),
                        center = center,
                        radius = glowRadiusPx,
                    ),
                    radius = glowRadiusPx,
                    center = center,
                )
            }
            .padding(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HeaderRow(
                eyebrow = copy.eyebrow,
                icon = copy.icon,
                weighted = weighted,
                toneBg = toneBg,
                toneFg = toneFg,
                heroInk = heroInk,
                heroInkSoft = heroInkSoft,
            )
            Spacer(modifier = Modifier.height(14.dp))

            FCMeter(
                avg = verdict.avg,
                tone = copy.tone,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            TitleBlock(
                titleLines = copy.titleLines,
                heroInk = heroInk,
                italicColor = italicLineColor,
                modifier = Modifier.padding(top = 4.dp),
            )

            HeadlineBlock(
                headline = copy.headline,
                sub = copy.sub,
                message = copy.message,
                headlineColor = headlineColor,
                heroInkSoft = heroInkSoft,
                heroInkBody = heroInkBody,
                modifier = Modifier.padding(top = 16.dp),
            )

            if (verdict.kind == FCVerdictKind.Final && verdict.need != null) {
                DifficultyMeter(
                    need = verdict.need,
                    heroInkFaint = heroInk.copy(alpha = 0.5f),
                    heroInk = heroInk,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(
    eyebrow: String,
    icon: dev.forcetower.unes.ui.feature.finalcountdown.FCVerdictIcon,
    weighted: Boolean,
    toneBg: Color,
    toneFg: Color,
    heroInk: Color,
    heroInkSoft: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(toneBg),
                contentAlignment = Alignment.Center,
            ) {
                FCVerdictGlyph(
                    icon = icon,
                    color = toneFg,
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 1.6f,
                )
            }
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.44.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = heroInk,
            )
        }
        Spacer(modifier = Modifier.width(0.dp))
        Box(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(
                if (weighted) R.string.final_countdown_hero_mode_weighted
                else R.string.final_countdown_hero_mode_simple,
            ).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.9.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = heroInkSoft,
        )
    }
}

@Composable
private fun TitleBlock(
    titleLines: List<String>,
    heroInk: Color,
    italicColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        titleLines.forEachIndexed { index, line ->
            if (line.isEmpty()) return@forEachIndexed
            val italic = index == 1
            Text(
                text = line,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.68).sp,
                    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                ),
                color = if (italic) italicColor.copy(alpha = 0.9f) else heroInk,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HeadlineBlock(
    headline: String,
    sub: String,
    message: String,
    headlineColor: Color,
    heroInkSoft: Color,
    heroInkBody: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 56.sp,
                lineHeight = 56.sp,
                letterSpacing = (-1.68).sp,
            ),
            color = headlineColor,
            maxLines = 1,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = sub.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    letterSpacing = 0.95.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = heroInkSoft,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
                color = heroInkBody,
            )
        }
    }
}

@Composable
private fun DifficultyMeter(
    need: Double,
    heroInkFaint: Color,
    heroInk: Color,
    modifier: Modifier = Modifier,
) {
    val coral = fcToneBackground(FCTone.Coral)
    val amber = fcToneBackground(FCTone.Amber)
    val green = fcToneBackground(FCTone.Green)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DifficultyTickLabel(
                text = stringResource(R.string.final_countdown_difficulty_low),
                color = heroInkFaint,
            )
            DifficultyTickLabel(
                text = stringResource(R.string.final_countdown_difficulty_mid),
                color = heroInkFaint,
            )
            DifficultyTickLabel(
                text = stringResource(R.string.final_countdown_difficulty_high),
                color = heroInkFaint,
            )
        }
        DifficultyBar(
            need = need,
            green = green.copy(alpha = 0.5f),
            amber = amber.copy(alpha = 0.7f),
            coral = coral.copy(alpha = 0.85f),
            indicator = heroInk,
        )
    }
}

@Composable
private fun DifficultyTickLabel(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 8.5.sp,
            letterSpacing = 1.19.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = color,
    )
}

@Composable
private fun DifficultyBar(
    need: Double,
    green: Color,
    amber: Color,
    coral: Color,
    indicator: Color,
) {
    val clamped = (need.coerceIn(0.0, 10.0) / 10.0).toFloat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(green, amber, coral),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .drawBehind {
                    val centerX = size.width * clamped
                    val widthPx = 3.dp.toPx()
                    val left = centerX - widthPx / 2f
                    val top = 0f
                    val bottom = size.height
                    drawRoundRect(
                        color = indicator,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(widthPx, bottom - top),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    )
                },
        )
    }
}

@Composable
private fun backgroundGradient(kind: FCVerdictKind): Brush {
    val v = MaterialTheme.melon.verdict
    val (top, bottom) = when (kind) {
        FCVerdictKind.Passed -> v.passedTop to v.passedBottom
        FCVerdictKind.Failed, FCVerdictKind.Impossible -> v.failedTop to v.failedBottom
        FCVerdictKind.Final, FCVerdictKind.BorderlineFinal, FCVerdictKind.FailingTrack ->
            v.finalTop to v.finalBottom
        FCVerdictKind.Borderline -> v.borderlineTop to v.borderlineBottom
        FCVerdictKind.OnTrack, FCVerdictKind.Empty -> v.neutralTop to v.neutralBottom
    }
    return Brush.linearGradient(
        colors = listOf(top, bottom),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
}

private fun headlineColor(tone: FCTone, surfaceLight: Color, peach: Color): Color {
    // Amber hero (borderline) reads better with peach on the panel; tones
    // whose `fg` is the cream surface invert to the tone's bg for contrast.
    if (tone == FCTone.Amber) return peach
    return surfaceLight
}

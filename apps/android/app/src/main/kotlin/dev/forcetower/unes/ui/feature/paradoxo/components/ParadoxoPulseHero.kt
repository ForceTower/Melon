package dev.forcetower.unes.ui.feature.paradoxo.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoPulseFact
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoRef
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.paradoxo.ParadoxoFormat
import kotlinx.coroutines.delay

private const val RotationMillis = 4_600L

// Rotating "pulso da universidade" hero — an always-dark mesh card that
// cycles through the curated facts every 4.6s (tap a dot to jump, tap the
// card to open the fact's discipline/teacher). Mirrors iOS
// `ParadoxoPulseHero` and the dc pulse card.
@Composable
internal fun ParadoxoPulseHero(
    facts: List<ParadoxoPulseFact>,
    onOpen: (ParadoxoRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (facts.isEmpty()) return
    var index by rememberSaveable(facts.size) { mutableIntStateOf(0) }
    val fact = facts[index.coerceIn(facts.indices)]

    LaunchedEffect(index, facts.size) {
        delay(RotationMillis)
        index = (index + 1) % facts.size
    }

    val night = MaterialTheme.melon.verdict.night
    val veil = MaterialTheme.melon.verdict.veil
    val onHero = MaterialTheme.melon.fixed.onHero
    val tone = paradoxoPulseTone(fact.kind)
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, spotColor = night, ambientColor = night)
            .clip(shape)
            .background(night)
            .clickable { onOpen(fact.ref) },
    ) {
        Mesh(variant = paradoxoPulseMesh(fact.kind), modifier = Modifier.matchParentSize())
        // Diagonal legibility scrim over the blobs (dc: 155°, 12% → 66%).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(veil.copy(alpha = 0.12f), veil.copy(alpha = 0.66f)),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)
                .animateContentSize(tween(280, easing = MelonMotion.EmphasizedEasing)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    // Live dot with the soft tone halo (dc: 0 0 0 3px tone40).
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .drawBehind {
                                drawCircle(color = tone.copy(alpha = 0.25f))
                                drawCircle(color = tone, radius = 3.5.dp.toPx())
                            },
                    )
                    Text(
                        text = paradoxoPulseLabel(fact.kind).uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.58.sp,
                        ),
                        color = onHero.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    facts.forEachIndexed { i, _ ->
                        val width by animateDpAsState(
                            targetValue = if (i == index) 15.dp else 5.dp,
                            animationSpec = MelonMotion.ease(),
                            label = "pulseDot",
                        )
                        Box(
                            modifier = Modifier
                                .width(width)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i == index) onHero else onHero.copy(alpha = 0.35f),
                                )
                                .clickable { index = i },
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = fact,
                transitionSpec = {
                    val ease = tween<Float>(280, easing = MelonMotion.EmphasizedEasing)
                    (fadeIn(ease) + slideInVertically { it / 6 }).togetherWith(fadeOut(ease))
                },
                contentKey = { it.id },
                label = "pulseFlip",
            ) { current ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = ParadoxoFormat.metric(current),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 62.sp,
                                lineHeight = 56.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-3.1).sp,
                            ),
                            color = onHero,
                        )
                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.36).sp,
                            ),
                            color = onHero,
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 4.dp),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .drawBehind {
                                drawLine(
                                    color = onHero.copy(alpha = 0.16f),
                                    start = Offset.Zero,
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                            .padding(top = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = current.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = onHero.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.paradoxo_pulse_explore),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = onHero,
                            )
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = onHero,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

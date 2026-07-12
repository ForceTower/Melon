package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Transient overlay between the confirmation sheet and the goodbye view —
// dc `EuScreen` logout "flashing" step. Full page-background plate that fades
// in fast, holding an error-tonal icon tile over a spinner + "Encerrando
// sessão…" row while the VM tears the session down (~0.9s).
@Composable
internal fun LogoutFlash(modifier: Modifier = Modifier) {
    val envelopeAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        envelopeAlpha.animateTo(1f, tween(durationMillis = 200, easing = EaseOut))
    }

    val err = MaterialTheme.melon.status.bad
    val ink = MaterialTheme.colorScheme.onBackground
    val tileShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = envelopeAlpha.value }
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(tileShape)
                    .background(err.copy(alpha = 0.12f).compositeOver(MaterialTheme.melon.surface.card))
                    .border(1.dp, err.copy(alpha = 0.32f), tileShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = err,
                    modifier = Modifier.size(26.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    color = err,
                    trackColor = ink.copy(alpha = 0.16f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.me_logout_flash_caption),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        letterSpacing = 0.26.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

private val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)

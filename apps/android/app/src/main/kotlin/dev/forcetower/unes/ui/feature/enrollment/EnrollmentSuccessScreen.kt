package dev.forcetower.unes.ui.feature.enrollment

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.LocalMelonDarkTheme
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon

// Proposta-enviada confirmation (dc `MatriculaScreen` success view): a
// full-bleed always-dark green mesh with the drawn check badge, the
// hours-in-disciplines summary, the glass stat trio and the light Concluir
// pill. Back (system or button) pops to the refreshed status hub.
@Composable
internal fun EnrollmentSuccessScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: EnrollmentViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    EnrollmentSuccessContent(state = state, onDone = onDone, modifier = modifier)
}

@Composable
private fun EnrollmentSuccessContent(
    state: EnrollmentUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val verdict = MaterialTheme.melon.verdict
    val onHero = MaterialTheme.melon.fixed.onHero

    SuccessChromeEffect()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(verdict.night),
    ) {
        Mesh(colors = verdict.passed.blobs, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            verdict.veil.copy(alpha = 0.2f),
                            verdict.veil.copy(alpha = 0.8f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(46.dp))
                    .background(onHero.copy(alpha = 0.14f))
                    .border(1.dp, onHero.copy(alpha = 0.26f), RoundedCornerShape(46.dp))
                    .scaleInOnAppear(delayMs = 80, fromScale = 0.6f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = onHero,
                    modifier = Modifier.size(46.dp),
                )
            }

            Text(
                text = stringResource(
                    R.string.enrollment_success_eyebrow_format,
                    state.window?.semester.orEmpty(),
                ).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                ),
                color = onHero.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(top = 26.dp)
                    .fadeUpOnAppear(delayMs = 200),
            )
            Text(
                text = stringResource(R.string.enrollment_success_title),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.02).sp,
                ),
                color = onHero,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fadeUpOnAppear(delayMs = 260),
            )
            SuccessBody(
                totalHours = state.totalHours,
                pickCount = state.resolvedPicks.size,
                onHero = onHero,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fadeUpOnAppear(delayMs = 320),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .padding(top = 28.dp)
                    .fadeUpOnAppear(delayMs = 380),
            ) {
                SuccessTile(
                    value = state.resolvedPicks.size.toString(),
                    label = stringResource(R.string.enrollment_success_disciplines),
                    onHero = onHero,
                    modifier = Modifier.weight(1f),
                )
                SuccessTile(
                    value = state.waitlistedCount.toString(),
                    label = stringResource(R.string.enrollment_success_queue),
                    onHero = onHero,
                    modifier = Modifier.weight(1f),
                )
                SuccessTile(
                    value = state.allowsOtherCount.toString(),
                    label = stringResource(R.string.enrollment_success_allows),
                    onHero = onHero,
                    modifier = Modifier.weight(1f),
                )
            }

            val doneLabel = stringResource(R.string.enrollment_success_done)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .padding(top = 30.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(MaterialTheme.melon.fixed.surfaceLight)
                    .clickable(role = Role.Button, onClickLabel = doneLabel, onClick = onDone)
                    .fadeUpOnAppear(delayMs = 440),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = doneLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.16).sp,
                    ),
                    color = MaterialTheme.melon.fixed.onSurfaceLight,
                )
            }
        }
    }
}

@Composable
private fun SuccessBody(totalHours: Int, pickCount: Int, onHero: Color, modifier: Modifier = Modifier) {
    val bold = SpanStyle(color = onHero, fontWeight = FontWeight.Bold)
    val hoursText = stringResource(R.string.enrollment_hours_format, totalHours)
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.enrollment_success_body_intro))
            withStyle(bold) { append(hoursText) }
            append(stringResource(R.string.enrollment_success_body_middle))
            withStyle(bold) { append(pluralStringResource(R.plurals.enrollment_success_body_count_format, pickCount, pickCount)) }
            append(stringResource(R.string.enrollment_success_body_outro))
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
        color = onHero.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        modifier = modifier.widthIn(max = 300.dp),
    )
}

@Composable
private fun SuccessTile(value: String, label: String, onHero: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(onHero.copy(alpha = 0.1f))
            .border(1.dp, onHero.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.78).sp,
            ),
            color = onHero,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.76.sp,
            ),
            color = onHero.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// The success plate is always dark, so the system-bar icons flip to light
// while it's on screen and restore to the resolved theme on the way out.
// (`SystemBarIconsEffect` can't be reused here — it has no dispose restore.)
@Composable
private fun SuccessChromeEffect() {
    val view = LocalView.current
    val darkTheme = LocalMelonDarkTheme.current
    if (view.isInEditMode) return
    DisposableEffect(darkTheme) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            val lightIcons = !darkTheme
            controller?.isAppearanceLightStatusBars = lightIcons
            controller?.isAppearanceLightNavigationBars = lightIcons
        }
    }
}

@Preview
@Composable
private fun EnrollmentSuccessPreview() {
    MelonTheme {
        EnrollmentSuccessContent(state = EnrollmentFixtures.state, onDone = {})
    }
}

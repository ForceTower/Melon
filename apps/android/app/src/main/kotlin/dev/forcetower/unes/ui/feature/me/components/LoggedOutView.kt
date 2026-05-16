package dev.forcetower.unes.ui.feature.me.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import java.util.Locale

// Goodbye screen rendered in place of the Me hub once the logout flow lands
// in `LogoutStep.LoggedOut`. Mirrors `LoggedOutView` on iOS — rose mesh halo
// at the top fading into the surface, serif farewell with the user's first
// name styled italic in accent, a reassurance card, and a CTA that flips the
// state machine back to `Idle`.
@Composable
internal fun LoggedOutView(
    firstName: String,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Ambient mesh — bleeds off the top into the surface so there's no
        // visible seam where the gradient stops. Behind everything else.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        ) {
            Mesh(
                variant = MeshVariant.Rose,
                intensity = 0.38f,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to surface,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Eyebrow()
            Spacer(Modifier.height(10.dp))
            Greeting(firstName = firstName)
            Spacer(Modifier.height(14.dp))
            BodyCopy()
            Spacer(Modifier.height(24.dp))
            LocalDataCard()
            Spacer(Modifier.height(20.dp))
            MelonPrimaryButton(
                text = stringResource(R.string.me_logged_out_cta),
                onClick = onSignIn,
                modifier = Modifier.fadeUpOnAppear(delayMs = 400),
            )
            Spacer(Modifier.height(14.dp))
            Footer()
        }
    }
}

@Composable
private fun Eyebrow() {
    Text(
        text = "◦ ${stringResource(R.string.me_logged_out_eyebrow)}".uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            letterSpacing = 1.44.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fadeUpOnAppear(delayMs = 50),
    )
}

@Composable
private fun Greeting(firstName: String) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = ink)) {
                append(stringResource(R.string.me_logged_out_greeting_prefix))
                append('\n')
            }
            withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Italic)) {
                append(firstName.lowercase(Locale.ROOT))
            }
            withStyle(SpanStyle(color = ink)) { append(".") }
        },
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 40.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.8).sp,
            fontWeight = FontWeight.Normal,
        ),
        modifier = Modifier.fadeUpOnAppear(delayMs = 120),
    )
}

@Composable
private fun BodyCopy() {
    Text(
        text = stringResource(R.string.me_logged_out_body),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            lineHeight = 21.sp,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .widthIn(max = 280.dp)
            .fadeUpOnAppear(delayMs = 220),
    )
}

@Composable
private fun LocalDataCard() {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val ok = MaterialTheme.melon.fixed.ok
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .fadeUpOnAppear(delayMs = 300),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ok)
                .border(3.dp, ok.copy(alpha = 0.2f), CircleShape),
        )
        Spacer(Modifier.size(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_logged_out_local_data_title),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.me_logged_out_local_data_hint),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.54.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink4,
            )
        }
    }
}

@Composable
private fun Footer() {
    Text(
        text = stringResource(R.string.me_logged_out_footer).uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.26.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .fadeUpOnAppear(delayMs = 500),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.components.MelonPrimaryButton
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import java.util.Locale

// Goodbye screen rendered in place of the Me hub once the logout flow lands
// in `LogoutStep.LoggedOut` — dc `EuScreen` "out" step. Magenta/coral mesh
// halo bleeding off the top into the page background, accent eyebrow, bold
// farewell with the user's first name in accent, a "dados limpos" reassurance
// card, and an ink CTA that flips the state machine back to `Idle`.
@Composable
internal fun LoggedOutView(
    firstName: String,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageBg = MaterialTheme.colorScheme.background
    val brand = MaterialTheme.melon.brand
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        // Ambient mesh — bleeds off the top into the page background so
        // there's no visible seam where the gradient stops. Behind everything.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
        ) {
            Mesh(
                colors = listOf(brand.magenta, brand.coral),
                intensity = 0.5f,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.92f to pageBg,
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
            Spacer(Modifier.height(12.dp))
            Greeting(firstName = firstName)
            Spacer(Modifier.height(16.dp))
            BodyCopy()
            Spacer(Modifier.height(26.dp))
            LocalDataCard()
            Spacer(Modifier.height(22.dp))
            MelonPrimaryButton(
                text = stringResource(R.string.me_logged_out_cta),
                onClick = onSignIn,
                contentColor = pageBg,
                modifier = Modifier.fadeUpOnAppear(delayMs = 400),
            )
            Spacer(Modifier.height(16.dp))
            Footer()
        }
    }
}

@Composable
private fun Eyebrow() {
    Text(
        text = stringResource(R.string.me_logged_out_eyebrow).uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
            letterSpacing = 0.78.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.primary,
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
            withStyle(SpanStyle(color = accent)) { append(firstName) }
            withStyle(SpanStyle(color = ink)) { append(".") }
        },
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 40.sp,
            lineHeight = 41.sp,
            letterSpacing = (-1.6).sp,
            fontWeight = FontWeight.ExtraBold,
        ),
        modifier = Modifier.fadeUpOnAppear(delayMs = 120),
    )
}

@Composable
private fun BodyCopy() {
    Text(
        text = stringResource(R.string.me_logged_out_body),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .widthIn(max = 300.dp)
            .fadeUpOnAppear(delayMs = 220),
    )
}

@Composable
private fun LocalDataCard() {
    val ok = MaterialTheme.melon.fixed.success
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp)
            .fadeUpOnAppear(delayMs = 300),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .background(ok.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(ok),
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_logged_out_local_data_title),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = stringResource(R.string.me_logged_out_local_data_hint),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun Footer() {
    val appInfo = rememberAppInfo()
    Text(
        text = stringResource(R.string.me_logged_out_footer_format, appInfo.version),
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            letterSpacing = 0.48.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier
            .fillMaxWidth()
            .fadeUpOnAppear(delayMs = 500),
        textAlign = TextAlign.Center,
    )
}

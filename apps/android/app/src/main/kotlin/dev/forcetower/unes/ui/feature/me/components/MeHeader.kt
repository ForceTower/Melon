package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.me.ProfileIdentity

// Top chrome of the Me screen. Mirrors `MeHeader` on iOS (and the JSX
// prototype): eyebrow row + serif greeting with the user's first name styled
// italic in the accent color. Lives inside the scroll view rather than the
// nav bar so the ambient mesh shows through.
@Composable
internal fun MeHeader(identity: ProfileIdentity, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val greeting = stringResource(R.string.me_header_greeting)
    val eyebrowFormat = stringResource(R.string.me_header_eyebrow_format, identity.semester)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = eyebrowFormat,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.44.sp,
                ),
                color = ink3,
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = ink)) { append("$greeting, ") }
                    withStyle(
                        SpanStyle(color = accent, fontStyle = FontStyle.Italic),
                    ) { append(identity.firstName) }
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.64).sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
        }
    }
}

package dev.forcetower.unes.ui.feature.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.feature.overview.OverviewFixtures

// App bar of the "Hoje" screen — date eyebrow, greeting, course line on the
// left; monogram avatar on the right.
@Composable
internal fun OverviewHeader(
    dateEyebrow: String,
    greeting: String,
    courseLine: String?,
    avatarInitials: String,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dateEyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (courseLine != null) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = courseLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                .clickable(onClickLabel = stringResource(R.string.overview_avatar_label)) {
                    onOpenProfile()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitials,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun OverviewHeaderPreview() {
    MelonTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            OverviewHeader(
                dateEyebrow = OverviewFixtures.DATE_EYEBROW,
                greeting = "Boa noite, Marina",
                courseLine = OverviewFixtures.COURSE_LINE,
                avatarInitials = "MA",
                onOpenProfile = {},
            )
        }
    }
}

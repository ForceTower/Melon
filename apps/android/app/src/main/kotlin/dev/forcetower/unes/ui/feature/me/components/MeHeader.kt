package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme

// M3 large-app-bar-style header — dc `EuScreen` top chrome: "Eu" title with
// the one-line subtitle. Lives inside the scroll region (same treatment as
// the Disciplinas header) so it rides away with the content.
@Composable
internal fun MeHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 18.dp),
    ) {
        Text(
            text = stringResource(R.string.me_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = stringResource(R.string.me_subtitle),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Preview
@Composable
private fun MeHeaderPreview() {
    MelonTheme {
        MeHeader()
    }
}

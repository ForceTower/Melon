package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon

// Sign-out pill at the bottom of the Eu screen — dc `EuScreen` logout button:
// full-width 52dp pill washed in the status red at low alpha.
@Composable
internal fun MeSignOutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bad = MaterialTheme.melon.status.bad
    val label = stringResource(R.string.me_sign_out_label)
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(bad.copy(alpha = 0.12f).compositeOver(MaterialTheme.melon.surface.card))
            .border(1.dp, bad.copy(alpha = 0.32f), shape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = bad,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = bad,
        )
    }
}

@Preview
@Composable
private fun MeSignOutButtonPreview() {
    MelonTheme {
        MeSignOutButton(onClick = {})
    }
}

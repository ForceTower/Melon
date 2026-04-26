package dev.forcetower.unes.ui.feature.me.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Quiet sign-out pill at the bottom of the Me hub. Same color the iOS variant
// reaches for: a deep coral that reads "destructive" without the visual
// weight of a filled button. Mirrors `SignOutButton` on iOS.
@Composable
internal fun MeSignOutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val line = MaterialTheme.melon.surface.line
    val destructive = MaterialTheme.melon.fixed.destructive
    val label = stringResource(R.string.me_sign_out_label)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(destructive.copy(alpha = 0.08f))
            .border(1.dp, line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeExitGlyph(color = destructive, modifier = Modifier.size(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.07).sp,
            ),
            color = destructive,
        )
    }
}

package dev.forcetower.unes.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon

/**
 * Tonal inline callout: a tinted plate carrying an icon, a bold title and a
 * one-line detail, optionally tappable.
 *
 * Pass the tint from the theme — `MaterialTheme.melon.status.warn` for
 * attention, `.status.bad` for a hard failure, `colorScheme.primary` for
 * neutral announcements.
 */
@Composable
fun MelonBanner(
    title: String,
    detail: String,
    tone: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Warning,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tone.copy(alpha = 0.14f).compositeOver(MaterialTheme.melon.surface.card))
            .border(1.dp, tone.copy(alpha = 0.30f), shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview
@Composable
private fun MelonBannerPreview() {
    MelonTheme {
        MelonBanner(
            title = "Sessão expirada",
            detail = "Entre novamente para retomar a sincronização.",
            tone = MaterialTheme.melon.status.bad,
            onClick = {},
        )
    }
}

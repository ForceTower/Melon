package dev.forcetower.unes.ui.feature.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.Message
import dev.forcetower.unes.ui.feature.messages.RelativeTime
import dev.forcetower.unes.ui.feature.messages.category
import dev.forcetower.unes.ui.feature.messages.categoryColor
import dev.forcetower.unes.ui.feature.messages.originIcon
import dev.forcetower.unes.ui.feature.messages.relativeTime

// One M3-style list row of the redesigned inbox — tonal circular avatar in the
// category hue, category tag (+ bookmark when saved), sender name, two-line
// preview, and the trailing time + unread dot. Unread rows sit on an
// accent-tinted plate ("Fundo tonal" in the dc prototype).
@Composable
internal fun MessageRow(
    message: Message,
    onOpen: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val hue = categoryColor(message.category)
    val shape = RoundedCornerShape(20.dp)
    val background = if (message.unread) {
        accent.copy(alpha = 0.08f).compositeOver(MaterialTheme.melon.surface.card)
    } else {
        Color.Transparent
    }

    val preview = message.subject?.takeIf { it.isNotBlank() }
        ?: message.preview?.takeIf { it.isNotBlank() }
        ?: message.body.replace(Regex("\\n+"), " ").trim()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable { onOpen(message) }
            .padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(hue.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = originIcon(message.origin),
                contentDescription = null,
                tint = hue,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message.sender.role.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.55.sp,
                    ),
                    color = hue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (message.starred) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = message.sender.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = if (message.unread) FontWeight.ExtraBold else FontWeight.SemiBold,
                    letterSpacing = (-0.16).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
                color = if (message.unread) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.outline
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Column(
            modifier = Modifier.padding(top = 2.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RelTimeLabel(time = relativeTime(message.receivedAt), unread = message.unread)
            if (message.unread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

@Composable
private fun RelTimeLabel(time: RelativeTime, unread: Boolean) {
    val text = when (time) {
        is RelativeTime.Literal -> time.text
        is RelativeTime.Resource -> if (time.arg != null) stringResource(time.res, time.arg) else stringResource(time.res)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = if (unread) FontWeight.ExtraBold else FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        maxLines = 1,
    )
}

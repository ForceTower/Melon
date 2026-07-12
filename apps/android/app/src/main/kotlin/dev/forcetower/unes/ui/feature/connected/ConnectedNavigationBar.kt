package dev.forcetower.unes.ui.feature.connected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon

// Native Material 3 bottom navigation for the Connected shell (2026 redesign)
// — opaque `surfaceContainer` plate with a hairline top border, accent pill
// indicator, and an unread badge on Mensagens.
@Composable
internal fun ConnectedNavigationBar(
    active: ConnectedTab,
    onChange: (ConnectedTab) -> Unit,
    badges: Map<ConnectedTab, Int>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.melon.surface.line),
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            ConnectedTab.entries.forEach { tab ->
                val selected = tab == active
                val badgeCount = badges[tab] ?: 0
                NavigationBarItem(
                    selected = selected,
                    onClick = { onChange(tab) },
                    icon = {
                        if (badgeCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.melon.fixed.onHero,
                                    ) {
                                        Text(text = badgeCount.toString())
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = tab.icon(selected),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            Icon(
                                imageVector = tab.icon(selected),
                                contentDescription = null,
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            ),
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        }
    }
}

private fun ConnectedTab.icon(selected: Boolean): ImageVector = when (this) {
    ConnectedTab.Overview -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    ConnectedTab.Schedule -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
    ConnectedTab.Classes -> if (selected) Icons.Filled.School else Icons.Outlined.School
    ConnectedTab.Messages -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
    ConnectedTab.Me -> if (selected) Icons.Filled.Person else Icons.Outlined.PersonOutline
}

@Preview
@Composable
private fun ConnectedNavigationBarPreview() {
    MelonTheme {
        ConnectedNavigationBar(
            active = ConnectedTab.Overview,
            onChange = {},
            badges = mapOf(ConnectedTab.Messages to 3),
        )
    }
}

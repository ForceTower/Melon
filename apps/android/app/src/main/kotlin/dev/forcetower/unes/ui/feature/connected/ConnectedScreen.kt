package dev.forcetower.unes.ui.feature.connected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.feature.overview.OverviewScreen

// The authenticated shell — hosts the liquid tab bar and routes to each
// feature's first screen. Mirrors iOS `ConnectedView` in shape: enum-driven
// tabs, single shared chrome, content swapped underneath.
//
// Only the Overview tab has a real implementation today (the design pass for
// `UNES Home.html`). Other tabs render placeholders so the bar's selection
// motion can be tested end-to-end before the corresponding features land.
@Composable
fun ConnectedScreen(modifier: Modifier = Modifier) {
    var active by rememberSaveable { mutableStateOf(ConnectedTab.Overview) }
    val unreadBadges = remember { mapOf(ConnectedTab.Messages to 2) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (active) {
            ConnectedTab.Overview -> OverviewScreen(bottomInset = TabBarBottomInset)
            else -> ComingSoonPanel(active)
        }

        LiquidTabBar(
            items = ConnectedTab.entries,
            active = active,
            onChange = { active = it },
            badges = unreadBadges,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 22.dp),
        )
    }
}

private val TabBarBottomInset = 110.dp

@Composable
private fun ComingSoonPanel(tab: ConnectedTab) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview
@Composable
private fun ConnectedScreenPreview() {
    MelonTheme { ConnectedScreen() }
}

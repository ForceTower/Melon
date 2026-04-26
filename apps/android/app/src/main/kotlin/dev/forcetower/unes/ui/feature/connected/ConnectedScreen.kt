package dev.forcetower.unes.ui.feature.connected

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.feature.me.MeScreen
import dev.forcetower.unes.ui.feature.messages.MessagesScreen
import dev.forcetower.unes.ui.feature.overview.OverviewScreen
import dev.forcetower.unes.ui.feature.schedule.ScheduleScreen

// The authenticated shell — hosts the liquid tab bar and routes to each
// feature's first screen. Mirrors iOS `ConnectedView` in shape: enum-driven
// tabs, single shared chrome, content swapped underneath.
//
// Overview, Schedule, Messages, and Me are wired to their real screens.
// Classes still falls through to a placeholder so the bar's selection
// motion can be tested end-to-end before that feature lands.
@Composable
fun ConnectedScreen(modifier: Modifier = Modifier) {
    val vm: ConnectedViewModel = hiltViewModel()
    // Fires on initial mount AND on every background → foreground transition,
    // mirroring iOS's `.task` + `.onChange(of: scenePhase)` pair. Concurrent
    // calls are deduped by a Mutex inside `ConnectedViewModel`.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onAppeared() }

    var active by rememberSaveable { mutableStateOf(ConnectedTab.Overview) }
    val unreadBadges = remember { mapOf(ConnectedTab.Messages to 2) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Backdrop captured into an offscreen layer with `BlurEffect` set so the
    // tab bar can replay it pre-blurred — Compose's native equivalent of CSS
    // `backdrop-filter: blur(...)`. Mesh.kt uses the same blur primitive (via
    // `Modifier.blur` — content blur). Both rely on API 31+; on older devices
    // the layer draws unblurred, leaving just the translucent chrome.
    val backdrop: GraphicsLayer? = if (BackdropBlurSupported) {
        rememberGraphicsLayer().also { layer ->
            layer.renderEffect = BlurEffect(BackdropBlurRadius, BackdropBlurRadius, TileMode.Decal)
        }
    } else null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null) {
                        Modifier.drawWithContent {
                            // Record the (sharp) screen content into the layer
                            // — the layer applies its blur on playback when
                            // the tab bar later calls `drawLayer(backdrop)`.
                            backdrop.record { this@drawWithContent.drawContent() }
                            drawContent()
                        }
                    } else Modifier,
                ),
        ) {
            when (active) {
                ConnectedTab.Overview -> OverviewScreen(bottomInset = TabBarBlockHeight + navBarBottom)
                ConnectedTab.Schedule -> ScheduleScreen(bottomInset = TabBarBlockHeight + navBarBottom)
                ConnectedTab.Messages -> MessagesScreen(bottomInset = TabBarBlockHeight + navBarBottom)
                ConnectedTab.Me -> MeScreen(bottomInset = TabBarBlockHeight + navBarBottom)
                else -> ComingSoonPanel(active)
            }
        }

        LiquidTabBar(
            items = ConnectedTab.entries,
            active = active,
            onChange = { active = it },
            badges = unreadBadges,
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Nav bars handle the gesture/3-button bar (any rotation);
                // horizontal display cutout keeps the bar off the notch when
                // the device is rotated to landscape. IME is intentionally
                // excluded so the bar stays under the keyboard.
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
                )
                .padding(horizontal = 14.dp, vertical = 22.dp),
        )
    }
}

// Visual height of the floating tab bar block (bar + 22dp top/bottom design
// margin). Content scroll views add this plus the live navigation-bar inset
// to keep the last item from sliding under the bar on devices with a
// 3-button nav bar.
private val TabBarBlockHeight = 110.dp

// 40px Gaussian σ — same order of magnitude as the JSX prototype's
// `backdrop-filter: blur(20px) saturate(180%)` (Compose's BlurEffect uses
// pixel radii and reads heavier than CSS blur for an equivalent perceived
// blur, hence 40 vs 20).
private const val BackdropBlurRadius = 40f
private val BackdropBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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

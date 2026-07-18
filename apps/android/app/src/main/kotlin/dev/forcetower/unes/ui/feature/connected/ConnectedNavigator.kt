package dev.forcetower.unes.ui.feature.connected

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

// Per-tab back stacks for the authenticated shell. Nav3 doesn't ship a
// multi-stack primitive, so we hold one `NavBackStack` per tab and feed
// only the active tab's entries to `NavDisplay`. Switching tabs preserves
// each tab's depth (matches iOS `TabView { Tab { … } }`); predictive back
// pops within the active tab and is a no-op at the tab root, same as iOS.
internal class ConnectedNavigator(
    private val activeTabState: MutableState<ConnectedTab>,
    val stacks: Map<ConnectedTab, NavBackStack<NavKey>>,
) {
    val activeTab: ConnectedTab get() = activeTabState.value

    val activeStack: NavBackStack<NavKey> get() = stacks.getValue(activeTab)

    fun selectTab(tab: ConnectedTab) {
        activeTabState.value = tab
    }

    fun navigate(route: NavKey) {
        activeStack.add(route)
    }

    fun goBack() {
        activeStack.removeLastOrNull()
    }

    // Deeplinks: atomically replace one tab's stack with a synthesized path
    // and bring that tab to front. `entries` must start at the tab's root.
    fun setStack(tab: ConnectedTab, entries: List<NavKey>) {
        val stack = stacks.getValue(tab)
        stack.clear()
        stack.addAll(entries)
        activeTabState.value = tab
    }
}

internal val LocalConnectedNavigator = compositionLocalOf<ConnectedNavigator> {
    error("No ConnectedNavigator provided")
}

@Composable
internal fun rememberConnectedNavigator(initial: ConnectedTab): ConnectedNavigator {
    val activeTab = rememberSaveable { mutableStateOf(initial) }
    val stacks = ConnectedTab.entries.associateWith { tab ->
        rememberNavBackStack(tab.rootRoute())
    }
    return remember(stacks) {
        ConnectedNavigator(activeTabState = activeTab, stacks = stacks)
    }
}

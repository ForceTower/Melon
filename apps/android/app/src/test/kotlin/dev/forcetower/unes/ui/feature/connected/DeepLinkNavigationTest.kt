package dev.forcetower.unes.ui.feature.connected

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

// Entity links rebuild the path a user would have walked, so back returns
// where they expect even if the tab already had depth.
internal class DeepLinkNavigationTest {
    private val navigator = ConnectedNavigator(
        activeTabState = mutableStateOf(ConnectedTab.Overview),
        stacks = ConnectedTab.entries.associateWith { NavBackStack<NavKey>(it.rootRoute()) },
    )

    @Test
    fun aMessageOpensOverTheInboxSoBackReturnsThere() {
        navigator.open(DeepLinkTarget.Message("m1"))
        assertEquals(ConnectedTab.Messages, navigator.activeTab)
        assertEquals(listOf(ConnectedRoute.MessagesList, ConnectedRoute.MessageDetail("m1")), stack())

        navigator.goBack()
        assertEquals(listOf<NavKey>(ConnectedRoute.MessagesList), stack())
    }

    @Test
    fun anEntityLinkReplacesWhateverTheTabHadOpen() {
        navigator.selectTab(ConnectedTab.Me)
        navigator.navigate(ConnectedRoute.Settings)

        navigator.open(DeepLinkTarget.Calendar)
        assertEquals(listOf(ConnectedRoute.Me, ConnectedRoute.Calendar), stack())
    }

    @Test
    fun materialLinksOpenThroughTheMaterialsHubOnMe() {
        navigator.open(DeepLinkTarget.MaterialDetail("mat-1"))
        assertEquals(ConnectedTab.Me, navigator.activeTab)
        assertEquals(
            listOf(ConnectedRoute.Me, ConnectedRoute.Materials, ConnectedRoute.MaterialsDetail("mat-1")),
            stack(),
        )

        navigator.open(DeepLinkTarget.MaterialsDiscipline("d1"))
        assertEquals(
            listOf(ConnectedRoute.Me, ConnectedRoute.Materials, ConnectedRoute.MaterialsDiscipline("d1")),
            stack(),
        )
    }

    // Like iOS `intentRoute`, a tab link only brings the tab forward.
    @Test
    fun tabAndReauthLinksSwitchTabsWithoutResettingThem() {
        navigator.open(DeepLinkTarget.Message("m1"))

        navigator.open(DeepLinkTarget.Reauth)
        assertEquals(ConnectedTab.Overview, navigator.activeTab)

        navigator.open(DeepLinkTarget.Tab(ConnectedTab.Messages))
        assertEquals(listOf(ConnectedRoute.MessagesList, ConnectedRoute.MessageDetail("m1")), stack())
    }

    private fun stack(): List<NavKey> = navigator.activeStack.toList()
}
